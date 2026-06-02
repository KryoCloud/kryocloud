package eu.kryocloud.api.plugin.cloud.model;

import java.util.List;

public record CloudCommandSpec(String name, String description, String usage, boolean executable, boolean cliOnly, List<String> aliases) {

    public CloudCommandSpec {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        usage = usage == null ? "" : usage;
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

}
