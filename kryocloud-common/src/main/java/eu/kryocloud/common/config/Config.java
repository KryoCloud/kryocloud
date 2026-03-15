package eu.kryocloud.common.config;

import eu.kryocloud.api.config.IConfig;
import eu.kryocloud.api.config.type.IConfigTypeProvider;
import eu.kryocloud.common.config.type.ConfigType;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Config implements IConfig {

    private final Path file;
    private final IConfigTypeProvider provider;
    private final Map<String, Object> defaults = new ConcurrentHashMap<>();

    public Config(Path file) {
        this.file = Objects.requireNonNull(file);
        ConfigType type = ConfigType.fromFileName(file.getFileName().toString());
        this.provider = type.getTypeProvider();
    }

    @Override
    public void load() {
        try {
            provider.load(file);

            if (defaults.isEmpty()) {
                return;
            }

            defaults.forEach((key, value) -> {
                if (provider.contains(key)) {
                    return;
                }
                provider.set(key, value);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + file, e);
        }
    }

    @Override
    public void save() {
        try {
            provider.save(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save config: " + file, e);
        }
    }

    @Override
    public void reload() {
        load();
    }

    @Override
    public void addDefault(String path, Object value) {
        defaults.put(path, value);

        if (provider.contains(path)) {
            return;
        }
        provider.set(path, value);
    }

    @Override
    public void put(String path, Object value) {
        provider.set(path, value);
    }

    @Override
    public <T> T get(String path, Class<T> type) {
        return provider.get(path, type);
    }
}
