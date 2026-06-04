package eu.kryocloud.launcher.classpath;

import eu.kryocloud.launcher.download.Downloader;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ClasspathLoader implements AutoCloseable {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
    private static final String PAPER_REPOSITORY = "https://repo.papermc.io/repository/maven-public";
    private static final String VERSION = "1.0.0-alpha.1";

    private static final List<Artifact> PROJECT_ARTIFACTS = List.of(
            artifact("eu.kryocloud", "kryocloud-api", VERSION),
            artifact("eu.kryocloud", "kryocloud-plugin-api", VERSION),
            artifact("eu.kryocloud", "kryocloud-common", VERSION),
            artifact("eu.kryocloud", "kryocloud-sphere", VERSION),
            artifact("eu.kryocloud", "kryocloud-network", VERSION),
            artifact("eu.kryocloud", "kryocloud-node", VERSION),
            artifact("eu.kryocloud", "kryocloud-wrapper", VERSION)
    );

    private static final List<Artifact> REMOTE_ARTIFACTS = List.of(
            artifact("it.unimi.dsi", "fastutil", "8.5.18"),
            artifact("org.yaml", "snakeyaml", "2.6"),
            artifact("org.tomlj", "tomlj", "1.1.1"),
            artifact("org.antlr", "antlr4-runtime", "4.11.1"),
            artifact("org.json", "json", "20251224"),
            artifact("org.jline", "jline-reader", "3.30.5"),
            artifact("org.jline", "jline-terminal", "3.30.5"),
            artifact("org.jline", "jline-terminal-jna", "3.30.5"),
            artifact("org.jline", "jline-terminal-jansi", "3.30.5"),
            artifact("net.java.dev.jna", "jna", "5.18.1"),
            artifact("org.fusesource.jansi", "jansi", "2.4.2"),
            artifact("org.slf4j", "slf4j-api", "2.0.17"),
            artifact("io.netty", "netty-common", "4.2.10.Final"),
            artifact("io.netty", "netty-buffer", "4.2.10.Final"),
            artifact("io.netty", "netty-resolver", "4.2.10.Final"),
            artifact("io.netty", "netty-transport", "4.2.10.Final"),
            artifact("io.netty", "netty-transport-native-unix-common", "4.2.10.Final"),
            artifact("io.netty", "netty-codec", "4.2.10.Final"),
            artifact("io.netty", "netty-codec-base", "4.2.10.Final"),
            artifact("io.netty", "netty-codec-compression", "4.2.10.Final"),
            artifact("io.netty", "netty-handler", "4.2.10.Final")
    );

    private final URLClassLoader classLoader;

    private ClasspathLoader(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static ClasspathLoader create() throws Exception {
        Path home = resolveHome();
        Path libs = home.resolve(".kryocloud").resolve("libs").toAbsolutePath().normalize();
        Path projectRoot = findProjectRoot();
        Downloader downloader = new Downloader();
        Set<Path> classpath = new LinkedHashSet<>();

        classpath.add(launcherLocation());

        for (Artifact artifact : PROJECT_ARTIFACTS) {
            resolveLocalProjectArtifact(projectRoot, artifact).ifPresent(classpath::add);
        }

        for (Artifact artifact : REMOTE_ARTIFACTS) {
            classpath.add(resolveRemoteArtifact(artifact, libs, downloader));
        }

        validateClasspath(classpath);

        List<URL> urls = new ArrayList<>();

        for (Path path : classpath) {
            urls.add(path.toUri().toURL());
        }

        URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
        validateRuntimeClasses(loader);
        return new ClasspathLoader(loader);
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public Object create(String className) throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            Class<?> type = Class.forName(className, true, classLoader);
            return type.getConstructor().newInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    public void invoke(Object instance, String methodName) throws Exception {
        invokeResult(instance, methodName);
    }

    public Object invokeResult(Object instance, String methodName) throws Exception {
        if (instance == null) {
            return null;
        }

        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            Method method = instance.getClass().getMethod(methodName);
            return method.invoke(instance);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    @Override
    public void close() {
    }

    private static java.util.Optional<Path> resolveLocalProjectArtifact(Path projectRoot, Artifact artifact) {
        if (projectRoot == null) {
            return java.util.Optional.empty();
        }

        Path localTarget = projectRoot.resolve(artifact.artifactId()).resolve("target").resolve(artifact.artifactId() + "-" + artifact.version() + ".jar");

        if (Files.exists(localTarget)) {
            return java.util.Optional.of(localTarget.toAbsolutePath().normalize());
        }

        Path localFinal = projectRoot.resolve(artifact.artifactId()).resolve("target").resolve(artifact.artifactId() + ".jar");

        if (Files.exists(localFinal)) {
            return java.util.Optional.of(localFinal.toAbsolutePath().normalize());
        }

        return java.util.Optional.empty();
    }

    private static Path resolveRemoteArtifact(Artifact artifact, Path libs, Downloader downloader) throws Exception {
        Path cached = libs.resolve(artifact.fileName());

        if (Files.exists(cached) && Files.size(cached) > 0) {
            return cached.toAbsolutePath().normalize();
        }

        Path m2 = m2Artifact(artifact);

        if (Files.exists(m2)) {
            if (cached.getParent() != null) {
                Files.createDirectories(cached.getParent());
            }

            Files.copy(m2, cached, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return cached.toAbsolutePath().normalize();
        }

        String repository = artifact.groupId().startsWith("com.velocitypowered") ? PAPER_REPOSITORY : MAVEN_CENTRAL;
        return downloader.download(repository + "/" + artifact.path(), cached).toAbsolutePath().normalize();
    }

    private static Path m2Artifact(Artifact artifact) {
        return Path.of(System.getProperty("user.home"), ".m2", "repository")
                .resolve(artifact.groupId().replace('.', '/'))
                .resolve(artifact.artifactId())
                .resolve(artifact.version())
                .resolve(artifact.fileName());
    }

    private static void validateClasspath(Set<Path> classpath) throws Exception {
        boolean nettyCodecBase = classpath.stream().anyMatch(path -> path.getFileName().toString().startsWith("netty-codec-base-"));
        boolean nettyTransport = classpath.stream().anyMatch(path -> path.getFileName().toString().startsWith("netty-transport-"));
        boolean jlineReader = classpath.stream().anyMatch(path -> path.getFileName().toString().startsWith("jline-reader-"));
        boolean jlineTerminal = classpath.stream().anyMatch(path -> {
            String name = path.getFileName().toString();
            return name.startsWith("jline-terminal-") && !name.startsWith("jline-terminal-jna-") && !name.startsWith("jline-terminal-jansi-");
        });
        boolean jlineJna = classpath.stream().anyMatch(path -> path.getFileName().toString().startsWith("jline-terminal-jna-"));

        if (!nettyCodecBase || !nettyTransport || !jlineReader || !jlineTerminal || !jlineJna) {
            throw new IllegalStateException("Runtime classpath is incomplete. Delete .kryocloud/libs and rebuild KryoCloud.");
        }
    }

    private static void validateRuntimeClasses(ClassLoader loader) throws Exception {
        Class.forName("io.netty.handler.codec.LengthFieldBasedFrameDecoder", false, loader);
        Class.forName("io.netty.util.internal.logging.MessageFormatter", false, loader);
        Class.forName("io.netty.util.concurrent.DefaultPromise$1", false, loader);
        Class.forName("io.netty.buffer.FreeChunkEvent", false, loader);
        Class.forName("io.netty.channel.socket.ChannelInputShutdownReadComplete", false, loader);
        Class.forName("org.jline.reader.LineReader", false, loader);
        Class.forName("org.jline.terminal.TerminalBuilder", false, loader);
        Class.forName("com.sun.jna.Library", false, loader);
        Class.forName("org.fusesource.jansi.AnsiConsole", false, loader);

        Class<?> nodeType = Class.forName("eu.kryocloud.node.KryoNode", false, loader);
        Class<?> wrapperType = Class.forName("eu.kryocloud.wrapper.KryoWrapper", false, loader);

        nodeType.getMethod("running");
        wrapperType.getMethod("running");
    }

    private static Path launcherLocation() throws Exception {
        CodeSource source = ClasspathLoader.class.getProtectionDomain().getCodeSource();

        if (source == null || source.getLocation() == null) {
            throw new IllegalStateException("Unable to resolve KryoCloud launcher location.");
        }

        return Path.of(source.getLocation().toURI()).toAbsolutePath().normalize();
    }

    private static Path resolveHome() {
        String property = System.getProperty("kryocloud.home");

        if (property != null && !property.isBlank()) {
            return Path.of(property).toAbsolutePath().normalize();
        }

        String environment = System.getenv("KRYOCLOUD_HOME");

        if (environment != null && !environment.isBlank()) {
            return Path.of(environment).toAbsolutePath().normalize();
        }

        Path pointer = findHomePointer(Path.of("").toAbsolutePath().normalize());

        if (pointer != null) {
            try {
                String value = Files.readString(pointer).trim();

                if (!value.isBlank()) {
                    return Path.of(value).toAbsolutePath().normalize();
                }
            } catch (Exception ignored) {
            }
        }

        return Path.of("").toAbsolutePath().normalize();
    }

    private static Path findProjectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();

        while (current != null) {
            if (Files.exists(current.resolve("kryocloud-node")) && Files.exists(current.resolve("kryocloud-wrapper"))) {
                return current;
            }

            current = current.getParent();
        }

        return null;
    }

    private static Path findHomePointer(Path start) {
        Path current = start;

        while (current != null) {
            Path pointer = current.resolve(".kryocloud-home");

            if (Files.exists(pointer)) {
                return pointer;
            }

            current = current.getParent();
        }

        return null;
    }

    private static Artifact artifact(String groupId, String artifactId, String version) {
        return new Artifact(groupId, artifactId, version);
    }

    private record Artifact(String groupId, String artifactId, String version) {

        String fileName() {
            return artifactId + "-" + version + ".jar";
        }

        String path() {
            return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + fileName();
        }
    }
}
