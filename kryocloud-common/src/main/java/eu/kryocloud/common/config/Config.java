package eu.kryocloud.common.config;

import eu.kryocloud.api.config.IConfig;
import eu.kryocloud.api.config.type.IConfigTypeProvider;
import eu.kryocloud.common.config.type.ConfigType;

import java.lang.reflect.Field;
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

            populateFields();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + file, e);
        }
    }

    private void populateFields() {
        Class<?> clazz = this.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals("file") || field.getName().equals("provider") || field.getName().equals("defaults")) {
                continue;
            }

            try {
                field.setAccessible(true);
                Class<?> fieldType = getWrapperType(field.getType());
                Object value = provider.get(field.getName(), fieldType);
                if (value != null) {
                    field.set(this, value);
                }
            } catch (IllegalAccessException _) {
            }
        }
    }

    private Class<?> getWrapperType(Class<?> type) {
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
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

        Field[] fields = this.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
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
