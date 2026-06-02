package eu.kryocloud.api.plugin.cloud.model;

import java.time.Instant;
import java.util.Map;

public record CloudServiceSnapshot(String name, String groupName, CloudServiceType type, CloudServiceState state, String wrapper, String host, int port, int memoryMb, Instant startedAt, Map<String, String> properties) {

    public CloudServiceSnapshot {
        name = requireText(name, "name");
        groupName = requireText(groupName, "groupName");
        type = type == null ? CloudServiceType.UNKNOWN : type;
        state = state == null ? CloudServiceState.UNKNOWN : state;
        wrapper = wrapper == null ? "" : wrapper.trim();
        host = host == null ? "" : host.trim();
        startedAt = startedAt == null ? Instant.EPOCH : startedAt;
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value.trim();
    }

}
