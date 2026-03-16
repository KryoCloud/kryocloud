package eu.kryocloud.common.config.type;

import it.unimi.dsi.fastutil.objects.*;
import org.tomlj.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TomlTypeProvider extends AbstractTypeProvider {

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
    public void save(Path file) throws Exception {
        Map<String, Object> tree = (Map<String, Object>) buildTree();
        StringBuilder builder = new StringBuilder();
        writeSection(builder, "", tree);
        Files.writeString(file, builder.toString());
    }

    private void writeSection(StringBuilder builder, String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> child) {
                String section = prefix.isEmpty()
                        ? key
                        : prefix + "." + key;
                builder.append("\n[").append(section).append("]\n");
                writeSection(builder, section, (Map<String, Object>) child);
                continue;
            }
            builder.append(key)
                    .append(" = ")
                    .append(format(value))
                    .append("\n");
        }
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
