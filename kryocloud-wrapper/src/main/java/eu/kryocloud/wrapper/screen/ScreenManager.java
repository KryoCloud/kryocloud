package eu.kryocloud.wrapper.screen;

import eu.kryocloud.api.screen.IScreen;
import eu.kryocloud.api.screen.IScreenManager;

import java.util.concurrent.ConcurrentHashMap;

public class ScreenManager implements IScreenManager {

    private final ConcurrentHashMap<String, IScreen> sessions = new ConcurrentHashMap<>();
    private final boolean isWindows;

    public ScreenManager() {
        String os = System.getProperty("os.name").toLowerCase();
        isWindows = os.contains("win");
    }

    public IScreen create(String session) {
        IScreen screen = isWindows
                ? new WindowsScreen(session)
                : new UnixScreen(session);

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