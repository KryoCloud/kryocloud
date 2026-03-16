package eu.kryocloud.common.config.type;

import eu.kryocloud.api.config.type.IConfigTypeProvider;
import it.unimi.dsi.fastutil.objects.*;

import java.util.Map;
import java.util.Set;

public abstract class AbstractTypeProvider implements IConfigTypeProvider {

    protected final Object2ObjectOpenHashMap<String, Object> data = new Object2ObjectOpenHashMap<>();

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
        Object value = data.get(path);

        if (value == null){
            return null;
        }

        if (type.isInstance(value)) {
            return (T) value;
        }

        switch (value) {
            case Number n when type == Integer.class -> {
                return (T) Integer.valueOf(n.intValue());
            }
            case Number n when type == Long.class -> {
                return (T) Long.valueOf(n.longValue());
            }
            case Number n when type == Double.class -> {
                return (T) Double.valueOf(n.doubleValue());
            }
            case Boolean b when type == Boolean.class -> {
                return (T) b;
            }
            default -> {
            }
        }

        if (type == String.class) {
            return (T) value.toString();
        }
        throw new IllegalArgumentException("Unsupported type conversion: " + type);
    }

    @Override
    public Set<String> keys(String path) {
        ObjectOpenHashSet<String> result = new ObjectOpenHashSet<>();
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
                flatten(key, (Map<String, Object>) map);
                continue;
            }
            data.put(key, value);
        }
    }

    protected Object buildTree() {
        Object2ObjectOpenHashMap<String, Object> root = new Object2ObjectOpenHashMap<>();

        for (Object2ObjectMap.Entry<String, Object> entry : data.object2ObjectEntrySet()) {
            String[] parts = entry.getKey().split("\\.");
            Map<String, Object> current = root;

            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                current = (Map<String, Object>) current.computeIfAbsent(part, k -> new Object2ObjectOpenHashMap<>());
            }
            current.put(parts[parts.length - 1], entry.getValue());
        }
        return root;
    }
}
