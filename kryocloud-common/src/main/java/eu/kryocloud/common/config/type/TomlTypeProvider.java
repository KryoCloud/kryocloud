package eu.kryocloud.common.config.type;

import org.tomlj.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TomlTypeProvider extends TypeProvider {

    @Override
    public void load(Path file) throws Exception {
        data.clear();
        if (!Files.exists(file)) {
            return;
        }
        TomlParseResult result = Toml.parse(file);
        if (result.hasErrors()) {
            throw new IllegalStateException(result.errors().toString());
        }
        flatten("", result.toMap());
    }

    @Override
    public <T> void save(Path file, T config) throws Exception {
        Map<String, String[]> comments = java.util.Map.of();
        Map<String, Object> tree = castMap(buildTree());

        if (config != null) {
            var encoded = encodeConfig(file, config);
            comments = encoded.comments();
            tree = encoded.treeData();
        }
        StringBuilder builder = new StringBuilder();

        writeSection(builder, "", tree, comments, true);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, builder.toString());
    }

    private void writeSection(StringBuilder builder, String prefix, Map<String, Object> map, Map<String, String[]> comments, boolean rootSection) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> child) {
                String section = prefix.isEmpty()
                        ? key
                        : prefix + "." + key;

                if (!rootSection || !builder.isEmpty()) {
                    builder.append("\n");
                }
                builder.append("[").append(section).append("]\n");
                writeSection(builder, section, castMap(child), comments, false);
                continue;
            }

            String[] fieldComments = comments.get(prefix.isEmpty() ? key : prefix + "." + key);
            if (fieldComments != null) {
                for (String comment : fieldComments) {
                    builder.append("# ").append(comment).append("\n");
                }
            }
            builder.append(key)
                    .append(" = ")
                    .append(format(value))
                    .append("\n");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private String format(Object value) {
        if (value instanceof String s) {
            return "\"" + s + "\"";
        }

        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }

        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) builder.append(", ");
                builder.append(format(list.get(i)));
            }
            builder.append("]");
            return builder.toString();
        }
        return "\"" + value + "\"";
    }
}
