package eu.kryocloud.node.console.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Box {

    private final String title;
    private final List<String> lines = new ArrayList<>();
    private int padding = 1;
    private int minWidth = 40;

    private Box(String title) {
        this.title = title == null ? "" : title;
    }

    public static Box titled(String title) {
        return new Box(title);
    }

    public static Box untitled() {
        return new Box("");
    }

    public Box padding(int padding) {
        if (padding < 0) {
            throw new IllegalArgumentException("padding must not be negative");
        }

        this.padding = padding;
        return this;
    }

    public Box minWidth(int minWidth) {
        if (minWidth < 12) {
            throw new IllegalArgumentException("minWidth must be at least 12");
        }

        this.minWidth = minWidth;
        return this;
    }

    public Box line(String content) {
        lines.add(content == null ? "" : content);
        return this;
    }

    public Box blank() {
        lines.add("");
        return this;
    }

    public void render(Consumer<String> sink) {
        if (sink == null) {
            throw new IllegalArgumentException("sink must not be null");
        }

        int contentWidth = Math.max(minWidth, longestLineWidth());
        int totalWidth = contentWidth + padding * 2;

        sink.accept(renderTop(totalWidth));

        for (String line : lines) {
            sink.accept(renderRow(line, contentWidth));
        }

        sink.accept(renderBottom(totalWidth));
    }

    private String renderTop(int totalWidth) {
        if (title.isEmpty()) {
            return Tone.DEEP.paint(Glyph.CORNER_TOP_LEFT.value() + Glyph.DASH.repeat(totalWidth) + Glyph.CORNER_TOP_RIGHT.value());
        }

        String visible = " " + Glyph.SNOWFLAKE.value() + " " + title + " ";
        int visibleWidth = visualWidth(visible);
        int remaining = Math.max(2, totalWidth - visibleWidth);
        String painted = Tone.DEEP.paint(Glyph.CORNER_TOP_LEFT.value() + Glyph.DASH.value()) + Tone.PRIMARY.paint(Glyph.SNOWFLAKE.value()) + " " + Tone.SHIMMER.paintBold(title) + " " + Tone.DEEP.paint(Glyph.DASH.repeat(remaining - 1) + Glyph.CORNER_TOP_RIGHT.value());
        return painted;
    }

    private String renderBottom(int totalWidth) {
        return Tone.DEEP.paint(Glyph.CORNER_BOTTOM_LEFT.value() + Glyph.DASH.repeat(totalWidth) + Glyph.CORNER_BOTTOM_RIGHT.value());
    }

    private String renderRow(String content, int contentWidth) {
        int width = visualWidth(content);
        int trailing = Math.max(0, contentWidth - width);
        String pad = " ".repeat(padding);
        return Tone.DEEP.paint(Glyph.PIPE.value()) + pad + content + " ".repeat(trailing) + pad + Tone.DEEP.paint(Glyph.PIPE.value());
    }

    private int longestLineWidth() {
        int max = 0;

        for (String line : lines) {
            int width = visualWidth(line);

            if (width > max) {
                max = width;
            }
        }

        return max;
    }

    static int visualWidth(String value) {
        if (value == null) {
            return 0;
        }

        int width = 0;
        boolean inEscape = false;

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if (inEscape) {
                if (character == 'm') {
                    inEscape = false;
                }

                continue;
            }

            if (character == '\u001B') {
                inEscape = true;
                continue;
            }

            width++;
        }

        return width;
    }
}
