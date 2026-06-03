package eu.kryocloud.api.plugin.messaging;

import java.util.Locale;

public record PluginChannel(String namespace, String name) {

    public PluginChannel {
        namespace = requirePart(namespace, "namespace");
        name = requirePart(name, "name");
    }

    public static PluginChannel of(String namespace, String name) {
        return new PluginChannel(namespace, name);
    }

    public static PluginChannel parse(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }

        String[] parts = key.split(":", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("key must use namespace:name");
        }

        return of(parts[0], parts[1]);
    }

    public String key() {
        return namespace + ":" + name;
    }

    @Override
    public String toString() {
        return key();
    }

    private static String requirePart(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        String text = value.trim().toLowerCase(Locale.ROOT);

        if (!text.matches("[a-z0-9][a-z0-9_-]{1,63}")) {
            throw new IllegalArgumentException(field + " must match [a-z0-9][a-z0-9_-]{1,63}");
        }

        return text;
    }

}
