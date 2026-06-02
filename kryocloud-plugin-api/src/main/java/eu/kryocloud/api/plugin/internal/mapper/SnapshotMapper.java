package eu.kryocloud.api.plugin.internal.mapper;

import eu.kryocloud.api.plugin.cloud.model.CloudGroupSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudServiceSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudServiceState;
import eu.kryocloud.api.plugin.cloud.model.CloudServiceType;
import eu.kryocloud.api.plugin.cloud.model.CloudStatsSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudTemplateSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudVersionSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudWrapperSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudWrapperState;
import eu.kryocloud.api.plugin.cloud.model.ServiceStartResult;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SnapshotMapper {

    private SnapshotMapper() {
    }

    public static CloudServiceSnapshot service(Map<String, String> payload) {
        return new CloudServiceSnapshot(
                Payload.string(payload, "service"),
                Payload.string(payload, "group"),
                enumValue(CloudServiceType.class, payload.get("type"), CloudServiceType.UNKNOWN),
                enumValue(CloudServiceState.class, payload.get("state"), CloudServiceState.UNKNOWN),
                Payload.string(payload, "wrapper"),
                Payload.string(payload, "host"),
                Payload.integer(payload, "port"),
                Payload.integer(payload, "memoryMb"),
                Payload.instant(payload, "startedAt"),
                Map.of()
        );
    }

    public static CloudGroupSnapshot group(Map<String, String> payload) {
        return new CloudGroupSnapshot(
                Payload.string(payload, "group"),
                enumValue(CloudServiceType.class, payload.get("type"), CloudServiceType.UNKNOWN),
                Payload.integer(payload, "minOnline"),
                Payload.integer(payload, "maxOnline"),
                Payload.integer(payload, "memoryMb"),
                Payload.string(payload, "template"),
                Payload.string(payload, "version"),
                Map.of()
        );
    }

    public static CloudWrapperSnapshot wrapper(Map<String, String> payload) {
        return new CloudWrapperSnapshot(
                Payload.string(payload, "wrapper"),
                enumValue(CloudWrapperState.class, payload.get("state"), CloudWrapperState.UNKNOWN),
                Payload.string(payload, "host"),
                Payload.integer(payload, "services"),
                Payload.integer(payload, "maxServices"),
                Payload.integer(payload, "usedMemoryMb"),
                Payload.integer(payload, "maxMemoryMb"),
                Payload.instant(payload, "lastHeartbeat"),
                Map.of()
        );
    }

    public static CloudTemplateSnapshot template(Map<String, String> payload) {
        return new CloudTemplateSnapshot(
                Payload.string(payload, "template"),
                Payload.string(payload, "path"),
                Payload.instant(payload, "updatedAt"),
                Map.of()
        );
    }

    public static CloudVersionSnapshot version(Map<String, String> payload) {
        return new CloudVersionSnapshot(
                Payload.string(payload, "version"),
                Payload.string(payload, "type"),
                Payload.string(payload, "path"),
                Payload.instant(payload, "installedAt"),
                Map.of()
        );
    }

    public static CloudStatsSnapshot stats(Map<String, String> payload) {
        return new CloudStatsSnapshot(
                Payload.integer(payload, "wrappers"),
                Payload.integer(payload, "onlineWrappers"),
                Payload.integer(payload, "services"),
                Payload.integer(payload, "runningServices"),
                Payload.integer(payload, "groups"),
                Payload.integer(payload, "usedMemoryMb"),
                Payload.integer(payload, "maxMemoryMb")
        );
    }

    public static ServiceStartResult startResult(Map<String, String> payload) {
        return new ServiceStartResult(
                Payload.string(payload, "service"),
                Payload.string(payload, "group"),
                Payload.string(payload, "wrapper"),
                Payload.bool(payload, "success"),
                Payload.string(payload, "message")
        );
    }

    public static List<Map<String, String>> list(Map<String, String> payload, String prefix) {
        int size = Payload.integer(payload, prefix + ".size");
        List<Map<String, String>> result = new ArrayList<>();

        for (int index = 0; index < size; index++) {
            String itemPrefix = prefix + "." + index + ".";
            java.util.LinkedHashMap<String, String> item = new java.util.LinkedHashMap<>();

            for (Map.Entry<String, String> entry : payload.entrySet()) {
                if (!entry.getKey().startsWith(itemPrefix)) {
                    continue;
                }

                item.put(entry.getKey().substring(itemPrefix.length()), entry.getValue());
            }

            result.add(Map.copyOf(item));
        }

        return List.copyOf(result);
    }

    public static List<String> values(Map<String, String> payload, String prefix) {
        int size = Payload.integer(payload, prefix + ".size");
        List<String> result = new ArrayList<>();

        for (int index = 0; index < size; index++) {
            result.add(payload.getOrDefault(prefix + "." + index, ""));
        }

        return List.copyOf(result);
    }

    public static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

}
