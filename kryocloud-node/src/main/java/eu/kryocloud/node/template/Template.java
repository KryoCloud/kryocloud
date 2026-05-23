package eu.kryocloud.node.template;

import eu.kryocloud.api.template.ITemplate;

import java.nio.file.Path;

public record Template(String name, Path path) implements ITemplate {

    public Template {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
    }
}