package eu.kryocloud.api.plugin.internal.platform;

import eu.kryocloud.api.plugin.description.PluginDescription;
import eu.kryocloud.api.plugin.logging.IPluginLogger;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlatformAccess {

    private PlatformAccess() {
    }

    public static Path dataDirectory(Object platformPlugin, PluginDescription description) {
        Object value = invoke(platformPlugin, "getDataFolder");

        if (value instanceof File file) {
            return file.toPath();
        }

        value = invoke(platformPlugin, "getDataDirectory");

        if (value instanceof Path path) {
            return path;
        }

        return Path.of("plugins", "KryoCloud", description.id());
    }

    public static IPluginLogger logger(Object platformPlugin, PluginDescription description) {
        Object value = invoke(platformPlugin, "getLogger");

        if (value instanceof Logger logger) {
            return new JavaPluginLogger(description, logger);
        }

        return new SystemPluginLogger(description);
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record JavaPluginLogger(PluginDescription description, Logger logger) implements IPluginLogger {

        @Override
        public void info(String message) {
            logger.info(prefix(message));
        }

        @Override
        public void success(String message) {
            logger.info(prefix(message));
        }

        @Override
        public void warn(String message) {
            logger.warning(prefix(message));
        }

        @Override
        public void error(String message) {
            logger.severe(prefix(message));
        }

        @Override
        public void error(String message, Throwable throwable) {
            logger.log(Level.SEVERE, prefix(message), throwable);
        }

        private String prefix(String message) {
            return "[KryoCloud/" + description.id() + "] " + safe(message);
        }

    }

    private record SystemPluginLogger(PluginDescription description) implements IPluginLogger {

        @Override
        public void info(String message) {
            System.out.println(prefix(message));
        }

        @Override
        public void success(String message) {
            System.out.println(prefix(message));
        }

        @Override
        public void warn(String message) {
            System.out.println(prefix("WARN: " + safe(message)));
        }

        @Override
        public void error(String message) {
            System.err.println(prefix(message));
        }

        @Override
        public void error(String message, Throwable throwable) {
            System.err.println(prefix(message));

            if (throwable == null) {
                return;
            }

            throwable.printStackTrace(System.err);
        }

        private String prefix(String message) {
            return "[KryoCloud/" + description.id() + "] " + safe(message);
        }

    }

    private static String safe(String message) {
        return message == null ? "" : message;
    }

}
