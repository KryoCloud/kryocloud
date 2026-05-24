package eu.kryocloud.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class KryoLogger {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final TextColor SNOWFLAKE_COLOR = TextColor.hex("#7DD3FC");
    private static final TextColor TIME_COLOR = TextColor.hex("#7C8EA3");
    private static final TextColor NAME_COLOR = TextColor.hex("#38BDF8");
    private static final TextColor MESSAGE_COLOR = TextColor.hex("#E0F2FE");
    private static final TextColor STACK_COLOR = TextColor.hex("#7C8EA3");

    private static final TextColor INFO_COLOR = TextColor.hex("#7DD3FC");
    private static final TextColor SUCCESS_COLOR = TextColor.hex("#34D399");
    private static final TextColor WARN_COLOR = TextColor.hex("#FDE68A");
    private static final TextColor ERROR_COLOR = TextColor.hex("#FCA5A5");
    private static final TextColor DEBUG_COLOR = TextColor.hex("#7C8EA3");

    private static final String SNOWFLAKE = "❄";

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
        log("INFO ", INFO_COLOR, message, null);
    }

    public void success(String message) {
        log("OK   ", SUCCESS_COLOR, message, null);
    }

    public void warn(String message) {
        log("WARN ", WARN_COLOR, message, null);
    }

    public void error(String message) {
        log("ERROR", ERROR_COLOR, message, null);
    }

    public void error(String message, Throwable throwable) {
        log("ERROR", ERROR_COLOR, message, throwable);
    }

    public void debug(String message) {
        if (!debugEnabled) {
            return;
        }

        log("DEBUG", DEBUG_COLOR, message, null);
    }

    private void log(String level, TextColor levelColor, String message, Throwable throwable) {
        String time = LocalTime.now().format(TIME_FORMAT);
        String line = paint(" " + SNOWFLAKE + " ", SNOWFLAKE_COLOR) + paint(time, TIME_COLOR) + "  " + paintBold(level, levelColor) + "  " + paint(padRight(name, 12), NAME_COLOR) + "  " + paint(safe(message), MESSAGE_COLOR);

        ConsoleOutput.println(line);

        if (throwable == null) {
            return;
        }

        if (!stackTracesEnabled) {
            return;
        }

        ConsoleOutput.println(paint(stackTrace(throwable), STACK_COLOR));
    }

    private String paint(String value, TextColor color) {
        if (!colorsEnabled) {
            return value;
        }

        return color.apply(value);
    }

    private String paintBold(String value, TextColor color) {
        if (!colorsEnabled) {
            return value;
        }

        return TextColor.BOLD.code() + color.apply(value);
    }

    private String padRight(String value, int width) {
        if (value.length() >= width) {
            return value;
        }

        return value + " ".repeat(width - value.length());
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
