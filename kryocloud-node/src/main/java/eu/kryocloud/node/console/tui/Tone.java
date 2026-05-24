package eu.kryocloud.node.console.tui;

import eu.kryocloud.common.logging.TextColor;

public enum Tone {

    PRIMARY("#7DD3FC"),
    SECONDARY("#38BDF8"),
    ACCENT("#A7F3D0"),
    SUCCESS("#34D399"),
    WARNING("#FDE68A"),
    DANGER("#FCA5A5"),
    INFO("#BAE6FD"),
    MUTED("#7C8EA3"),
    FROST("#E0F2FE"),
    CRYSTAL("#CFEFFF"),
    DEEP("#475569"),
    SHIMMER("#F0F9FF");

    private final TextColor color;

    Tone(String hex) {
        this.color = TextColor.hex(hex);
    }

    public TextColor color() {
        return color;
    }

    public String paint(String value) {
        return color.apply(value);
    }

    public String paintBold(String value) {
        return TextColor.BOLD.code() + color.apply(value);
    }
}
