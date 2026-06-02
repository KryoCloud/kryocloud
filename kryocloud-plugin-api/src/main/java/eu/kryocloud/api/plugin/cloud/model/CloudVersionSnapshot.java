package eu.kryocloud.api.plugin.cloud.model;

import java.time.Instant;
import java.util.Map;

public record CloudVersionSnapshot(String name, String type, String path, Instant installedAt, Map<String, String> properties) {

    public CloudVersionSnapshot {
        name = requireText(name, "name");
        type = type == null ? "" : type.trim();
        path = path == null ? "" : path.trim();
        installedAt = installedAt == null ? Instant.EPOCH : installedAt;
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value.trim();
    }

}
