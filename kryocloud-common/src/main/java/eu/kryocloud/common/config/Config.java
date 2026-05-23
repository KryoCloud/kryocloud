package eu.kryocloud.common.config;

import eu.kryocloud.api.config.IConfig;
import eu.kryocloud.api.config.type.IConfigTypeProvider;
import eu.kryocloud.common.config.codec.ConfigDecoder;
import eu.kryocloud.common.config.type.ConfigType;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Config implements IConfig {

    private final Path file;
    private final IConfigTypeProvider provider;
    private final Map<String, Object> defaults = new ConcurrentHashMap<>();

    public Config(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }

        this.file = file;
        ConfigType type = ConfigType.fromFileName(file.getFileName().toString());
        this.provider = type.getTypeProvider();
    }

    @Override
    public void load() {
        try {
            provider.load(file);

            if (!defaults.isEmpty()) {
                defaults.forEach((key, value) -> {
                    if (provider.contains(key)) {
                        return;
                    }

                    provider.set(key, value);
                });
            }

            ConfigDecoder.decode(this, provider);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load config: " + file, exception);
        }
    }

    @Override
    public void save() {
        try {
            provider.save(file, this);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to save config: " + file, exception);
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

    @Override
    public IConfigTypeProvider getProvider() {
        return provider;
    }

    public Path file() {
        return file;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName()).append("{");

        java.lang.reflect.Field[] fields = getClass().getDeclaredFields();

        for (int index = 0; index < fields.length; index++) {
            java.lang.reflect.Field field = fields[index];
            field.setAccessible(true);

            try {
                builder.append(field.getName()).append("=").append(field.get(this));
            } catch (IllegalAccessException exception) {
                builder.append(field.getName()).append("=<inaccessible>");
            }

            if (index < fields.length - 1) {
                builder.append(", ");
            }
        }

        builder.append("}");
        return builder.toString();
    }
}
