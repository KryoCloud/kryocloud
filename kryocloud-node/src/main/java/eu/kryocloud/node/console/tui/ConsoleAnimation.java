package eu.kryocloud.node.console.tui;

import eu.kryocloud.node.console.ConsoleContext;

import java.time.Duration;
import java.util.function.Supplier;

public final class ConsoleAnimation {

    private static final String[] SPINNER_FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final String CLEAR_LINE = "\r\u001B[2K";

    public void spin(ConsoleContext context, String label, Duration duration) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }

        validateDuration(duration, "duration");
        animate(context, () -> ConsoleTheme.PRIMARY.apply(nextFrame()) + " " + ConsoleTheme.value(label), duration, Duration.ofMillis(80));
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
        animate(context, () -> ConsoleTheme.PRIMARY.apply(nextFrame()) + " " + lineSupplier.get(), duration, interval);
        context.raw(CLEAR_LINE);
    }

    public void success(ConsoleContext context, String message) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        context.raw(CLEAR_LINE);
        context.success("✓ " + safe(message));
    }

    public void failure(ConsoleContext context, String message) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        context.raw(CLEAR_LINE);
        context.error("✕ " + safe(message));
    }

    private void animate(ConsoleContext context, Supplier<String> lineSupplier, Duration duration, Duration interval) {
        long deadline = System.currentTimeMillis() + duration.toMillis();

        while (System.currentTimeMillis() <= deadline) {
            context.raw(CLEAR_LINE + lineSupplier.get());
            sleep(interval);
        }
    }

    private String nextFrame() {
        long frame = System.currentTimeMillis() / 80L;
        int index = (int) (frame % SPINNER_FRAMES.length);
        return SPINNER_FRAMES[index];
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