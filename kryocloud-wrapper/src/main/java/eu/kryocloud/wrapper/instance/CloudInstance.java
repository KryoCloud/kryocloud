package eu.kryocloud.wrapper.instance;

import eu.kryocloud.api.instance.ICloudInstance;
import eu.kryocloud.api.screen.IScreen;
import eu.kryocloud.wrapper.instance.process.InstanceProcessSpec;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CloudInstance implements ICloudInstance {

    private final InstanceProcessSpec processSpec;
    private final IScreen screen;

    public CloudInstance(InstanceProcessSpec processSpec, IScreen screen) {
        if (processSpec == null) {
            throw new IllegalArgumentException("processSpec must not be null");
        }

        if (screen == null) {
            throw new IllegalArgumentException("screen must not be null");
        }

        this.processSpec = processSpec;
        this.screen = screen;
    }

    @Override
    public void start() throws Exception {
        Path jarPath = workingDirectory().resolve(processSpec.jarName());

        if (!Files.exists(jarPath)) {
            throw new IllegalStateException("Minecraft server jar does not exist: " + jarPath);
        }

        Files.createDirectories(workingDirectory().resolve("logs"));
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
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }

        if (!screen.exists()) {
            throw new IllegalStateException("Minecraft instance screen is not running: " + processSpec.name());
        }

        screen.send(command);
    }

    @Override
    public boolean isOnline() throws Exception {
        return screen.exists();
    }

    @Override
    public String logs() throws Exception {
        Path logFile = logFile();

        if (Files.exists(logFile)) {
            return Files.readString(logFile);
        }

        return screen.capture();
    }

    @Override
    public Path workingDirectory() {
        return processSpec.workingDirectory();
    }

    public InstanceProcessSpec processSpec() {
        return processSpec;
    }

    private String commandLine() {
        String args = String.join(" ", processSpec.jvmArgs()).trim();
        String javaCommand = quote(processSpec.javaExecutable()) + " -Xms" + processSpec.minMemoryMb() + "M -Xmx" + processSpec.maxMemoryMb() + "M -jar " + processSpec.jarName() + " nogui";

        if (!args.isBlank()) {
            javaCommand = quote(processSpec.javaExecutable()) + " -Xms" + processSpec.minMemoryMb() + "M -Xmx" + processSpec.maxMemoryMb() + "M " + args + " -jar " + processSpec.jarName() + " nogui";
        }

        if (windows()) {
            return "if not exist logs mkdir logs && " + javaCommand + " > logs\\kryocloud-instance.log 2>&1 & exit";
        }

        return "mkdir -p logs && " + javaCommand + " 2>&1 | tee -a logs/kryocloud-instance.log";
    }

    private String quote(String value) {
        if (value.contains(" ")) {
            return "\"" + value + "\"";
        }

        return value;
    }

    private Path logFile() {
        return workingDirectory().resolve("logs").resolve("kryocloud-instance.log");
    }

    private boolean windows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
