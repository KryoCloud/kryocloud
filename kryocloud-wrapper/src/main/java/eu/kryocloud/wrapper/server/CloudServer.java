package eu.kryocloud.wrapper.server;

import eu.kryocloud.api.screen.IScreen;
import eu.kryocloud.api.server.ICloudServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CloudServer implements ICloudServer {

    private final String name;
    private final Path workingDirectory;
    private final int minMemory;
    private final int maxMemory;
    private final List<String> jvmArgs;
    private final String jar;
    private final IScreen screen;

    public CloudServer(String name, Path workingDirectory, int minMemory, int maxMemory, List<String> jvmArgs, String jar, IScreen screen) {
        validateName(name);

        if (workingDirectory == null) {
            throw new IllegalArgumentException("workingDirectory must not be null");
        }

        if (minMemory < 1) {
            throw new IllegalArgumentException("minMemory must be greater than 0");
        }

        if (maxMemory < minMemory) {
            throw new IllegalArgumentException("maxMemory must be greater than or equal to minMemory");
        }

        if (jvmArgs == null) {
            throw new IllegalArgumentException("jvmArgs must not be null");
        }

        if (jar == null || jar.isBlank()) {
            throw new IllegalArgumentException("jar must not be blank");
        }

        if (screen == null) {
            throw new IllegalArgumentException("screen must not be null");
        }

        this.name = name;
        this.workingDirectory = workingDirectory;
        this.minMemory = minMemory;
        this.maxMemory = maxMemory;
        this.jvmArgs = List.copyOf(jvmArgs);
        this.jar = jar;
        this.screen = screen;
    }

    @Override
    public void start() throws Exception {
        Path jarPath = workingDirectory.resolve(jar);

        if (!Files.exists(jarPath)) {
            throw new IllegalStateException("Server jar does not exist: " + jarPath);
        }

        screen.start(commandLine());
    }

    @Override
    public void stop() throws Exception {
        if (!screen.exists()) {
            return;
        }

        screen.send("stop");
    }

    @Override
    public void command(String command) throws Exception {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }

        if (command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }

        if (!screen.exists()) {
            throw new IllegalStateException("Server screen is not running: " + name);
        }

        screen.send(command);
    }

    @Override
    public boolean isOnline() throws Exception {
        return screen.exists();
    }

    @Override
    public String logs() throws Exception {
        return screen.capture();
    }

    @Override
    public Path workingDirectory() {
        return workingDirectory;
    }

    public String name() {
        return name;
    }

    public int minMemory() {
        return minMemory;
    }

    public int maxMemory() {
        return maxMemory;
    }

    public List<String> jvmArgs() {
        return jvmArgs;
    }

    public String jar() {
        return jar;
    }

    private String commandLine() {
        String args = String.join(" ", jvmArgs).trim();

        if (args.isBlank()) {
            return "java -Xms" + minMemory + "M -Xmx" + maxMemory + "M -jar " + jar + " nogui";
        }

        return "java -Xms" + minMemory + "M -Xmx" + maxMemory + "M " + args + " -jar " + jar + " nogui";
    }

    private void validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (!name.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("name contains unsupported characters: " + name);
        }
    }
}