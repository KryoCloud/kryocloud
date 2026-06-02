package eu.kryocloud.launcher.classpath;

import eu.kryocloud.launcher.argument.LauncherMode;
import eu.kryocloud.launcher.dependency.Artifact;
import eu.kryocloud.launcher.repository.RepositoryResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClasspathLoader implements AutoCloseable {

    private static final String VERSION = System.getProperty("kryocloud.version", "1.0.0-alpha.1");

    private final URLClassLoader classLoader;

    public ClasspathLoader(Path projectRoot, Path cacheDirectory, LauncherMode mode) {
        RepositoryResolver resolver = new RepositoryResolver(projectRoot, cacheDirectory);
        List<Path> files = resolver.resolveAll(artifacts(mode));
        URL[] urls = files.stream().map(ClasspathLoader::url).toArray(URL[]::new);
        this.classLoader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());

        verifyRuntimeClasspath();
    }

    private void verifyRuntimeClasspath() {
        require("io.netty.channel.EventLoopGroup");
        require("io.netty.handler.codec.LengthFieldBasedFrameDecoder");
        require("org.jline.reader.LineReader");
    }

    private void require(String className) {
        try {
            Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Launcher runtime classpath is incomplete. Missing " + className + ". Delete .kryocloud/libs and rebuild KryoCloud once.", exception);
        }
    }

    public Object create(String className) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("className must not be blank");
        }

        try {
            Class<?> type = Class.forName(className, true, classLoader);
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create " + className, exception);
        }
    }

    public void shutdown(Object instance) {
        if (instance == null) {
            return;
        }

        try {
            Method method = instance.getClass().getMethod("shutdown");
            method.invoke(instance);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception exception) {
            throw new RuntimeException("Failed to shutdown " + instance.getClass().getName(), exception);
        }
    }

    @Override
    public void close() throws Exception {
        classLoader.close();
    }

    private static URL url(Path path) {
        try {
            return path.toUri().toURL();
        } catch (Exception exception) {
            throw new RuntimeException("Invalid classpath path " + path, exception);
        }
    }

    private static List<Artifact> artifacts(LauncherMode mode) {
        List<Artifact> artifacts = new ArrayList<>();

        artifacts.add(kryo("kryocloud-api"));
        artifacts.add(kryo("kryocloud-plugin-api"));
        artifacts.add(kryo("kryocloud-common"));
        artifacts.add(kryo("kryocloud-network"));

        if (mode == LauncherMode.NODE || mode == LauncherMode.ALL) {
            artifacts.add(kryo("kryocloud-node"));
        }

        if (mode == LauncherMode.WRAPPER || mode == LauncherMode.ALL) {
            artifacts.add(kryo("kryocloud-wrapper"));
        }

        artifacts.add(new Artifact("it.unimi.dsi", "fastutil", "8.5.18"));

        artifacts.add(new Artifact("io.netty", "netty-common", "4.2.10.Final"));
        artifacts.add(new Artifact("io.netty", "netty-buffer", "4.2.10.Final"));
        artifacts.add(new Artifact("io.netty", "netty-resolver", "4.2.10.Final"));
        artifacts.add(new Artifact("io.netty", "netty-transport", "4.2.10.Final"));
        artifacts.add(new Artifact("io.netty", "netty-codec", "4.2.10.Final"));
        artifacts.add(new Artifact("io.netty", "netty-codec-base", "4.2.10.Final"));
        artifacts.add(new Artifact("io.netty", "netty-codec-compression", "4.2.10.Final"));

        artifacts.add(new Artifact("org.yaml", "snakeyaml", "2.6"));
        artifacts.add(new Artifact("org.tomlj", "tomlj", "1.1.1"));
        artifacts.add(new Artifact("org.antlr", "antlr4-runtime", "4.13.2"));
        artifacts.add(new Artifact("org.json", "json", "20251224"));
        artifacts.add(new Artifact("org.jline", "jline-reader", "3.30.5"));
        artifacts.add(new Artifact("org.jline", "jline-terminal", "3.30.5"));
        artifacts.add(new Artifact("org.jline", "jline-terminal-jna", "3.30.5"));
        artifacts.add(new Artifact("net.java.dev.jna", "jna", "5.18.1"));

        return List.copyOf(artifacts);
    }

    private static Artifact kryo(String artifactId) {
        return new Artifact("eu.kryocloud", artifactId, VERSION);
    }

}
