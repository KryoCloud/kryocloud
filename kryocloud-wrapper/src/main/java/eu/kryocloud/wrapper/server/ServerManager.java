package eu.kryocloud.wrapper.server;

import eu.kryocloud.api.screen.IScreenManager;
import eu.kryocloud.api.server.ICloudServer;
import eu.kryocloud.api.server.IServerManager;

import java.util.concurrent.ConcurrentHashMap;

public final class ServerManager implements IServerManager {

    private final ConcurrentHashMap<String, ICloudServer> servers = new ConcurrentHashMap<>();
    private final IScreenManager screenManager;

    public ServerManager(IScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    @Override
    public ICloudServer create(String name, String path, String jar) {
        var screen = screenManager.create(name);
        var server = new CloudServer(name, path, jar, screen);

        servers.put(name, server);
        return server;
    }

    @Override
    public ICloudServer get(String name) {
        return servers.get(name);
    }

    @Override
    public void remove(String name) {
        servers.remove(name);
    }
}