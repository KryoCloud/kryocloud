package eu.kryocloud.common.config.codec;

import eu.kryocloud.api.config.type.IConfigTypeProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class ConfigDecoder {

    private ConfigDecoder() {
    }

    public static <T> void decode(T config, IConfigTypeProvider provider) {
        for (Field field : getConfigFields(config.getClass())) {
            try {
                field.setAccessible(true);
                Class<?> fieldType = getWrapperType(field.getType());
                Object value = provider.get(field.getName(), fieldType);
                if (value != null) {
                    field.set(config, value);
                }
            } catch (IllegalAccessException _) {
            }
        }
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

    private static Class<?> getWrapperType(Class<?> type) {
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }
}

