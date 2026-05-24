package eu.kryocloud.node.console.tui;

import java.util.List;

public final class Banner {

    private static final String TAGLINE = "frost-tier minecraft cloud";

    private Banner() {
    }

    public static List<String> renderStatic() {
        String title = Tone.PRIMARY.paintBold("KryoCloud");
        String version = Tone.MUTED.paint("node");
        String line = Tone.DEEP.paint("─".repeat(44));

        return List.of(
                "",
                "  " + Tone.PRIMARY.paint(Glyph.SNOWFLAKE.value()) + "  " + title + Tone.MUTED.paint("  " + Glyph.SEPARATOR.value() + "  ") + version,
                "  " + line,
                "  " + Tone.MUTED.paint(TAGLINE),
                ""
        );
    }

    public static int height() {
        return renderStatic().size();
    }

    public static int bannerWidth() {
        return 48;
    }

    public static String tagline() {
        return TAGLINE;
    }
}
