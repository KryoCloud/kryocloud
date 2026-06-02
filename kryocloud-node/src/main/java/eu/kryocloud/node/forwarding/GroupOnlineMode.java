package eu.kryocloud.node.forwarding;

import java.util.Locale;

public enum GroupOnlineMode {

    AUTO,
    TRUE,
    FALSE;

    public static GroupOnlineMode parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "TRUE", "ON", "ONLINE", "ONLINE_MODE", "YES" -> TRUE;
            case "FALSE", "OFF", "OFFLINE", "NO" -> FALSE;
            default -> AUTO;
        };
    }

}
