package eu.kryocloud.common.logging;

import org.jline.reader.LineReader;

import java.util.concurrent.locks.ReentrantLock;

public final class ConsoleOutput {

    private static final String CLEAR_LINE = "\r\u001B[2K";
    private static final ReentrantLock LOCK = new ReentrantLock();

    private static volatile LineReader lineReader;
    private static volatile boolean transientLineActive;

    private ConsoleOutput() {
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

    public static void println(String message) {
        String safeMessage = message == null ? "" : message;

        LOCK.lock();

        try {
            clearTransientLine();

            LineReader reader = lineReader;

            if (reader != null) {
                reader.printAbove(safeMessage);
                return;
            }

            System.out.println(safeMessage);
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
