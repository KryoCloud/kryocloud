package eu.kryocloud.wrapper.server;

import eu.kryocloud.api.screen.IScreen;
import eu.kryocloud.api.screen.IScreenManager;
import eu.kryocloud.api.server.ICloudServer;
import eu.kryocloud.api.server.IServerManager;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ServerManager implements IServerManager {

    private final ConcurrentMap<String, ICloudServer> servers = new ConcurrentHashMap<>();
    private final IScreenManager screenManager;

    public ServerManager(IScreenManager screenManager) {
        if (screenManager == null) {
            throw new IllegalArgumentException("screenManager must not be null");
        }

        this.screenManager = screenManager;
    }

    @Override
    public ICloudServer create(String name, Path workingDirectory, int minMemory, int maxMemory, List<String> jvmArgs) {
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

        IScreen screen = screenManager.create(name, workingDirectory);
        ICloudServer server = new CloudServer(name, workingDirectory, minMemory, maxMemory, jvmArgs, "server.jar", screen);
        ICloudServer existing = servers.putIfAbsent(name, server);

        if (existing != null) {
            screenManager.remove(name);
            throw new IllegalStateException("Server already exists: " + name);
        }

        return server;
    }

    @Override
    public ICloudServer get(String name) {
        validateName(name);
        return servers.get(name);
    }

    @Override
    public void remove(String name) {
        validateName(name);

        ICloudServer server = servers.remove(name);

        if (server != null) {
            stopSafely(name, server);
        }

        screenManager.remove(name);
    }

    public boolean contains(String name) {
        validateName(name);
        return servers.containsKey(name);
    }

    public int size() {
        return servers.size();
    }

    public Map<String, ICloudServer> servers() {
        return Map.copyOf(servers);
    }

    public void clear() {
        for (String name : servers.keySet()) {
            remove(name);
        }
    }

    private void stopSafely(String name, ICloudServer server) {
        try {
            server.stop();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to stop server " + name, exception);
        }
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