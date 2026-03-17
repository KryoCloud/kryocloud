package eu.kryocloud.common.config.type;

import eu.kryocloud.api.config.IConfig;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JsonTypeProvider extends TypeProvider {

    @Override
    public void load(Path file) throws Exception {
        data.clear();
        if (!Files.exists(file)) {
            return;
        }
        JSONObject json = new JSONObject(new JSONTokener(Files.newInputStream(file)));
        flatten("", json.toMap());
    }

    @Override
    public void save(Path file, IConfig config) throws Exception {
        if (config != null) {
            reflectFields(config);
        }
        Map<String, Object> tree = castMap(buildTree());

        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, formatObject(tree, 0));
    }

    private String formatObject(Map<String, Object> map, int depth) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");

        int index = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            builder.append(indent(depth + 1))
                    .append('"')
                    .append(escape(entry.getKey()))
                    .append("\": ")
                    .append(formatValue(entry.getValue(), depth + 1));

            if (++index < map.size()) {
                builder.append(',');
            }
            builder.append('\n');
        }

        builder.append(indent(depth)).append('}');
        return builder.toString();
    }

    private String formatArray(List<?> list, int depth) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(formatValue(list.get(i), depth));
        }

        builder.append(']');
        return builder.toString();
    }

    private String formatValue(Object value, int depth) {
        if (value instanceof Map<?, ?> map) {
            return formatObject(castMap(map), depth);
        }
        if (value instanceof List<?> list) {
            return formatArray(list, depth + 1);
        }
        if (value instanceof String s) {
            return '"' + escape(s) + '"';
        }
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        if (value == null) {
            return "null";
        }
        return '"' + escape(String.valueOf(value)) + '"';
    }

    private String indent(int depth) {
        return "    ".repeat(depth);
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
