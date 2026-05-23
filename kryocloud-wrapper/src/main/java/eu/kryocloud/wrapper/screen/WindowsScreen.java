package eu.kryocloud.wrapper.screen;

import eu.kryocloud.api.screen.IScreen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public final class WindowsScreen implements IScreen {

    private final String session;
    private final Path workingDirectory;
    private final StringBuilder outputBuffer = new StringBuilder();
    private final ReentrantLock ioLock = new ReentrantLock();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile Process process;
    private volatile BufferedWriter writer;
    private volatile Thread readerThread;

    public WindowsScreen(String session, Path workingDirectory) {
        validateSession(session);

        if (workingDirectory == null) {
            throw new IllegalArgumentException("workingDirectory must not be null");
        }

        this.session = session;
        this.workingDirectory = workingDirectory;
    }

    @Override
    public void start(String command) throws Exception {
        validateCommand(command);

        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Windows screen session is already running: " + session);
        }

        try {
            Files.createDirectories(workingDirectory);

            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe");
            processBuilder.directory(workingDirectory.toFile());
            processBuilder.redirectErrorStream(true);

            Process startedProcess = processBuilder.start();
            BufferedWriter startedWriter = new BufferedWriter(new OutputStreamWriter(startedProcess.getOutputStream()));

            process = startedProcess;
            writer = startedWriter;
            readerThread = createReaderThread(startedProcess);
            readerThread.start();

            send(command);
        } catch (Exception exception) {
            running.set(false);
            closeWriter();
            destroyProcess();
            throw exception;
        }
    }

    @Override
    public void send(String command) throws Exception {
        validateCommand(command);

        ioLock.lock();

        try {
            BufferedWriter activeWriter = writer;

            if (activeWriter == null) {
                throw new IllegalStateException("Windows screen writer is not available for session: " + session);
            }

            activeWriter.write(command);
            activeWriter.newLine();
            activeWriter.flush();
        } finally {
            ioLock.unlock();
        }
    }

    @Override
    public String capture() {
        ioLock.lock();

        try {
            return outputBuffer.toString();
        } finally {
            ioLock.unlock();
        }
    }

    @Override
    public void stop() throws Exception {
        if (!running.getAndSet(false)) {
            return;
        }

        try {
            send("exit");
        } catch (Exception exception) {
            appendLine("Failed to send exit to Windows screen " + session + ": " + exception.getMessage());
        }

        closeWriter();
        destroyProcess();
    }

    @Override
    public boolean exists() {
        Process activeProcess = process;
        return running.get() && activeProcess != null && activeProcess.isAlive();
    }

    private Thread createReaderThread(Process activeProcess) {
        Thread thread = new Thread(() -> readOutput(activeProcess), "kryocloud-screen-" + session + "-reader");
        thread.setDaemon(true);
        return thread;
    }

    private void readOutput(Process activeProcess) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(activeProcess.getInputStream()))) {
            String line = reader.readLine();

            while (line != null) {
                appendLine(line);
                line = reader.readLine();
            }
        } catch (IOException exception) {
            appendLine("Windows screen reader failed for " + session + ": " + exception.getMessage());
        } finally {
            running.set(false);
            closeWriter();
        }
    }

    private void appendLine(String line) {
        ioLock.lock();

        try {
            outputBuffer.append(line).append('\n');
        } finally {
            ioLock.unlock();
        }
    }

    private void closeWriter() {
        ioLock.lock();

        try {
            BufferedWriter activeWriter = writer;
            writer = null;

            if (activeWriter == null) {
                return;
            }

            activeWriter.close();
        } catch (IOException exception) {
            outputBuffer.append("Failed to close Windows screen writer for ").append(session).append(": ").append(exception.getMessage()).append('\n');
        } finally {
            ioLock.unlock();
        }
    }

    private void destroyProcess() {
        Process activeProcess = process;
        process = null;

        if (activeProcess == null) {
            return;
        }

        if (!activeProcess.isAlive()) {
            return;
        }

        activeProcess.destroy();
    }

    private void validateSession(String session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }

        if (session.isBlank()) {
            throw new IllegalArgumentException("session must not be blank");
        }
    }

    private void validateCommand(String command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }

        if (command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }
    }
}