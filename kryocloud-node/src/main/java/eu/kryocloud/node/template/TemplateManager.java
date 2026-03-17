package eu.kryocloud.node.template;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.template.ITemplate;
import eu.kryocloud.api.template.ITemplateManager;
import eu.kryocloud.node.config.LaunchConfig;
import eu.kryocloud.common.config.type.ConfigType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;

public class TemplateManager implements ITemplateManager {

    private final HashMap<String, ITemplate> templates;
    private final ConfigType configType;

    public TemplateManager(IConfigProvider configProvider) {
        this.templates = new HashMap<>();
        this.configType = ConfigType.fromFileName(configProvider.getConfig(LaunchConfig.class).getFileExtension());
        this.loadTemplates();
    }

    private void loadTemplates() {
        for (File file : Objects.requireNonNull(new File("templates").listFiles())) {
            if (file.isFile() && file.getName().endsWith(this.configType.getEnding())) {
                try {
                    ITemplate template = this.configType.getTypeProvider().get(file.toPath().toString(), ITemplate.class);
                    this.templates.put(template.name(), template);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void createTemplate(ITemplate template) {
        try {
            this.configType.getTypeProvider().save(Path.of("templates", template.name() + this.configType.getEnding()), template);
            Files.createDirectories(template.path());
            this.templates.put(template.name(), template);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteTemplate(String name) {
        try {
            ITemplate template = this.templates.get(name);
            Files.delete(Path.of("templates", template.name() + this.configType.getEnding()));
            Files.delete(template.path());
            this.templates.remove(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean existsTemplate(String name) {
        return this.templates.containsKey(name);
    }

    @Override
    public ITemplate template(String name) {
        return this.templates.get(name);
    }

    @Override
    public HashMap<String, ITemplate> templates() {
        return templates;
    }
}
