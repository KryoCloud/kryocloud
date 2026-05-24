package eu.kryocloud.node.console.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Table {

    private final List<Column> columns = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    private Table() {
    }

    public static Table builder() {
        return new Table();
    }

    public Table column(Column column) {
        if (column == null) {
            throw new IllegalArgumentException("column must not be null");
        }

        columns.add(column);
        return this;
    }

    public Table row(String... cells) {
        if (cells == null) {
            throw new IllegalArgumentException("cells must not be null");
        }

        if (cells.length != columns.size()) {
            throw new IllegalArgumentException("row size " + cells.length + " does not match column count " + columns.size());
        }

        List<String> safeCells = new ArrayList<>(cells.length);

        for (String cell : cells) {
            safeCells.add(cell == null ? "" : cell);
        }

        rows.add(safeCells);
        return this;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public void render(Consumer<String> sink) {
        if (sink == null) {
            throw new IllegalArgumentException("sink must not be null");
        }

        if (columns.isEmpty()) {
            return;
        }

        int[] widths = computeWidths();

        sink.accept(border(widths, Glyph.CORNER_TOP_LEFT, Glyph.T_DOWN, Glyph.CORNER_TOP_RIGHT));
        sink.accept(headerRow(widths));
        sink.accept(border(widths, Glyph.T_RIGHT, Glyph.CROSS_T, Glyph.T_LEFT));

        for (List<String> row : rows) {
            sink.accept(dataRow(row, widths));
        }

        sink.accept(border(widths, Glyph.CORNER_BOTTOM_LEFT, Glyph.T_UP, Glyph.CORNER_BOTTOM_RIGHT));
    }

    private int[] computeWidths() {
        int[] widths = new int[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            widths[i] = Math.max(columns.get(i).minWidth(), Box.visualWidth(columns.get(i).header()));
        }

        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                int cellWidth = Box.visualWidth(row.get(i));

                if (cellWidth > widths[i]) {
                    widths[i] = cellWidth;
                }
            }
        }

        return widths;
    }

    private String border(int[] widths, Glyph left, Glyph middle, Glyph right) {
        StringBuilder builder = new StringBuilder();
        builder.append(left.value());

        for (int i = 0; i < widths.length; i++) {
            builder.append(Glyph.DASH.repeat(widths[i] + 2));

            if (i < widths.length - 1) {
                builder.append(middle.value());
                continue;
            }

            builder.append(right.value());
        }

        return Tone.DEEP.paint(builder.toString());
    }

    private String headerRow(int[] widths) {
        StringBuilder builder = new StringBuilder();
        builder.append(Tone.DEEP.paint(Glyph.PIPE.value()));

        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            String painted = Tone.SECONDARY.paintBold(column.header());
            builder.append(" ").append(pad(painted, column.header(), widths[i], column.align())).append(" ");
            builder.append(Tone.DEEP.paint(Glyph.PIPE.value()));
        }

        return builder.toString();
    }

    private String dataRow(List<String> row, int[] widths) {
        StringBuilder builder = new StringBuilder();
        builder.append(Tone.DEEP.paint(Glyph.PIPE.value()));

        for (int i = 0; i < row.size(); i++) {
            String cell = row.get(i);
            builder.append(" ").append(pad(cell, cell, widths[i], columns.get(i).align())).append(" ");
            builder.append(Tone.DEEP.paint(Glyph.PIPE.value()));
        }

        return builder.toString();
    }

    private String pad(String painted, String plain, int width, Column.Align align) {
        int visible = Box.visualWidth(plain);
        int diff = Math.max(0, width - visible);

        if (align == Column.Align.RIGHT) {
            return " ".repeat(diff) + painted;
        }

        if (align == Column.Align.CENTER) {
            int leftPad = diff / 2;
            int rightPad = diff - leftPad;
            return " ".repeat(leftPad) + painted + " ".repeat(rightPad);
        }

        return painted + " ".repeat(diff);
    }
}
