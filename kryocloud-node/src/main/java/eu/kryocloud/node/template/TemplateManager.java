package eu.kryocloud.node.template;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.template.ITemplate;
import eu.kryocloud.api.template.ITemplateManager;
import eu.kryocloud.common.config.type.ConfigType;
import eu.kryocloud.node.config.LaunchConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public final class TemplateManager implements ITemplateManager {

    private final ConcurrentMap<String, ITemplate> templates = new ConcurrentHashMap<>();
    private final Path templatesDirectory = Path.of("templates");
    private final ConfigType configType;

    public TemplateManager(IConfigProvider configProvider) {
        if (configProvider == null) {
            throw new IllegalArgumentException("configProvider must not be null");
        }

        LaunchConfig launchConfig = configProvider.getConfig(LaunchConfig.class);

        if (launchConfig == null) {
            throw new IllegalStateException("LaunchConfig must be registered before TemplateManager starts");
        }

        configType = ConfigType.fromFileName("template" + launchConfig.getFileExtension());
        loadTemplates();
        createDefaultTemplateIfMissing();
    }

    private void loadTemplates() {
        try {
            Files.createDirectories(templatesDirectory);

            try (Stream<Path> paths = Files.list(templatesDirectory)) {
                paths.filter(Files::isRegularFile).filter(this::isTemplateConfig).sorted(Comparator.comparing(Path::getFileName)).forEach(this::loadTemplate);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load templates", exception);
        }
    }

    private void loadTemplate(Path path) {
        try {
            TemplateConfig config = new TemplateConfig(path);
            config.load();
            config.save();

            Template template = config.toTemplate();
            Files.createDirectories(template.path());
            templates.put(template.name(), template);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load template config " + path, exception);
        }
    }

    private void createDefaultTemplateIfMissing() {
        if (existsTemplate("default")) {
            return;
        }

        Path configPath = templatesDirectory.resolve("default" + configType.getEnding());
        TemplateConfig config = new TemplateConfig(configPath);
        config.setName("default");
        config.setPath(templatesDirectory.resolve("default").toString());
        config.save();

        Template template = config.toTemplate();

        try {
            Files.createDirectories(template.path());
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create default template directory", exception);
        }

        templates.put(template.name(), template);
    }

    @Override
    public void createTemplate(ITemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("template must not be null");
        }

        try {
            TemplateConfig config = new TemplateConfig(templatesDirectory.resolve(template.name() + configType.getEnding()));
            config.setName(template.name());
            config.setPath(template.path().toString());
            config.save();

            Files.createDirectories(template.path());
            templates.put(template.name(), template);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create template " + template.name(), exception);
        }
    }

    @Override
    public void deleteTemplate(String name) {
        validateName(name);

        ITemplate template = templates.remove(name);

        if (template == null) {
            return;
        }

        try {
            Files.deleteIfExists(templatesDirectory.resolve(template.name() + configType.getEnding()));
            deleteDirectoryIfExists(template.path());
        } catch (Exception exception) {
            throw new RuntimeException("Failed to delete template " + name, exception);
        }
    }

    @Override
    public boolean existsTemplate(String name) {
        validateName(name);
        return templates.containsKey(name);
    }

    @Override
    public ITemplate template(String name) {
        validateName(name);
        return templates.get(name);
    }

    @Override
    public HashMap<String, ITemplate> templates() {
        return new HashMap<>(templates);
    }

    private boolean isTemplateConfig(Path path) {
        return path.getFileName().toString().endsWith(configType.getEnding());
    }

    private void deleteDirectoryIfExists(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private void validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}