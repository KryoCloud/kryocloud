package eu.kryocloud.node.console;

import eu.kryocloud.common.logging.ConsoleOutput;
import eu.kryocloud.node.KryoNode;
import eu.kryocloud.node.console.tui.ConsoleTheme;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public record ConsoleContext(KryoNode node, Terminal terminal, LineReader reader, AtomicBoolean running) {

    public ConsoleContext {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        if (terminal == null) {
            throw new IllegalArgumentException("terminal must not be null");
        }

        if (reader == null) {
            throw new IllegalArgumentException("reader must not be null");
        }

        if (running == null) {
            throw new IllegalArgumentException("running must not be null");
        }
    }

    public void print(String message) {
        ConsoleOutput.println(message);
    }

    public void raw(String message) {
        ConsoleOutput.printRaw(message);
    }

    public void success(String message) {
        print(ConsoleTheme.success(message));
    }

    public void warn(String message) {
        print(ConsoleTheme.warning(message));
    }

    public void error(String message) {
        print(ConsoleTheme.danger(message));
    }

    public void header(String message) {
        print("");
        print(ConsoleTheme.title(message));
        print(ConsoleTheme.divider(Math.max(24, message.length() + 8)));
    }

    public void row(String label, String value) {
        print("  " + ConsoleTheme.label(label + ": ") + ConsoleTheme.value(value));
    }

    public void bullet(String value) {
        print(" " + ConsoleTheme.bullet() + " " + value);
    }

    public String ask(String question, String fallback) {
        String suffix = "";

        if (fallback != null && !fallback.isBlank()) {
            suffix = " " + muted("[" + fallback + "]");
        }

        String answer = reader.readLine(ConsoleTheme.PRIMARY.apply(question) + suffix + " ");

        if (answer == null || answer.isBlank()) {
            return fallback;
        }

        return answer;
    }

    public boolean confirm(String question, boolean fallback) {
        String fallbackText = fallback ? "Y/n" : "y/N";

        while (true) {
            String answer = ask(question + " " + muted("(" + fallbackText + ")"), fallback ? "y" : "n");
            String normalized = answer.toLowerCase(Locale.ROOT);

            if ("y".equals(normalized) || "yes".equals(normalized) || "j".equals(normalized) || "ja".equals(normalized)) {
                return true;
            }

            if ("n".equals(normalized) || "no".equals(normalized) || "nein".equals(normalized)) {
                return false;
            }

            warn("Please answer with y/j or n.");
        }
    }

    public String code(String value) {
        return ConsoleTheme.command(value);
    }

    public String muted(String value) {
        return ConsoleTheme.muted(value);
    }

    public String accent(String value) {
        return ConsoleTheme.PRIMARY.apply(value);
    }

    public String good(String value) {
        return ConsoleTheme.success(value);
    }

    public String bad(String value) {
        return ConsoleTheme.danger(value);
    }

    public String yellow(String value) {
        return ConsoleTheme.warning(value);
    }

    public void stopConsole() {
        running.set(false);
    }
}
