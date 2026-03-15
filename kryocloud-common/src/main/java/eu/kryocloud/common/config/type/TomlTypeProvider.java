package eu.kryocloud.common.config.type;

import eu.kryocloud.api.config.type.IConfigTypeProvider;

import java.nio.file.Path;
import java.util.*;

public class TomlTypeProvider implements IConfigTypeProvider {

    @Override
    public void load(Path file) throws Exception {

    }

    @Override
    public void save(Path file) throws Exception {

    }

    @Override
    public void set(String path, Object value) {

    }

    @Override
    public void remove(String path) {

    }

    @Override
    public boolean contains(String path) {
        return false;
    }

    @Override
    public <T> T get(String path, Class<T> type) {
        return null;
    }

    @Override
    public Set<String> keys(String path) {
        return Set.of();
    }
}
