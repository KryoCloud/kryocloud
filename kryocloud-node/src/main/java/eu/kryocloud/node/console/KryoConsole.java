package eu.kryocloud.node.console;

import eu.kryocloud.common.layout.KryoDirectoryLayout;
import eu.kryocloud.common.logging.ConsoleOutput;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.node.KryoNode;
import eu.kryocloud.node.console.tui.ConsoleIntro;
import eu.kryocloud.node.console.tui.KryoPrompt;
import org.jline.reader.EndOfFileException;
import org.jline.reader.History;
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
import java.util.concurrent.atomic.AtomicLong;

public final class KryoConsole implements AutoCloseable {

    private static final KryoLogger LOGGER = KryoLogger.logger("Console");
    private static final long INTERRUPT_CONFIRMATION_WINDOW_MILLIS = 5_000L;

    private final KryoNode node;
    private final CommandRegistry commandRegistry;
    private final KryoPrompt prompt;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastInterruptAtMillis = new AtomicLong(0L);

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
        ConsoleOutput.prepareInteractiveConsole();
        Thread.currentThread().setContextClassLoader(KryoConsole.class.getClassLoader());

        try (Terminal terminal = createTerminal()) {
            KryoCompleter completer = new KryoCompleter(new KryoCommandCompleter(node, commandRegistry));
            Path historyFile = historyFile();
            DefaultHistory history = new DefaultHistory();
            LineReader activeReader = LineReaderBuilder.builder().terminal(terminal).parser(createParser()).history(history).completer(completer).variable(LineReader.HISTORY_FILE, historyFile).variable(LineReader.HISTORY_SIZE, 10_000).variable(LineReader.LIST_MAX, 100).build();
            configureReader(activeReader);
            loadHistory(history);
            ConsoleContext context = new ConsoleContext(node, terminal, activeReader, running, completer);

            reader = activeReader;
            ConsoleOutput.attach(activeReader);
            ConsoleOutput.runDirect(() -> new ConsoleIntro().play(context));
            ConsoleOutput.flushDeferred();
            node.markConsoleReady();

            while (running.get()) {
                readAndExecute(context, activeReader);
            }

            saveHistory(activeReader.getHistory());
            ConsoleOutput.detach(activeReader);
        } catch (Exception exception) {
            LOGGER.error("Console crashed", exception);
        }
    }

    private Terminal createTerminal() throws Exception {
        try {
            TerminalBuilder builder = TerminalBuilder.builder().system(true).nativeSignals(false);
            setBoolean(builder, "jna", true);
            setBoolean(builder, "jansi", true);
            setBoolean(builder, "dumb", false);
            return builder.build();
        } catch (Exception exception) {
            LOGGER.info("Using limited terminal mode: " + exception.getMessage());
            TerminalBuilder builder = TerminalBuilder.builder().system(true).nativeSignals(false);
            setBoolean(builder, "dumb", true);
            return builder.build();
        }
    }

    private void setBoolean(TerminalBuilder builder, String method, boolean value) {
        try {
            builder.getClass().getMethod(method, boolean.class).invoke(builder, value);
        } catch (Exception ignored) {
        }
    }

    private DefaultParser createParser() {
        DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedQuote(false);
        return parser;
    }

    private Path historyFile() {
        try {
            Path historyFile = KryoDirectoryLayout.CONFIG.resolve("console.history");
            Files.createDirectories(historyFile.getParent());
            return historyFile;
        } catch (Exception exception) {
            LOGGER.warn("Failed to prepare console history: " + exception.getMessage());
            return Path.of(".kryocloud-console.history").toAbsolutePath().normalize();
        }
    }

    private void configureReader(LineReader activeReader) {
        activeReader.option(LineReader.Option.HISTORY_IGNORE_DUPS, true);
        activeReader.option(LineReader.Option.HISTORY_REDUCE_BLANKS, true);
        activeReader.option(LineReader.Option.AUTO_LIST, true);
        activeReader.option(LineReader.Option.AUTO_MENU, true);
        activeReader.option(LineReader.Option.COMPLETE_IN_WORD, true);
        activeReader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);
    }

    private void loadHistory(History history) {
        try {
            history.load();
        } catch (Exception ignored) {
        }
    }

    private void saveHistory(History history) {
        try {
            history.save();
        } catch (Exception ignored) {
        }
    }

    private void readAndExecute(ConsoleContext context, LineReader activeReader) {
        try {
            String line = activeReader.readLine(prompt.render());
            lastInterruptAtMillis.set(0L);
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

        long now = System.currentTimeMillis();
        long previous = lastInterruptAtMillis.getAndSet(now);

        if (previous < 1 || now - previous > INTERRUPT_CONFIRMATION_WINDOW_MILLIS) {
            ConsoleOutput.clearTransient();
            context.warn("Press CTRL+C again within 5 seconds to shutdown KryoCloud.");
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
