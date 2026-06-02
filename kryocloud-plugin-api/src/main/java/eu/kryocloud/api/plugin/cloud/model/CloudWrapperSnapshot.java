package eu.kryocloud.api.plugin.cloud.model;

import java.time.Instant;
import java.util.Map;

public record CloudWrapperSnapshot(String name, CloudWrapperState state, String host, int services, int maxServices, int usedMemoryMb, int maxMemoryMb, Instant lastHeartbeat, Map<String, String> properties) {

    public CloudWrapperSnapshot {
        name = requireText(name, "name");
        state = state == null ? CloudWrapperState.UNKNOWN : state;
        host = host == null ? "" : host.trim();
        lastHeartbeat = lastHeartbeat == null ? Instant.EPOCH : lastHeartbeat;
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value.trim();
    }

}
