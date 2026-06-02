package eu.kryocloud.api.plugin.cloud.model;

import java.util.Map;

public record CloudGroupSnapshot(String name, CloudServiceType type, int minOnline, int maxOnline, int memoryMb, String template, String version, Map<String, String> properties) {

    public CloudGroupSnapshot {
        name = requireText(name, "name");
        type = type == null ? CloudServiceType.UNKNOWN : type;
        template = template == null ? "" : template.trim();
        version = version == null ? "" : version.trim();
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value.trim();
    }

}
