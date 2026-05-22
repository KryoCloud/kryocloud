package eu.kryocloud.common.logging;

import org.jline.reader.LineReader;

import java.util.concurrent.locks.ReentrantLock;

public final class ConsoleOutput {

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static volatile LineReader lineReader;

    private ConsoleOutput() {
    }

    public static void attach(LineReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader must not be null");
        }

        lineReader = reader;
    }

    public static void detach(LineReader reader) {
        if (reader == null) {
            return;
        }

        if (lineReader != reader) {
            return;
        }

        lineReader = null;
    }

    public static void println(String message) {
        String safeMessage = message == null ? "" : message;

        LOCK.lock();

        try {
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
        } finally {
            LOCK.unlock();
        }
    }
}