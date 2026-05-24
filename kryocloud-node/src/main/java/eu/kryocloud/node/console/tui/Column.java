package eu.kryocloud.node.console.tui;

public final class Column {

    public enum Align {
        LEFT,
        RIGHT,
        CENTER
    }

    private final String header;
    private final Align align;
    private final int minWidth;

    private Column(String header, Align align, int minWidth) {
        if (header == null) {
            throw new IllegalArgumentException("header must not be null");
        }

        if (align == null) {
            throw new IllegalArgumentException("align must not be null");
        }

        if (minWidth < 1) {
            throw new IllegalArgumentException("minWidth must be greater than 0");
        }

        this.header = header;
        this.align = align;
        this.minWidth = minWidth;
    }

    public static Column left(String header) {
        return new Column(header, Align.LEFT, 1);
    }

    public static Column right(String header) {
        return new Column(header, Align.RIGHT, 1);
    }

    public static Column center(String header) {
        return new Column(header, Align.CENTER, 1);
    }

    public Column minWidth(int minWidth) {
        return new Column(header, align, minWidth);
    }

    public String header() {
        return header;
    }

    public Align align() {
        return align;
    }

    public int minWidth() {
        return minWidth;
    }
}
