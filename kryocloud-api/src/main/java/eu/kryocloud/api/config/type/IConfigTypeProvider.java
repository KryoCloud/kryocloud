package eu.kryocloud.api.config.type;

import java.nio.file.Path;
import java.util.Set;

public interface IConfigTypeProvider {

    void load(Path file) throws Exception;
    void save(Path file) throws Exception;

    void set(String path, Object value);
    void remove(String path);

    boolean contains(String path);

    <T> T get(String path, Class<T> type);

    Set<String> keys(String path);

}
