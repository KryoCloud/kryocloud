package eu.kryocloud.node.console.tui;

import eu.kryocloud.node.console.ConsoleContext;

import java.time.Duration;
import java.util.function.Supplier;

public final class ConsoleAnimation {

    private static final String[] FROST_SPINNER = {
            Glyph.SNOWFLAKE.value(),
            Glyph.SNOWFLAKE_LIGHT.value(),
            Glyph.CRYSTAL.value(),
            Glyph.DIAMOND.value(),
            Glyph.CRYSTAL.value(),
            Glyph.SNOWFLAKE_LIGHT.value()
    };
    private static final String CLEAR_LINE = "\r\u001B[2K";
    private static final long FRAME_MILLIS = 90L;

    public void spin(ConsoleContext context, String label, Duration duration) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }

        validateDuration(duration, "duration");
        animate(context, () -> "  " + Tone.PRIMARY.paint(nextFrame()) + "  " + Tone.FROST.paint(label), duration, Duration.ofMillis(FRAME_MILLIS));
        context.raw(CLEAR_LINE);
    }

    public void live(ConsoleContext context, Supplier<String> lineSupplier, Duration duration, Duration interval) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        if (lineSupplier == null) {
            throw new IllegalArgumentException("lineSupplier must not be null");
        }

        validateDuration(duration, "duration");
        validateDuration(interval, "interval");
        animate(context, () -> "  " + Tone.PRIMARY.paint(nextFrame()) + "  " + lineSupplier.get(), duration, interval);
        context.raw(CLEAR_LINE);
    }

    public void success(ConsoleContext context, String message) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        context.raw(CLEAR_LINE);
        context.print("  " + Tone.SUCCESS.paint(Glyph.CHECK.value()) + "  " + Tone.FROST.paint(safe(message)));
    }

    public void failure(ConsoleContext context, String message) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        context.raw(CLEAR_LINE);
        context.print("  " + Tone.DANGER.paint(Glyph.CROSS.value()) + "  " + Tone.DANGER.paint(safe(message)));
    }

    private void animate(ConsoleContext context, Supplier<String> lineSupplier, Duration duration, Duration interval) {
        long deadline = System.currentTimeMillis() + duration.toMillis();

        while (System.currentTimeMillis() <= deadline) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            context.raw(CLEAR_LINE + lineSupplier.get());
            sleep(interval);
        }
    }

    private String nextFrame() {
        long frame = System.currentTimeMillis() / FRAME_MILLIS;
        int index = (int) Math.floorMod(frame, FROST_SPINNER.length);
        return FROST_SPINNER[index];
    }

    private void validateDuration(Duration duration, String name) {
        if (duration == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
