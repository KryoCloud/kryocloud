package eu.kryocloud.common.logging;

import org.jline.reader.LineReader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public final class ConsoleOutput {

    private static final String CLEAR_LINE = "\r\u001B[2K";
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final ThreadLocal<Boolean> DIRECT_OUTPUT = ThreadLocal.withInitial(() -> false);
    private static final List<String> DEFERRED_LINES = new ArrayList<>();

    private static volatile LineReader lineReader;
    private static volatile boolean transientLineActive;
    private static volatile boolean deferBackgroundOutput;

    private ConsoleOutput() {
    }

    public static void prepareInteractiveConsole() {
        LOCK.lock();

        try {
            clearTransientLine();
            lineReader = null;
            transientLineActive = false;
        } finally {
            LOCK.unlock();
        }
    }

    public static void attach(LineReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader must not be null");
        }

        LOCK.lock();

        try {
            lineReader = reader;
        } finally {
            LOCK.unlock();
        }
    }

    public static void detach(LineReader reader) {
        if (reader == null) {
            return;
        }

        LOCK.lock();

        try {
            if (lineReader != reader) {
                return;
            }

            lineReader = null;
            transientLineActive = false;
        } finally {
            LOCK.unlock();
        }
    }

    public static void deferBackgroundOutput(boolean enabled) {
        LOCK.lock();

        try {
            deferBackgroundOutput = enabled;
        } finally {
            LOCK.unlock();
        }
    }

    public static void flushDeferred() {
        LOCK.lock();

        try {
            if (DEFERRED_LINES.isEmpty()) {
                deferBackgroundOutput = false;
                return;
            }

            List<String> lines = List.copyOf(DEFERRED_LINES);
            DEFERRED_LINES.clear();
            deferBackgroundOutput = false;

            for (String line : lines) {
                printLine(line);
            }
        } finally {
            LOCK.unlock();
        }
    }

    public static void discardDeferred() {
        LOCK.lock();

        try {
            DEFERRED_LINES.clear();
            deferBackgroundOutput = false;
        } finally {
            LOCK.unlock();
        }
    }

    public static void runDirect(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("runnable must not be null");
        }

        boolean previous = DIRECT_OUTPUT.get();
        DIRECT_OUTPUT.set(true);

        try {
            runnable.run();
        } finally {
            DIRECT_OUTPUT.set(previous);
        }
    }

    public static void println(String message) {
        String safeMessage = message == null ? "" : message;

        LOCK.lock();

        try {
            if (deferBackgroundOutput && !DIRECT_OUTPUT.get()) {
                DEFERRED_LINES.add(safeMessage);
                return;
            }

            printLine(safeMessage);
        } finally {
            LOCK.unlock();
        }
    }

    public static void printRaw(String message) {
        String safeMessage = message == null ? "" : message;

        LOCK.lock();

        try {
            System.out.print(safeMessage);
            System.out.flush();
            updateTransientState(safeMessage);
        } finally {
            LOCK.unlock();
        }
    }

    public static void clearTransient() {
        LOCK.lock();

        try {
            clearTransientLine();
        } finally {
            LOCK.unlock();
        }
    }

    private static void printLine(String message) {
        clearTransientLine();

        LineReader reader = lineReader;

        if (reader != null) {
            reader.printAbove(message);
            return;
        }

        System.out.println(message);
    }

    private static void clearTransientLine() {
        if (!transientLineActive) {
            return;
        }

        System.out.print(CLEAR_LINE);
        System.out.flush();
        transientLineActive = false;
    }

    private static void updateTransientState(String message) {
        if (message.isEmpty()) {
            return;
        }

        if (message.endsWith("\n") || message.endsWith("\r\n")) {
            transientLineActive = false;
            return;
        }

        if (message.contains("\r")) {
            transientLineActive = true;
        }
    }
}
