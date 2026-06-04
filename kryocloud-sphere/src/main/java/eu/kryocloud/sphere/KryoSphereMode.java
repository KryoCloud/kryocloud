package eu.kryocloud.sphere;

import java.util.Locale;

public enum KryoSphereMode {

    NONE,
    BASIC,
    STRICT;

    public static KryoSphereMode parse(String value) {
        if (value == null || value.isBlank()) {
            return BASIC;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');

        return switch (normalized) {
            case "OFF", "FALSE", "DISABLED", "NO", "NONE" -> NONE;
            case "ON", "TRUE", "ENABLED", "YES", "BASIC" -> BASIC;
            case "STRICT", "SPHERE", "KRYOSPHERE", "HARD" -> STRICT;
            default -> BASIC;
        };
    }

    public boolean enabled() {
        return this != NONE;
    }

}
