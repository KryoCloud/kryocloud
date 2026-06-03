package eu.kryocloud.node.console.tui;

public final class KryoPrompt {

    private final String nodeName;

    public KryoPrompt(String nodeName) {
        if (nodeName == null || nodeName.isBlank()) {
            throw new IllegalArgumentException("nodeName must not be blank");
        }

        this.nodeName = nodeName;
    }

    public StringBuilder renderStartBuilder() {
        StringBuilder builder = new StringBuilder();
        builder.append(Tone.PRIMARY.paint(Glyph.SNOWFLAKE.value()));
        builder.append(' ');
        builder.append(System.getProperty("user.name"));
        builder.append('@');
        builder.append(Tone.SECONDARY.paint(nodeName));
        return builder;
    }

    public String renderEndBuilder(StringBuilder builder) {
        builder.append(' ');
        builder.append(Tone.PRIMARY.paint(Glyph.PROMPT.value()));
        builder.append(' ');
        return builder.toString();
    }

    public String render() {
        return renderEndBuilder(renderStartBuilder());
    }

    public String renderWithStatus(int runningServices, int wrappers) {
        if (runningServices < 0) {
            throw new IllegalArgumentException("runningServices must not be negative");
        }

        if (wrappers < 0) {
            throw new IllegalArgumentException("wrappers must not be negative");
        }

        StringBuilder builder = renderStartBuilder();
        builder.append(Tone.DEEP.paint(" " + Glyph.SEPARATOR.value() + " "));
        builder.append(Tone.MUTED.paint("svc "));
        builder.append(Tone.ACCENT.paint(String.valueOf(runningServices)));
        builder.append(Tone.DEEP.paint(" " + Glyph.SEPARATOR.value() + " "));
        builder.append(Tone.MUTED.paint("wrp "));
        builder.append(Tone.INFO.paint(String.valueOf(wrappers)));
        return renderEndBuilder(builder);
    }
}
