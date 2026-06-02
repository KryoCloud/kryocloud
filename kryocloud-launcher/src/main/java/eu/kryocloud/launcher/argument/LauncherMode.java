package eu.kryocloud.launcher.argument;

import java.util.Locale;

public enum LauncherMode {

    NODE,
    WRAPPER,
    ALL;

    public static LauncherMode parse(String value) {
        if (value == null || value.isBlank()) {
            return NODE;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "node", "--node", "-node" -> NODE;
            case "wrapper", "--wrapper", "-wrapper" -> WRAPPER;
            case "all", "both", "local", "--all", "--both", "--local" -> ALL;
            default -> throw new IllegalArgumentException("Unknown launcher mode: " + value);
        };
    }

}
