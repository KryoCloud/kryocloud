package eu.kryocloud.wrapper;

import eu.kryocloud.api.screen.IScreenManager;
import eu.kryocloud.api.server.IServerManager;
import eu.kryocloud.api.wrapper.IWrapper;
import eu.kryocloud.wrapper.screen.ScreenManager;
import eu.kryocloud.wrapper.server.ServerManager;

public class KryoWrapper implements IWrapper {

    private final IScreenManager screenManager;
    private final IServerManager serverManager;

    public KryoWrapper() {
        try {
            this.screenManager = new ScreenManager();
            this.serverManager = new ServerManager(screenManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public IServerManager serverManager() {
        return serverManager;
    }
}
