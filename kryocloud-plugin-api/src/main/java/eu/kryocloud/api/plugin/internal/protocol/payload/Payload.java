package eu.kryocloud.api.plugin.internal.protocol.payload;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Payload {

    private final Map<String, String> values = new LinkedHashMap<>();

    private Payload() {
    }

    public static Payload create() {
        return new Payload();
    }

    public Payload put(String key, Object value) {
        if (key == null || key.isBlank()) {
            return this;
        }

        if (value == null) {
            return this;
        }

        values.put(key, String.valueOf(value));
        return this;
    }

    public Payload putAll(Map<String, String> values) {
        if (values == null) {
            return this;
        }

        values.forEach(this::put);
        return this;
    }

    public Map<String, String> map() {
        return Map.copyOf(values);
    }

    public static String string(Map<String, String> payload, String key) {
        return payload.getOrDefault(key, "");
    }

    public static int integer(Map<String, String> payload, String key) {
        return integer(payload, key, 0);
    }

    public static int integer(Map<String, String> payload, String key, int fallback) {
        try {
            return Integer.parseInt(payload.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static long longValue(Map<String, String> payload, String key) {
        return longValue(payload, key, 0L);
    }

    public static long longValue(Map<String, String> payload, String key, long fallback) {
        try {
            return Long.parseLong(payload.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static double doubleValue(Map<String, String> payload, String key) {
        try {
            return Double.parseDouble(payload.getOrDefault(key, "0"));
        } catch (NumberFormatException ignored) {
            return 0D;
        }
    }

    public static boolean bool(Map<String, String> payload, String key) {
        return Boolean.parseBoolean(payload.getOrDefault(key, "false"));
    }

    public static Instant instant(Map<String, String> payload, String key) {
        try {
            return Instant.parse(payload.getOrDefault(key, Instant.EPOCH.toString()));
        } catch (RuntimeException ignored) {
            return Instant.EPOCH;
        }
    }

}
