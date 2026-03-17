package eu.kryocloud.common.config.codec;

import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.type.ConfigType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigEncoder {

    private ConfigEncoder() {
    }

    public static <T> Result encode(Path file, T config, Map<String, Object> currentData) {
        LinkedHashMap<String, Object> orderedFieldValues = new LinkedHashMap<>();
        LinkedHashMap<String, Object> remainingValues = new LinkedHashMap<>(currentData);
        LinkedHashMap<String, String[]> comments = new LinkedHashMap<>();
        boolean supportsComments = ConfigType.fromFileName(file.getFileName().toString()).supportsComments();

        for (Field field : getConfigFields(config.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(config);
                if (value != null) {
                    orderedFieldValues.put(field.getName(), value);
                    remainingValues.remove(field.getName());
                }

                if (supportsComments) {
                    Comment comment = field.getAnnotation(Comment.class);
                    if (comment != null && comment.value().length > 0) {
                        comments.put(field.getName(), comment.value());
                    }
                }
            } catch (IllegalAccessException _) {
            }
        }

        LinkedHashMap<String, Object> flatData = new LinkedHashMap<>();
        flatData.putAll(orderedFieldValues);
        flatData.putAll(remainingValues);

        return new Result(flatData, buildTree(flatData), comments);
    }

    private static LinkedHashMap<String, Object> buildTree(Map<String, Object> flatData) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : flatData.entrySet()) {
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

    private static Field[] getConfigFields(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        java.util.LinkedHashSet<Field> fields = new java.util.LinkedHashSet<>();

        for (Field field : declaredFields) {
            if (isConfigField(field)) {
                fields.add(field);
            }
        }
        return fields.toArray(Field[]::new);
    }

    private static boolean isConfigField(Field field) {
        int modifiers = field.getModifiers();
        return !field.isSynthetic() && !Modifier.isStatic(modifiers);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    public record Result(LinkedHashMap<String, Object> flatData,
                         LinkedHashMap<String, Object> treeData,
                         LinkedHashMap<String, String[]> comments) {
    }
}

