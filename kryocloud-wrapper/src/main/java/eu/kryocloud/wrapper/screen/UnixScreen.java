package eu.kryocloud.wrapper.screen;

import eu.kryocloud.api.screen.IScreen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UnixScreen implements IScreen {

    private final String session;
    private final Path workingDirectory;

    public UnixScreen(String session, Path workingDirectory) {
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
        Files.createDirectories(workingDirectory);
        runAndRequireExit("screen", "-dmS", session, "bash", "-lc", command);
    }

    @Override
    public void send(String command) throws Exception {
        validateCommand(command);
        runAndRequireExit("screen", "-S", session, "-X", "stuff", command + "\n");
    }

    @Override
    public String capture() throws Exception {
        Path hardcopy = Files.createTempFile("kryocloud-screen-" + safePrefix(session) + "-", ".log");

        try {
            runAndRequireExit("screen", "-S", session, "-X", "hardcopy", hardcopy.toAbsolutePath().toString());
            return Files.readString(hardcopy, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(hardcopy);
        }
    }

    @Override
    public void stop() throws Exception {
        if (!exists()) {
            return;
        }

        runAndRequireExit("screen", "-S", session, "-X", "quit");
    }

    @Override
    public boolean exists() throws Exception {
        ProcessResult result = run("screen", "-ls");
        return result.output().contains("." + session);
    }

    private ProcessResult run(String... command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        byte[] output = process.getInputStream().readAllBytes();
        int exitCode = process.waitFor();

        return new ProcessResult(exitCode, new String(output, StandardCharsets.UTF_8));
    }

    private void runAndRequireExit(String... command) throws Exception {
        ProcessResult result = run(command);

        if (result.exitCode() == 0) {
            return;
        }

        throw new IOException("Screen command failed for session " + session + " with exit code " + result.exitCode() + ": " + result.output());
    }

    private void validateSession(String session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }

        if (session.isBlank()) {
            throw new IllegalArgumentException("session must not be blank");
        }

        if (!session.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("session contains unsupported characters: " + session);
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

    private String safePrefix(String value) {
        String prefix = value.replaceAll("[^A-Za-z0-9_.-]", "_");

        if (prefix.length() >= 3) {
            return prefix;
        }

        return "scr" + prefix;
    }

    private record ProcessResult(int exitCode, String output) {
    }
}