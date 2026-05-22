package eu.kryocloud.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class KryoLogger {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static volatile boolean colorsEnabled = true;
    private static volatile boolean debugEnabled = false;
    private static volatile boolean stackTracesEnabled = true;

    private final String name;

    public KryoLogger(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        this.name = name;
    }

    public static KryoLogger logger(String name) {
        return new KryoLogger(name);
    }

    public static void colorsEnabled(boolean enabled) {
        colorsEnabled = enabled;
    }

    public static void debugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static void stackTracesEnabled(boolean enabled) {
        stackTracesEnabled = enabled;
    }

    public void info(String message) {
        log("INFO", TextColor.hex("#5EEAD4"), message, null);
    }

    public void success(String message) {
        log("OK", TextColor.hex("#3DDC97"), message, null);
    }

    public void warn(String message) {
        log("WARN", TextColor.hex("#FFD166"), message, null);
    }

    public void error(String message) {
        log("ERROR", TextColor.hex("#FF5C5C"), message, null);
    }

    public void error(String message, Throwable throwable) {
        log("ERROR", TextColor.hex("#FF5C5C"), message, throwable);
    }

    public void debug(String message) {
        if (!debugEnabled) {
            return;
        }

        log("DEBUG", TextColor.hex("#8B949E"), message, null);
    }

    private void log(String level, TextColor color, String message, Throwable throwable) {
        String time = LocalTime.now().format(TIME_FORMAT);
        String prefix = "[" + time + " " + level + " " + name + "] ";
        String line = color(prefix, color) + safe(message);

        ConsoleOutput.println(line);

        if (throwable == null) {
            return;
        }

        if (!stackTracesEnabled) {
            return;
        }

        ConsoleOutput.println(color(stackTrace(throwable), TextColor.hex("#8B949E")));
    }

    private String color(String value, TextColor color) {
        if (!colorsEnabled) {
            return value;
        }

        return color.apply(value);
    }

    private String safe(String message) {
        if (message == null) {
            return "";
        }

        return message;
    }

    private String stackTrace(Throwable throwable) {
        StringWriter output = new StringWriter();
        throwable.printStackTrace(new PrintWriter(output));
        return output.toString();
    }
}