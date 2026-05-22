package eu.kryocloud.node.console;

import eu.kryocloud.common.logging.ConsoleOutput;
import eu.kryocloud.common.logging.TextColor;
import eu.kryocloud.node.KryoNode;
import org.jline.terminal.Terminal;

import java.util.concurrent.atomic.AtomicBoolean;

public record ConsoleContext(KryoNode node, Terminal terminal, AtomicBoolean running) {

    public ConsoleContext {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        if (terminal == null) {
            throw new IllegalArgumentException("terminal must not be null");
        }

        if (running == null) {
            throw new IllegalArgumentException("running must not be null");
        }
    }

    public void print(String message) {
        ConsoleOutput.println(message);
    }

    public void success(String message) {
        print(TextColor.hex("#3DDC97").apply(message));
    }

    public void warn(String message) {
        print(TextColor.hex("#FFD166").apply(message));
    }

    public void error(String message) {
        print(TextColor.hex("#FF5C5C").apply(message));
    }

    public void header(String message) {
        print("");
        print(TextColor.BOLD.code() + TextColor.hex("#F8F9FA").code() + message + TextColor.RESET.code());
    }

    public String code(String value) {
        return TextColor.hex("#D0D7DE").apply(value);
    }

    public String muted(String value) {
        return TextColor.hex("#8B949E").apply(value);
    }

    public String accent(String value) {
        return TextColor.hex("#5EEAD4").apply(value);
    }

    public String good(String value) {
        return TextColor.hex("#3DDC97").apply(value);
    }

    public String bad(String value) {
        return TextColor.hex("#FF5C5C").apply(value);
    }

    public String yellow(String value) {
        return TextColor.hex("#FFD166").apply(value);
    }

    public void stopConsole() {
        running.set(false);
    }
}