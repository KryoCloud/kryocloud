package eu.kryocloud.common.config.type;

import eu.kryocloud.api.config.type.IConfigTypeProvider;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class YamlTypeProvider implements IConfigTypeProvider {

    private final Yaml yaml = new Yaml();
    private Map<String, Object> data = new Object2ObjectOpenHashMap<>();

    @Override
    public void load(Path file) throws Exception {
        if (!Files.exists(file)) {
            data = new Object2ObjectOpenHashMap<>();
            return;
        }

        try (InputStream in = Files.newInputStream(file)) {
            Object loaded = yaml.load(in);
            if (!(loaded instanceof Map<?, ?> map)) {
                data = new Object2ObjectOpenHashMap<>();
                return;
            }

            data = convertMap(map);
        }
    }

    @Override
    public void save(Path file) throws Exception {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (var out = Files.newOutputStream(file)) {
            yaml.dump(data, new OutputStreamWriter(out));
        }
    }

    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);

            if (!(next instanceof Map)) {
                Map<String, Object> newMap = new Object2ObjectOpenHashMap<>();
                current.put(parts[i], newMap);
                current = newMap;

                continue;
            }
            current = (Map<String, Object>) next;
        }
        current.put(parts[parts.length - 1], value);
    }

    @Override
    public void remove(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);

            if (!(next instanceof Map)) {
                return;
            }
            current = (Map<String, Object>) next;
        }
        current.remove(parts[parts.length - 1]);
    }

    @Override
    public boolean contains(String path) {
        return getNode(path) != null;
    }

    @Override
    public <T> T get(String path, Class<T> type) {
        Object value = getNode(path);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        if (value instanceof Number n) {

            if (type == Integer.class) {
                return type.cast(n.intValue());
            }

            if (type == Long.class) {
                return type.cast(n.longValue());
            }

            if (type == Double.class) {
                return type.cast(n.doubleValue());
            }
        }
        if (type == String.class) {
            return type.cast(String.valueOf(value));
        }
        return null;
    }

    @Override
    public Set<String> keys(String path) {
        Object node = path == null || path.isEmpty() ? data : getNode(path);
        if (!(node instanceof Map<?, ?> map)) {
            return Set.of();
        }
        return (Set<String>) map.keySet();
    }

    private Object getNode(String path) {
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }

            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Map<String, Object> convertMap(Map<?, ?> input) {
        Map<String, Object> result = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<?, ?> entry : input.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                value = convertMap(nested);
            }

            result.put(String.valueOf(entry.getKey()), value);
        }
        return result;
    }
}