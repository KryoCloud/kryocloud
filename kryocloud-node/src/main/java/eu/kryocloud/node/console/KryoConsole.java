package eu.kryocloud.node.console;

import eu.kryocloud.common.layout.KryoDirectoryLayout;
import eu.kryocloud.common.logging.ConsoleOutput;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.node.KryoNode;
import eu.kryocloud.node.console.tui.ConsoleIntro;
import eu.kryocloud.node.console.tui.KryoPrompt;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class KryoConsole implements AutoCloseable {

    private static final KryoLogger LOGGER = KryoLogger.logger("Console");

    private final KryoNode node;
    private final CommandRegistry commandRegistry;
    private final KryoPrompt prompt;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile Thread thread;
    private volatile LineReader reader;

    public KryoConsole(KryoNode node, CommandRegistry commandRegistry) {
        this(node, commandRegistry, "node");
    }

    public KryoConsole(KryoNode node, CommandRegistry commandRegistry, String nodeName) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        if (commandRegistry == null) {
            throw new IllegalArgumentException("commandRegistry must not be null");
        }

        this.node = node;
        this.commandRegistry = commandRegistry;
        this.prompt = new KryoPrompt(nodeName);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        Thread consoleThread = new Thread(this::runConsole, "kryocloud-node-console");
        consoleThread.setDaemon(false);
        thread = consoleThread;
        consoleThread.start();
    }

    @Override
    public void close() {
        running.set(false);

        LineReader activeReader = reader;

        if (activeReader != null) {
            ConsoleOutput.detach(activeReader);
        }

        Thread consoleThread = thread;

        if (consoleThread == null) {
            return;
        }

        if (consoleThread == Thread.currentThread()) {
            return;
        }

        consoleThread.interrupt();
    }

    private void runConsole() {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            LineReader activeReader = LineReaderBuilder.builder().terminal(terminal).parser(new DefaultParser()).history(new DefaultHistory()).completer(new KryoCommandCompleter(node, commandRegistry)).build();
            prepareHistory(activeReader);
            ConsoleContext context = new ConsoleContext(node, terminal, activeReader, running);

            reader = activeReader;
            ConsoleOutput.attach(activeReader);
            new ConsoleIntro().play(context);
            node.markConsoleReady();

            while (running.get()) {
                readAndExecute(context, activeReader);
            }

            ConsoleOutput.detach(activeReader);
        } catch (Exception exception) {
            LOGGER.error("Console crashed", exception);
        }
    }

    private void prepareHistory(LineReader activeReader) {
        try {
            Path historyFile = KryoDirectoryLayout.CONFIG.resolve("console.history");
            Files.createDirectories(historyFile.getParent());
            activeReader.setVariable(LineReader.HISTORY_FILE, historyFile);
            activeReader.option(LineReader.Option.HISTORY_IGNORE_DUPS, true);
            activeReader.option(LineReader.Option.HISTORY_REDUCE_BLANKS, true);
        } catch (Exception exception) {
            LOGGER.warn("Failed to prepare console history: " + exception.getMessage());
        }
    }

    private void readAndExecute(ConsoleContext context, LineReader activeReader) {
        try {
            String line = activeReader.readLine(prompt.render());
            execute(context, line);
        } catch (UserInterruptException exception) {
            shutdownFromInterrupt(context);
        } catch (EndOfFileException exception) {
            context.stopConsole();
        } catch (Exception exception) {
            context.error(exception.getMessage());
        }
    }

    private void shutdownFromInterrupt(ConsoleContext context) {
        if (!running.get()) {
            return;
        }

        try {
            ConsoleOutput.clearTransient();
            commandRegistry.command("shutdown").orElseThrow(() -> new IllegalStateException("shutdown command is not registered")).execute(context, List.of());
        } catch (Exception exception) {
            context.error(exception.getMessage());
            context.stopConsole();
            node.shutdown();
        }
    }

    private void execute(ConsoleContext context, String line) throws Exception {
        if (line == null || line.isBlank()) {
            return;
        }

        List<String> parts = Arrays.stream(line.trim().split("\\s+")).filter(part -> !part.isBlank()).toList();

        if (parts.isEmpty()) {
            return;
        }

        String commandName = parts.getFirst();
        List<String> arguments = parts.subList(1, parts.size());
        ConsoleCommand command = commandRegistry.command(commandName).orElseThrow(() -> new IllegalArgumentException("Unknown command: " + commandName));

        command.execute(context, arguments);
    }
}
