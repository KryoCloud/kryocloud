package eu.kryocloud.common.logging;

public final class TextColor {

    public static final TextColor RESET = new TextColor(-1, -1, -1, "\u001B[0m");
    public static final TextColor BOLD = new TextColor(-1, -1, -1, "\u001B[1m");

    public static final TextColor GRAY = hex("#8B949E");
    public static final TextColor RED = hex("#FF5C5C");
    public static final TextColor GREEN = hex("#3DDC97");
    public static final TextColor YELLOW = hex("#FFD166");
    public static final TextColor BLUE = hex("#6EA8FE");
    public static final TextColor MAGENTA = hex("#C77DFF");
    public static final TextColor CYAN = hex("#5EEAD4");
    public static final TextColor WHITE = hex("#F8F9FA");

    private final int red;
    private final int green;
    private final int blue;
    private final String escape;

    private TextColor(int red, int green, int blue, String escape) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.escape = escape;
    }

    public static TextColor rgb(int red, int green, int blue) {
        validateChannel(red, "red");
        validateChannel(green, "green");
        validateChannel(blue, "blue");
        return new TextColor(red, green, blue, "\u001B[38;2;" + red + ";" + green + ";" + blue + "m");
    }

    public static TextColor hex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("hex must not be null");
        }

        String normalized = hex.trim();

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (!normalized.matches("[A-Fa-f0-9]{6}")) {
            throw new IllegalArgumentException("hex must be in format #RRGGBB");
        }

        int red = Integer.parseInt(normalized.substring(0, 2), 16);
        int green = Integer.parseInt(normalized.substring(2, 4), 16);
        int blue = Integer.parseInt(normalized.substring(4, 6), 16);

        return rgb(red, green, blue);
    }

    public String apply(String text) {
        return escape + safe(text) + RESET.escape;
    }

    public String code() {
        return escape;
    }

    public int red() {
        return red;
    }

    public int green() {
        return green;
    }

    public int blue() {
        return blue;
    }

    private static void validateChannel(int value, String name) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(name + " must be between 0 and 255");
        }
    }

    private String safe(String text) {
        if (text == null) {
            return "null";
        }

        return text;
    }
}