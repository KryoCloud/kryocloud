package eu.kryocloud.wrapper.server;

import eu.kryocloud.api.screen.IScreen;
import eu.kryocloud.api.server.ICloudServer;

import java.nio.file.Path;
import java.util.List;

public class CloudServer implements ICloudServer {

    private final String name;
    private final Path workingDirectory;
    private final int minMemory;
    private final int maxMemory;
    private final List<String> jvmArgs;
    private final String jar;
    private final IScreen screen;

    public CloudServer(String name, Path workingDirectory, int minMemory, int maxMemory, List<String> jvmArgs, String jar, IScreen screen) {
        this.name = name;
        this.workingDirectory = workingDirectory;
        this.minMemory = minMemory;
        this.maxMemory = maxMemory;
        this.jvmArgs = jvmArgs;
        this.jar = jar;
        this.screen = screen;
    }

    @Override
    public void start() throws Exception {
        String command = "java " + "-Xms" + minMemory + "M -Xmx" + maxMemory + "M " + String.join(" ", jvmArgs) + " -jar " + jar + " --nogui";
        screen.start(command);
    }

    @Override
    public void stop() throws Exception {
        screen.send("stop");
    }

    @Override
    public void command(String command) throws Exception {
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
}
