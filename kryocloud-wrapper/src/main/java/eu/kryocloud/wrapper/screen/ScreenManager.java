package eu.kryocloud.wrapper.screen;

import eu.kryocloud.api.screen.IScreen;
import eu.kryocloud.api.screen.IScreenManager;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ScreenManager implements IScreenManager {

    private final ConcurrentMap<String, IScreen> sessions = new ConcurrentHashMap<>();
    private final boolean windows;

    public ScreenManager() {
        this.windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @Override
    public IScreen create(String session, Path workingDirectory) {
        validateSession(session);

        if (workingDirectory == null) {
            throw new IllegalArgumentException("workingDirectory must not be null");
        }

        IScreen screen = createScreen(session, workingDirectory);
        IScreen existing = sessions.putIfAbsent(session, screen);

        if (existing != null) {
            throw new IllegalStateException("Screen session already exists: " + session);
        }

        return screen;
    }

    @Override
    public IScreen get(String session) {
        validateSession(session);
        return sessions.get(session);
    }

    @Override
    public void remove(String session) {
        validateSession(session);

        IScreen screen = sessions.remove(session);

        if (screen == null) {
            return;
        }

        try {
            screen.stop();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to stop screen session " + session, exception);
        }
    }

    public int size() {
        return sessions.size();
    }

    public Map<String, IScreen> sessions() {
        return Map.copyOf(sessions);
    }

    public void clear() {
        for (String session : sessions.keySet()) {
            remove(session);
        }
    }

    private IScreen createScreen(String session, Path workingDirectory) {
        if (windows) {
            return new WindowsScreen(session, workingDirectory);
        }

        return new UnixScreen(session, workingDirectory);
    }

    private void validateSession(String session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }

        if (session.isBlank()) {
            throw new IllegalArgumentException("session must not be blank");
        }
    }
}