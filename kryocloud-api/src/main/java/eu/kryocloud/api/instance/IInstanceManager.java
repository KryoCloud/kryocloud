package eu.kryocloud.api.instance;

import java.nio.file.Path;
import java.util.List;

public interface IInstanceManager {

    ICloudInstance create(String name, String javaExecutable, Path workingDirectory, int minMemory, int maxMemory, List<String> jvmArgs);

    ICloudInstance get(String name);

    void remove(String name);
}
