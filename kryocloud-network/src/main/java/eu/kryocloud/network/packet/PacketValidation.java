package eu.kryocloud.network.packet;

public final class PacketValidation {

    private PacketValidation() {
    }

    public static String string(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }

        return value;
    }

    public static String nonBlankString(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }

        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }

    public static <T> T value(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }

        return value;
    }

    public static int nonNegativeInt(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }

        return value;
    }

    public static int positiveInt(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }

        return value;
    }

    public static long nonNegativeLong(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }

        return value;
    }

    public static long positiveLong(long value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }

        return value;
    }

    public static int port(int value, String name) {
        if (value < 1 || value > 65_535) {
            throw new IllegalArgumentException(name + " must be between 1 and 65535");
        }

        return value;
    }
}