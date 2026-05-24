package eu.kryocloud.node.console.tui;

public enum Glyph {

    SNOWFLAKE("❄"),
    SNOWFLAKE_LIGHT("✻"),
    SNOWFLAKE_TINY("·"),
    DIAMOND("◆"),
    CRYSTAL("✦"),
    CHECK("✓"),
    CROSS("✕"),
    ARROW("›"),
    PROMPT("❯"),
    DOT("•"),
    DOT_HEAVY("●"),
    PIPE("│"),
    DASH("─"),
    DASH_HEAVY("━"),
    CORNER_TOP_LEFT("╭"),
    CORNER_TOP_RIGHT("╮"),
    CORNER_BOTTOM_LEFT("╰"),
    CORNER_BOTTOM_RIGHT("╯"),
    T_DOWN("┬"),
    T_UP("┴"),
    T_RIGHT("├"),
    T_LEFT("┤"),
    CROSS_T("┼"),
    BAR_FULL("█"),
    BAR_7("▉"),
    BAR_6("▊"),
    BAR_5("▋"),
    BAR_4("▌"),
    BAR_3("▍"),
    BAR_2("▎"),
    BAR_1("▏"),
    BAR_EMPTY("░"),
    BRACKET_LEFT("⟦"),
    BRACKET_RIGHT("⟧"),
    SEPARATOR("⋄");

    private final String value;

    Glyph(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public String repeat(int count) {
        if (count < 1) {
            return "";
        }

        return value.repeat(count);
    }

    @Override
    public String toString() {
        return value;
    }
}
