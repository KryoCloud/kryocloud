package eu.kryocloud.wrapper.screen;

import eu.kryocloud.api.screen.IScreen;
import eu.kryocloud.api.screen.IScreenManager;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class ScreenManager implements IScreenManager {

    private final ConcurrentHashMap<String, IScreen> sessions = new ConcurrentHashMap<>();
    private final boolean isWindows;

    public ScreenManager() {
        String os = System.getProperty("os.name").toLowerCase();
        isWindows = os.contains("win");
    }

    public IScreen create(String session, Path workingDirectory) {
        IScreen screen = isWindows
                ? new WindowsScreen(session, workingDirectory)
                : new UnixScreen(session, workingDirectory);

        sessions.put(session, screen);
        return screen;
    }

    public IScreen get(String session) {
        return sessions.get(session);
    }

    public void remove(String session) {
        sessions.remove(session);
    }
}