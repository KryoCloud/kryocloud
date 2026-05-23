package eu.kryocloud.api.server;

import java.nio.file.Path;

public interface ICloudServer {

    void start() throws Exception;
    void stop() throws Exception;

    void command(String command) throws Exception;

    boolean isOnline() throws Exception;
    String logs() throws Exception;
    Path workingDirectory();

}
