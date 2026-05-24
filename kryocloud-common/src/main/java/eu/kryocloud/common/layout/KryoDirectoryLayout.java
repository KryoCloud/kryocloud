package eu.kryocloud.common.layout;

import java.nio.file.Files;
import java.nio.file.Path;

public final class KryoDirectoryLayout {

    private static final String HOME_PROPERTY = "kryocloud.home";
    private static final String HOME_ENVIRONMENT = "KRYOCLOUD_HOME";
    private static final String HOME_POINTER_NAME = ".kryocloud-home";

    private static volatile Path homePointer = defaultHomePointer();
    private static volatile String homeSource = "default";

    public static volatile Path ROOT;
    public static volatile Path TMP;
    public static volatile Path STATIC;
    public static volatile Path CONFIG;
    public static volatile Path ADDONS;
    public static volatile Path JDK;
    public static volatile Path TEMPLATES;
    public static volatile Path STORAGE;
    public static volatile Path VERSIONS;
    public static volatile Path GROUPS;

    static {
        bootstrap();
    }

    private KryoDirectoryLayout() {
    }

    public static synchronized void bootstrap() {
        String property = System.getProperty(HOME_PROPERTY);

        if (property != null && !property.isBlank()) {
            homeSource = "system-property";
            homePointer = defaultHomePointer();
            use(Path.of(property));
            return;
        }

        String environment = System.getenv(HOME_ENVIRONMENT);

        if (environment != null && !environment.isBlank()) {
            homeSource = "environment";
            homePointer = defaultHomePointer();
            use(Path.of(environment));
            return;
        }

        Path pointer = findHomePointer();

        if (pointer != null) {
            homeSource = "pointer";
            homePointer = pointer;
            use(readPointer(pointer));
            return;
        }

        homeSource = "unconfigured";
        homePointer = defaultHomePointer();
        use(Path.of("."));
    }

    public static synchronized void use(Path root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }

        Path normalizedRoot = root.toAbsolutePath().normalize();

        ROOT = normalizedRoot;
        TMP = normalizedRoot.resolve("tmp");
        STATIC = normalizedRoot.resolve("static");
        CONFIG = normalizedRoot.resolve("config");
        ADDONS = normalizedRoot.resolve("addons");
        JDK = normalizedRoot.resolve(".jdk");
        TEMPLATES = normalizedRoot.resolve("templates");
        STORAGE = normalizedRoot.resolve("storage");
        VERSIONS = STORAGE.resolve("versions");
        GROUPS = CONFIG.resolve("groups");
    }

    public static synchronized void persistHomePointer(Path root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }

        Path pointer = homePointer == null ? defaultHomePointer() : homePointer;

        try {
            Files.createDirectories(pointer.toAbsolutePath().normalize().getParent());
            Files.writeString(pointer, root.toAbsolutePath().normalize().toString());
            homePointer = pointer.toAbsolutePath().normalize();
            homeSource = "pointer";
        } catch (Exception exception) {
            throw new RuntimeException("Failed to write KryoCloud home pointer " + pointer.toAbsolutePath().normalize(), exception);
        }
    }

    public static synchronized void resetHomePointer() {
        Path pointer = homePointer == null ? defaultHomePointer() : homePointer;

        try {
            Files.deleteIfExists(pointer);
            homeSource = "unconfigured";
        } catch (Exception exception) {
            throw new RuntimeException("Failed to delete KryoCloud home pointer " + pointer.toAbsolutePath().normalize(), exception);
        }
    }

    public static boolean hasPersistedHomePointer() {
        return findHomePointer() != null;
    }

    public static boolean hasExternalHome() {
        String property = System.getProperty(HOME_PROPERTY);
        String environment = System.getenv(HOME_ENVIRONMENT);
        return property != null && !property.isBlank() || environment != null && !environment.isBlank();
    }

    public static Path homePointer() {
        return homePointer == null ? defaultHomePointer() : homePointer;
    }

    public static String homeSource() {
        return homeSource;
    }

    public static Path launchDirectory() {
        return defaultPointerDirectory();
    }

    public static Path suggestedHomeDirectory() {
        return launchDirectory().resolve("kryocloud-runtime").toAbsolutePath().normalize();
    }

    public static String homeSummary() {
        return "home=" + ROOT.toAbsolutePath().normalize() + ", source=" + homeSource + ", pointer=" + homePointer().toAbsolutePath().normalize();
    }

    public static void ensureNodeDirectories() {
        create(ROOT);
        create(CONFIG);
        create(GROUPS);
        create(ADDONS);
        create(JDK);
        create(TEMPLATES);
        create(STORAGE);
        create(VERSIONS);
    }

    public static void ensureWrapperDirectories() {
        create(ROOT);
        create(CONFIG);
        create(ADDONS);
        create(JDK);
        create(TEMPLATES);
        create(TMP);
        create(STATIC);
    }

    private static Path findHomePointer() {
        Path current = Path.of("").toAbsolutePath().normalize();

        while (current != null) {
            Path pointer = current.resolve(HOME_POINTER_NAME);

            if (Files.exists(pointer)) {
                return pointer.toAbsolutePath().normalize();
            }

            current = current.getParent();
        }

        return null;
    }

    private static Path readPointer(Path pointer) {
        try {
            String value = Files.readString(pointer).trim();

            if (value.isBlank()) {
                return Path.of(".");
            }

            return Path.of(value);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read KryoCloud home pointer " + pointer.toAbsolutePath().normalize(), exception);
        }
    }

    private static Path defaultHomePointer() {
        return defaultPointerDirectory().resolve(HOME_POINTER_NAME).toAbsolutePath().normalize();
    }

    private static Path defaultPointerDirectory() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path projectRoot = projectRoot(current);

        if (projectRoot != null) {
            return projectRoot;
        }

        return current;
    }

    private static Path projectRoot(Path start) {
        Path current = start;

        while (current != null) {
            if (Files.exists(current.resolve(".git"))) {
                return current;
            }

            if (aggregatorPom(current.resolve("pom.xml"))) {
                return current;
            }

            current = current.getParent();
        }

        return null;
    }

    private static boolean aggregatorPom(Path pom) {
        if (!Files.exists(pom)) {
            return false;
        }

        try {
            String content = Files.readString(pom);
            return content.contains("<modules>") && content.contains("</modules>");
        } catch (Exception exception) {
            return false;
        }
    }

    private static void create(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create directory " + path, exception);
        }
    }
}
