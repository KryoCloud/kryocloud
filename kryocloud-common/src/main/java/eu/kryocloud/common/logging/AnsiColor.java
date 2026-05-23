package eu.kryocloud.common.logging;

public enum AnsiColor {

    RESET("\u001B[0m"),
    GRAY("\u001B[90m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    MAGENTA("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m"),
    BOLD("\u001B[1m");

    private final String code;

    AnsiColor(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public String apply(String text) {
        if (text == null) {
            return code + "null" + RESET.code;
        }

        return code + text + RESET.code;
    }
}