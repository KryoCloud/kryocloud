package eu.kryocloud.common.config.type;

import eu.kryocloud.api.config.IConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlTypeProvider extends TypeProvider {

    private final Yaml yaml = new Yaml();

    @Override
    public void load(Path file) throws Exception {
        data.clear();
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            Object loaded = yaml.load(in);
            if (!(loaded instanceof Map<?, ?> map)) {
                return;
            }
            flatten("", convert(map));
        }
    }

    @Override
    public void save(Path file, IConfig config) throws Exception {
        if (config != null) {
            reflectFields(config);
        }
        Map<String, String[]> comments = getFieldComments(file, config);
        Map<String, Object> tree = castMap(buildTree());

        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeMap(writer, tree, comments, "", 0);
        }
    }

    private Map<String, Object> convert(Map<?, ?> input) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                value = convert(nested);
            }
            result.put(String.valueOf(entry.getKey()), value);
        }
        return result;
    }

    private void writeMap(BufferedWriter writer, Map<String, Object> map, Map<String, String[]> comments, String prefix, int depth) throws Exception {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            String[] fieldComments = comments.get(path);

            if (fieldComments != null) {
                for (String comment : fieldComments) {
                    indent(writer, depth);
                    writer.write("# ");
                    writer.write(comment);
                    writer.newLine();
                }
            }

            if (value instanceof Map<?, ?> child) {
                indent(writer, depth);
                writer.write(key);
                writer.write(":");
                writer.newLine();
                writeMap(writer, castMap(child), comments, path, depth + 1);
                continue;
            }

            indent(writer, depth);
            writer.write(key);
            writer.write(": ");
            writer.write(format(value));
            writer.newLine();
        }
    }

    private void indent(BufferedWriter writer, int depth) throws Exception {
        for (int i = 0; i < depth; i++) {
            writer.write("  ");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private String format(Object value) {
        if (value instanceof String s) {
            return '"' + escape(s) + '"';
        }

        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }

        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(format(list.get(i)));
            }
            builder.append(']');
            return builder.toString();
        }

        return '"' + escape(String.valueOf(value)) + '"';
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
