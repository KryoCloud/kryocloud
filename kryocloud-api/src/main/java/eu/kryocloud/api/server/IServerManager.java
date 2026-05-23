package eu.kryocloud.api.server;

import java.nio.file.Path;
import java.util.List;

public interface IServerManager {

    ICloudServer create(String name, Path workingDirectory, int minMemory, int maxMemory, List<String> jvmArgs);
    ICloudServer get(String name);
    void remove(String name);

}
