package eu.kryocloud.api.screen;

import java.nio.file.Path;

public interface IScreenManager {

    IScreen create(String session, Path workingDirectory);

    IScreen get(String session);

    void remove(String session);

}
