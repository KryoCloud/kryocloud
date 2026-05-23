package eu.kryocloud.common.layout;

import java.nio.file.Files;
import java.nio.file.Path;

public final class KryoDirectoryLayout {

    public static final Path ROOT = Path.of(".");
    public static final Path TMP = Path.of("tmp");
    public static final Path STATIC = Path.of("static");
    public static final Path CONFIG = Path.of("config");
    public static final Path ADDONS = Path.of("addons");
    public static final Path TEMPLATES = Path.of("templates");
    public static final Path STORAGE = Path.of("storage");
    public static final Path VERSIONS = STORAGE.resolve("versions");
    public static final Path GROUPS = CONFIG.resolve("groups");

    private KryoDirectoryLayout() {
    }

    public static void ensureNodeDirectories() {
        create(CONFIG);
        create(GROUPS);
        create(ADDONS);
        create(TEMPLATES);
        create(STORAGE);
        create(VERSIONS);
    }

    public static void ensureWrapperDirectories() {
        create(CONFIG);
        create(ADDONS);
        create(TEMPLATES);
        create(TMP);
        create(STATIC);
    }

    private static void create(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create directory " + path, exception);
        }
    }
}
