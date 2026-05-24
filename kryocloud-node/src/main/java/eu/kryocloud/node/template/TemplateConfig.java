package eu.kryocloud.node.template;

import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;
import eu.kryocloud.common.layout.KryoDirectoryLayout;

import java.nio.file.Path;

public final class TemplateConfig extends Config {

    @Comment("Unique template name")
    private String name = "default";

    @Comment("Directory containing the template files")
    private String path = "templates/default";

    public TemplateConfig(Path path) {
        super(path);
    }

    public Template toTemplate() {
        return new Template(requireNonBlank(name, "name"), templatePath(requireNonBlank(path, "path")));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private Path templatePath(String rawPath) {
        Path configuredPath = Path.of(rawPath);

        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        if (rawPath.startsWith("templates/") || rawPath.startsWith("templates\\")) {
            return KryoDirectoryLayout.ROOT.resolve(rawPath).toAbsolutePath().normalize();
        }

        return KryoDirectoryLayout.TEMPLATES.resolve(rawPath).toAbsolutePath().normalize();
    }

    private String requireNonBlank(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }

        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
