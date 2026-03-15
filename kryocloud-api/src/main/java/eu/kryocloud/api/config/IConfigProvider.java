package eu.kryocloud.api.config;

import java.nio.file.Path;

public interface IConfigProvider {

    <T> T registerConfig(Path path, Class<T> clazz);
    void unregisterConfig(Path path);

}
