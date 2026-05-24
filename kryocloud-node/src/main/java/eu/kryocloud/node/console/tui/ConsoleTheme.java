package eu.kryocloud.node.console.tui;

import eu.kryocloud.common.logging.TextColor;
import eu.kryocloud.network.protocol.CloudServiceState;

public final class ConsoleTheme {

    private ConsoleTheme() {
    }

    public static String title(String value) {
        return TextColor.BOLD.code() + Tone.PRIMARY.paint(Glyph.SNOWFLAKE.value() + " " + value) + TextColor.RESET.code();
    }

    public static String subtitle(String value) {
        return Tone.MUTED.paint(value);
    }

    public static String label(String value) {
        return Tone.SECONDARY.paint(value);
    }

    public static String value(String value) {
        return Tone.CRYSTAL.paint(value);
    }

    public static String muted(String value) {
        return Tone.MUTED.paint(value);
    }

    public static String success(String value) {
        return Tone.SUCCESS.paint(value);
    }

    public static String warning(String value) {
        return Tone.WARNING.paint(value);
    }

    public static String danger(String value) {
        return Tone.DANGER.paint(value);
    }

    public static String info(String value) {
        return Tone.INFO.paint(value);
    }

    public static String accent(String value) {
        return Tone.ACCENT.paint(value);
    }

    public static String frost(String value) {
        return Tone.FROST.paint(value);
    }

    public static String crystal(String value) {
        return Tone.PRIMARY.paint(Glyph.SNOWFLAKE.value() + " ") + Tone.FROST.paint(value);
    }

    public static String bullet() {
        return Tone.PRIMARY.paint(Glyph.DIAMOND.value());
    }

    public static String separator() {
        return Tone.MUTED.paint("  " + Glyph.SEPARATOR.value() + "  ");
    }

    public static String divider(int width) {
        int safe = Math.max(12, width);
        return Tone.DEEP.paint(Glyph.DASH.repeat(safe));
    }

    public static String frostDivider(int width) {
        int safe = Math.max(12, width);
        int third = Math.max(1, safe / 3);
        return Tone.DEEP.paint(Glyph.DASH.repeat(third)) + Tone.PRIMARY.paint(" " + Glyph.SNOWFLAKE.value() + " ") + Tone.DEEP.paint(Glyph.DASH.repeat(safe - third - 3));
    }

    public static String state(CloudServiceState state) {
        if (state == null) {
            return Tone.MUTED.paint("UNKNOWN");
        }

        return switch (state) {
            case RUNNING -> Tone.SUCCESS.paint("RUNNING");
            case STARTING, PREPARING -> Tone.INFO.paint(state.name());
            case STOPPING -> Tone.WARNING.paint("STOPPING");
            case STOPPED -> Tone.MUTED.paint("STOPPED");
            case FAILED -> Tone.DANGER.paint("FAILED");
        };
    }

    public static String code(String value) {
        return Tone.PRIMARY.paint(value);
    }


    public static String progressBar(double ratio, int width) {
        return ProgressBar.render(ratio, width);
    }

    public static String progressBarWithPercent(double ratio, int width) {
        return ProgressBar.renderWithPercent(ratio, width);
    }

    public static String percent(double ratio) {
        int percent = (int) Math.round(Math.max(0.0D, Math.min(1.0D, ratio)) * 100.0D);
        String text = padLeft(percent + "%", 4);

        if (percent >= 85) {
            return Tone.DANGER.paint(text);
        }

        if (percent >= 65) {
            return Tone.WARNING.paint(text);
        }

        return Tone.INFO.paint(text);
    }

    private static String padLeft(String value, int width) {
        if (value.length() >= width) {
            return value;
        }

        return " ".repeat(width - value.length()) + value;
    }
}
