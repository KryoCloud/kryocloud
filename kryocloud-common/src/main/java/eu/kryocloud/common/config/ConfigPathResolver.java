package eu.kryocloud.common.config;

import eu.kryocloud.common.config.type.ConfigType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigPathResolver {

    private static final List<String> ENDINGS = List.of(".toml", ".yaml", ".yml", ".json", ".cfg");

    private ConfigPathResolver() {
    }

    public static Path resolve(Path directory, String name) {
        return resolve(directory, name, ".cfg");
    }

    public static Path resolve(Path directory, String name, String fallbackExtension) {
        if (directory == null) {
            throw new IllegalArgumentException("directory must not be null");
        }

        String cleanName = sanitizeName(name);

        for (String ending : ENDINGS) {
            Path path = directory.resolve(cleanName + ending);

            if (Files.exists(path)) {
                return path;
            }
        }

        return directory.resolve(cleanName + normalizeExtension(fallbackExtension));
    }

    public static Path target(Path directory, String name, ConfigType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        return directory.resolve(sanitizeName(name) + type.getEnding());
    }

    public static String normalizeExtension(String value) {
        if (value == null || value.isBlank()) {
            return ".cfg";
        }

        String trimmed = value.trim().toLowerCase();
        return trimmed.startsWith(".") ? trimmed : "." + trimmed;
    }

    private static String sanitizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        return value.trim().replaceAll("[^A-Za-z0-9_.-]", "-");
    }
}
