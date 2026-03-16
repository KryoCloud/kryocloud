package eu.kryocloud.common.config.type;

import it.unimi.dsi.fastutil.objects.*;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class JsonTypeProvider extends AbstractTypeProvider {

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
    public void save(Path file) throws Exception {
        Map<String, Object> tree = (Map<String, Object>) buildTree();
        JSONObject json = new JSONObject(tree);
        Files.writeString(file, json.toString(4));
    }
}
