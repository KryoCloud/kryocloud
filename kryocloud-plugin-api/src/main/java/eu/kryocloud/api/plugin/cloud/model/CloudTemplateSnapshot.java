package eu.kryocloud.api.plugin.cloud.model;

import java.time.Instant;
import java.util.Map;

public record CloudTemplateSnapshot(String name, String path, Instant updatedAt, Map<String, String> properties) {

    public CloudTemplateSnapshot {
        name = requireText(name, "name");
        path = path == null ? "" : path.trim();
        updatedAt = updatedAt == null ? Instant.EPOCH : updatedAt;
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value.trim();
    }

}
