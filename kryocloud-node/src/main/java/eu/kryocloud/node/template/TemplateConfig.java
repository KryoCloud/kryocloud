package eu.kryocloud.node.template;

import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;

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
        return new Template(requireNonBlank(name, "name"), Path.of(requireNonBlank(path, "path")));
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