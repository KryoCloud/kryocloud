package eu.kryocloud.common.argument;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Map;

public class ArgumentReader {

    public static Map<String, String> read(String[] args) {
        Map<String, String> values = new Object2ObjectOpenHashMap<>();

        for (String arg : args) {
            if (!arg.startsWith("--")) {
                continue;
            }
            String s = arg.substring(2);

            int i = s.indexOf('=');
            if (i == -1) {
                continue;
            }
            values.put(s.substring(0, i), s.substring(i + 1));
        }

        return values;
    }
}