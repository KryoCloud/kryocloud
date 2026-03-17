package eu.kryocloud.api.config;

import java.nio.file.Path;

public interface IConfigProvider {

    <T> T registerConfig(Path path, Class<T> clazz) throws Exception;

    void unregisterConfig(Class<?> clazz);

    <T> T getConfig(Class<T> clazz);

}
