package eu.kryocloud.node.template;

import eu.kryocloud.api.template.ITemplate;

import java.nio.file.Path;

public record Template(String name, Path path) implements ITemplate {
}
