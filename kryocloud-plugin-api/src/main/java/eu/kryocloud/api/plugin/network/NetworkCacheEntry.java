package eu.kryocloud.api.plugin.network;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public record NetworkCacheEntry(String key, byte[] value, String contentType, long version, Instant updatedAt, Optional<Instant> expiresAt, String sourcePlugin, String sourceService, String sourceGroup, String sourceWrapper) {

    public NetworkCacheEntry {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }

        key = key.trim();
        value = value == null ? new byte[0] : Arrays.copyOf(value, value.length);
        contentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType.trim();
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        expiresAt = expiresAt == null ? Optional.empty() : expiresAt;
        sourcePlugin = sourcePlugin == null ? "" : sourcePlugin.trim();
        sourceService = sourceService == null ? "" : sourceService.trim();
        sourceGroup = sourceGroup == null ? "" : sourceGroup.trim();
        sourceWrapper = sourceWrapper == null ? "" : sourceWrapper.trim();
    }

    public static NetworkCacheEntry empty(String key) {
        return new NetworkCacheEntry(key, new byte[0], "application/octet-stream", 0L, Instant.EPOCH, Optional.empty(), "", "", "", "");
    }

    public static NetworkCacheEntry fromPayload(Map<String, String> payload) {
        String key = payload.getOrDefault("key", "");
        byte[] value = Base64.getDecoder().decode(payload.getOrDefault("value", ""));
        long version = longValue(payload.get("version"));
        Instant updatedAt = instant(payload.get("updatedAt"), Instant.now());
        Optional<Instant> expiresAt = optionalInstant(payload.get("expiresAt"));

        return new NetworkCacheEntry(
                key,
                value,
                payload.getOrDefault("contentType", "application/octet-stream"),
                version,
                updatedAt,
                expiresAt,
                payload.getOrDefault("sourcePlugin", ""),
                payload.getOrDefault("sourceService", ""),
                payload.getOrDefault("sourceGroup", ""),
                payload.getOrDefault("sourceWrapper", "")
        );
    }

    @Override
    public byte[] value() {
        return Arrays.copyOf(value, value.length);
    }

    public String text() {
        return new String(value, StandardCharsets.UTF_8);
    }

    public boolean expired() {
        return expiresAt.map(instant -> instant.isBefore(Instant.now())).orElse(false);
    }

    private static long longValue(String value) {
        try {
            return Long.parseLong(value == null || value.isBlank() ? "0" : value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static Instant instant(String value, Instant fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Instant.parse(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Optional<Instant> optionalInstant(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Instant.parse(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

}
