package eu.kryocloud.node.console;

import eu.kryocloud.common.logging.ConsoleOutput;
import eu.kryocloud.node.KryoNode;
import eu.kryocloud.node.console.tui.ConsoleTheme;
import eu.kryocloud.node.console.tui.Glyph;
import eu.kryocloud.node.console.tui.Tone;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public record ConsoleContext(KryoNode node, Terminal terminal, LineReader reader, AtomicBoolean running, KryoCompleter completer) {

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

        if (completer == null) {
            throw new IllegalArgumentException("completer must not be null");
        }
    }

    public void print(String message) {
        ConsoleOutput.println(message);
    }

    public void raw(String message) {
        ConsoleOutput.printRaw(message);
    }

    public void success(String message) {
        print("  " + Tone.SUCCESS.paint(Glyph.CHECK.value()) + "  " + Tone.FROST.paint(message));
    }

    public void warn(String message) {
        print("  " + Tone.WARNING.paint("!") + "  " + Tone.WARNING.paint(message));
    }

    public void error(String message) {
        print("  " + Tone.DANGER.paint(Glyph.CROSS.value()) + "  " + Tone.DANGER.paint(message));
    }

    public void info(String message) {
        print("  " + Tone.INFO.paint(Glyph.DOT.value()) + "  " + Tone.FROST.paint(message));
    }

    public void header(String title) {
        print("");
        print(" " + Tone.PRIMARY.paint(Glyph.SNOWFLAKE.value()) + " " + Tone.SHIMMER.paintBold(title));
        print(" " + ConsoleTheme.frostDivider(Math.max(36, title.length() + 14)));
    }

    public void row(String label, String value) {
        print("   " + Tone.SECONDARY.paint(label + ":") + " " + Tone.CRYSTAL.paint(value));
    }

    public void bullet(String value) {
        print("   " + ConsoleTheme.bullet() + " " + value);
    }

    public String ask(String question, String fallback) {
        String suffix = "";

        if (fallback != null && !fallback.isBlank()) {
            suffix = " " + muted("[" + fallback + "]");
        }

        String prompt = "  " + Tone.PRIMARY.paint(Glyph.ARROW.value()) + "  " + Tone.PRIMARY.paint(question) + suffix + " ";
        String answer = reader.readLine(prompt);

        if (answer == null || answer.isBlank()) {
            return fallback;
        }

        return answer;
    }

    public String ask(String question, String fallback, Collection<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return ask(question, fallback);
        }

        return completer.withPromptCandidates(candidates, () -> ask(question, fallback));
    }

    public boolean confirm(String question, boolean fallback) {
        String fallbackText = fallback ? "Y/n" : "y/N";

        while (true) {
            String answer = ask(question + " " + muted("(" + fallbackText + ")"), fallback ? "y" : "n", List.of("y", "j", "yes", "ja", "n", "no", "nein"));
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
        return ConsoleTheme.code(value);
    }

    public String muted(String value) {
        return ConsoleTheme.muted(value);
    }

    public String accent(String value) {
        return Tone.PRIMARY.paint(value);
    }

    public String good(String value) {
        return Tone.SUCCESS.paint(value);
    }

    public String bad(String value) {
        return Tone.DANGER.paint(value);
    }

    public String yellow(String value) {
        return Tone.WARNING.paint(value);
    }

    public String value(String value) {
        return Tone.CRYSTAL.paint(value);
    }

    public String label(String value) {
        return Tone.SECONDARY.paint(value);
    }

    public void stopConsole() {
        running.set(false);
    }
}
