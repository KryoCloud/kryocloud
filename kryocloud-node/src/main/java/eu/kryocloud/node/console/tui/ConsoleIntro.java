package eu.kryocloud.node.console.tui;

import eu.kryocloud.node.console.ConsoleContext;

public final class ConsoleIntro {

    private static final String CLEAR_LINE = "\r\u001B[2K";
    private static final String[] STAGES = {"wrappers", "groups", "versions", "commands", "ready"};
    private static final String[] SPINNER = {Glyph.SNOWFLAKE.value(), Glyph.SNOWFLAKE_LIGHT.value(), Glyph.CRYSTAL.value(), Glyph.DIAMOND.value()};

    public void play(ConsoleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        renderStatic(context);
        runStages(context);
        finish(context);
    }

    private void renderStatic(ConsoleContext context) {
        for (String line : Banner.renderStatic()) {
            context.print(line);
        }
    }

    private void runStages(ConsoleContext context) {
        for (String stage : STAGES) {
            renderStage(context, stage);
        }

        context.raw(CLEAR_LINE);
    }

    private void renderStage(ConsoleContext context, String stage) {
        for (int frame = 0; frame < 5; frame++) {
            String glyph = SPINNER[frame % SPINNER.length];
            context.raw(CLEAR_LINE + "  " + Tone.PRIMARY.paint(glyph) + "  " + Tone.MUTED.paint("initializing " + stage));
            sleep(70L);
        }

        context.raw(CLEAR_LINE + "  " + Tone.SUCCESS.paint(Glyph.CHECK.value()) + "  " + Tone.FROST.paint(stage) + "\n");
    }

    private void finish(ConsoleContext context) {
        context.print("");
        context.print("  " + Tone.PRIMARY.paint(Glyph.SNOWFLAKE.value()) + "  " + Tone.FROST.paint("KryoCloud console ready"));
        context.print("  " + Tone.MUTED.paint("type ") + ConsoleTheme.code("help") + Tone.MUTED.paint(" to explore commands"));
        context.print("  " + Tone.MUTED.paint("press ") + ConsoleTheme.code("CTRL+C") + Tone.MUTED.paint(" twice within 5s to shutdown"));
        context.print("");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
