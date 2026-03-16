package eu.kryocloud.common.config.type;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class YamlTypeProvider extends AbstractTypeProvider {

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
    public void save(Path file) throws Exception {
        Map<String, Object> tree = (Map<String, Object>) buildTree();
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (var out = Files.newOutputStream(file)) {
            yaml.dump(tree, new OutputStreamWriter(out, StandardCharsets.UTF_8));
        }
    }

    private Map<String, Object> convert(Map<?, ?> input) {
        Map<String, Object> result = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                value = convert(nested);
            }
            result.put(String.valueOf(entry.getKey()), value);
        }
        return result;
    }
}
