package eu.kryocloud.node.console.tui;

public final class ProgressBar {

    private static final String[] FRACTIONS = {
            "",
            Glyph.BAR_1.value(),
            Glyph.BAR_2.value(),
            Glyph.BAR_3.value(),
            Glyph.BAR_4.value(),
            Glyph.BAR_5.value(),
            Glyph.BAR_6.value(),
            Glyph.BAR_7.value()
    };

    private ProgressBar() {
    }

    public static String render(double ratio, int width) {
        int safeWidth = Math.max(8, width);
        double safeRatio = clamp(ratio);
        double scaled = safeRatio * safeWidth;
        int fullBlocks = (int) Math.floor(scaled);
        int remainder = (int) Math.round((scaled - fullBlocks) * 8.0D);

        if (remainder == 8) {
            fullBlocks++;
            remainder = 0;
        }

        if (fullBlocks > safeWidth) {
            fullBlocks = safeWidth;
            remainder = 0;
        }

        String filled = Glyph.BAR_FULL.repeat(fullBlocks);
        String cap = remainder > 0 && fullBlocks < safeWidth ? FRACTIONS[remainder] : "";
        int emptyCount = safeWidth - fullBlocks - (cap.isEmpty() ? 0 : 1);
        String empty = Glyph.BAR_EMPTY.repeat(Math.max(0, emptyCount));
        Tone fillTone = toneFor(safeRatio);
        String body = fillTone.paint(filled + cap) + Tone.DEEP.paint(empty);

        return Tone.MUTED.paint(Glyph.BRACKET_LEFT.value()) + body + Tone.MUTED.paint(Glyph.BRACKET_RIGHT.value());
    }

    public static String renderWithPercent(double ratio, int width) {
        return render(ratio, width) + " " + ConsoleTheme.percent(ratio);
    }

    private static Tone toneFor(double ratio) {
        if (ratio >= 0.85D) {
            return Tone.DANGER;
        }

        if (ratio >= 0.65D) {
            return Tone.WARNING;
        }

        return Tone.PRIMARY;
    }

    private static double clamp(double ratio) {
        if (Double.isNaN(ratio)) {
            return 0.0D;
        }

        return Math.max(0.0D, Math.min(1.0D, ratio));
    }
}
