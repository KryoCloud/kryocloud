package eu.kryocloud.wrapper.server;

import eu.kryocloud.api.screen.IScreen;
import eu.kryocloud.api.server.ICloudServer;

public class CloudServer implements ICloudServer {

    private final String name;
    private final String path;
    private final String jar;
    private final IScreen screen;

    public CloudServer(String name, String path, String jar, IScreen screen) {
        this.name = name;
        this.path = path;
        this.jar = jar;
        this.screen = screen;
    }

    @Override
    public void start() throws Exception {
        String command = "java -jar " + jar + " --nogui";
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
}
