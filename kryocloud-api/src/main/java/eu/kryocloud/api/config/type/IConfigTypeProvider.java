package eu.kryocloud.api.config.type;

import eu.kryocloud.api.config.IConfig;

import java.nio.file.Path;
import java.util.Set;

public interface IConfigTypeProvider {

    void load(Path file) throws Exception;
    void save(Path file, IConfig config) throws Exception;

    void set(String path, Object value);
    void remove(String path);

    boolean contains(String path);

    <T> T get(String path, Class<T> type);

    Set<String> keys(String path);

}
