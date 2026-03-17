package eu.kryocloud.common.config;

import eu.kryocloud.api.config.IConfig;
import eu.kryocloud.api.config.type.IConfigTypeProvider;
import eu.kryocloud.common.config.codec.ConfigDecoder;
import eu.kryocloud.common.config.type.ConfigType;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Config implements IConfig {

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

            if (!defaults.isEmpty()) {
                defaults.forEach((key, value) -> {
                    if (provider.contains(key)) {
                        return;
                    }
                    provider.set(key, value);
                });
            }

            ConfigDecoder.decode(this, provider);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + file, e);
        }
    }

    @Override
    public void save() {
        try {
            provider.save(file, this);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");

        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            java.lang.reflect.Field field = fields[i];
            field.setAccessible(true);
            try {
                sb.append(field.getName()).append("=").append(field.get(this));
                if (i < fields.length - 1) {
                    sb.append(", ");
                }
            } catch (IllegalAccessException e) {
                sb.append(field.getName()).append("=<inaccessible>");
                if (i < fields.length - 1) {
                    sb.append(", ");
                }
            }
        }

        sb.append("}");
        return sb.toString();
    }
}
