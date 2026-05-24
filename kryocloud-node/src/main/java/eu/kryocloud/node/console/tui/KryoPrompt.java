package eu.kryocloud.node.console.tui;

public final class KryoPrompt {

    private final String nodeName;

    public KryoPrompt(String nodeName) {
        if (nodeName == null || nodeName.isBlank()) {
            throw new IllegalArgumentException("nodeName must not be blank");
        }

        this.nodeName = nodeName;
    }

    public String render() {
        StringBuilder builder = new StringBuilder();
        builder.append(Tone.PRIMARY.paint(Glyph.SNOWFLAKE.value()));
        builder.append(' ');
        builder.append(Tone.SHIMMER.paintBold("kryo"));
        builder.append(Tone.MUTED.paint(Glyph.ARROW.value()));
        builder.append(Tone.SECONDARY.paint(nodeName));
        builder.append(' ');
        builder.append(Tone.PRIMARY.paint(Glyph.PROMPT.value()));
        builder.append(' ');
        return builder.toString();
    }

    public String renderWithStatus(int runningServices, int wrappers) {
        if (runningServices < 0) {
            throw new IllegalArgumentException("runningServices must not be negative");
        }

        if (wrappers < 0) {
            throw new IllegalArgumentException("wrappers must not be negative");
        }

        StringBuilder builder = new StringBuilder();
        builder.append(Tone.PRIMARY.paint(Glyph.SNOWFLAKE.value()));
        builder.append(' ');
        builder.append(Tone.SHIMMER.paintBold("kryo"));
        builder.append(Tone.MUTED.paint(Glyph.ARROW.value()));
        builder.append(Tone.SECONDARY.paint(nodeName));
        builder.append(Tone.DEEP.paint(" " + Glyph.SEPARATOR.value() + " "));
        builder.append(Tone.MUTED.paint("svc "));
        builder.append(Tone.ACCENT.paint(String.valueOf(runningServices)));
        builder.append(Tone.DEEP.paint(" " + Glyph.SEPARATOR.value() + " "));
        builder.append(Tone.MUTED.paint("wrp "));
        builder.append(Tone.INFO.paint(String.valueOf(wrappers)));
        builder.append(' ');
        builder.append(Tone.PRIMARY.paint(Glyph.PROMPT.value()));
        builder.append(' ');
        return builder.toString();
    }
}
