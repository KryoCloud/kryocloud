package eu.kryocloud.common.config.type;

import eu.kryocloud.api.config.type.IConfigTypeProvider;
import eu.kryocloud.common.config.codec.ConfigEncoder;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class TypeProvider implements IConfigTypeProvider {

    protected final LinkedHashMap<String, Object> data = new LinkedHashMap<>();

    @Override
    public void set(String path, Object value) {
        data.put(path, value);
    }

    @Override
    public void remove(String path) {
        data.remove(path);
    }

    @Override
    public boolean contains(String path) {
        return data.containsKey(path);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String path, Class<T> type) {
        Class<?> targetType = wrapType(type);
        Object value = data.get(path);

        if (value == null){
            return null;
        }

        if (targetType.isInstance(value)) {
            return (T) value;
        }

        switch (value) {
            case Number n when targetType == Integer.class -> {
                return (T) Integer.valueOf(n.intValue());
            }
            case Number n when targetType == Long.class -> {
                return (T) Long.valueOf(n.longValue());
            }
            case Number n when targetType == Double.class -> {
                return (T) Double.valueOf(n.doubleValue());
            }
            case Number n when targetType == Float.class -> {
                return (T) Float.valueOf(n.floatValue());
            }
            case Number n when targetType == Short.class -> {
                return (T) Short.valueOf(n.shortValue());
            }
            case Number n when targetType == Byte.class -> {
                return (T) Byte.valueOf(n.byteValue());
            }
            case Boolean b when targetType == Boolean.class -> {
                return (T) b;
            }
            case String s -> {
                return (T) convertStringValue(path, s, targetType);
            }
            default -> {
            }
        }

        if (targetType == String.class) {
            return (T) value.toString();
        }
        throw new IllegalArgumentException("Unsupported type conversion for path '" + path + "': " + value.getClass() + " -> " + targetType);
    }

    private Class<?> wrapType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
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

    private Object convertStringValue(String path, String value, Class<?> targetType) {
        String trimmed = value.trim();

        try {
            if (targetType == String.class) {
                return value;
            }
            if (targetType == Integer.class) {
                return Integer.valueOf(trimmed);
            }
            if (targetType == Long.class) {
                return Long.valueOf(trimmed);
            }
            if (targetType == Double.class) {
                return Double.valueOf(trimmed);
            }
            if (targetType == Float.class) {
                return Float.valueOf(trimmed);
            }
            if (targetType == Short.class) {
                return Short.valueOf(trimmed);
            }
            if (targetType == Byte.class) {
                return Byte.valueOf(trimmed);
            }
            if (targetType == Boolean.class) {
                if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
                    return Boolean.valueOf(trimmed);
                }
                throw new IllegalArgumentException("Invalid boolean value");
            }
            if (targetType == Character.class) {
                if (trimmed.length() == 1) {
                    return trimmed.charAt(0);
                }
                throw new IllegalArgumentException("Expected a single character");
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid value for path '" + path + "': '" + value + "' cannot be converted to " + targetType.getSimpleName(), e);
        }

        throw new IllegalArgumentException("Unsupported type conversion for path '" + path + "': " + String.class + " -> " + targetType);
    }

    @Override
    public Set<String> keys(String path) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String prefix = path == null || path.isEmpty() ? "" : path + ".";

        for (String key : data.keySet()) {
            if (!key.startsWith(prefix)) {
                continue;
            }

            String remaining = key.substring(prefix.length());
            int dot = remaining.indexOf('.');
            result.add(dot == -1 ? remaining : remaining.substring(0, dot));
        }
        return result;
    }

    protected void flatten(String prefix, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty()
                    ? entry.getKey()
                    : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> map) {
                flatten(key, castMap(map));
                continue;
            }
            data.put(key, value);
        }
    }

    protected Object buildTree() {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            Map<String, Object> current = root;

            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                current = castMap(current.computeIfAbsent(part, ignored -> new LinkedHashMap<String, Object>()));
            }
            current.put(parts[parts.length - 1], entry.getValue());
        }
        return root;
    }

    protected <T> ConfigEncoder.Result encodeConfig(java.nio.file.Path file, T config) {
        ConfigEncoder.Result result = ConfigEncoder.encode(file, config, data);
        data.clear();
        data.putAll(result.flatData());
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
