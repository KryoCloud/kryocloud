package eu.kryocloud.plugins.proxybridge.command;

public final class CloudMessageFormatter {

    public static final String PREFIX = "§8｢§b§lKryoCloud§8｣ §7";

    private CloudMessageFormatter() {
    }

    public static String message(String message) {
        return PREFIX + plain(message);
    }

    private static String plain(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }

        return message;
    }

}
