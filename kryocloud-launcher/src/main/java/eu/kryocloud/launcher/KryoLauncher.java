package eu.kryocloud.launcher;

import eu.kryocloud.launcher.argument.LauncherArguments;
import eu.kryocloud.launcher.argument.LauncherMode;
import eu.kryocloud.launcher.classpath.ClasspathLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class KryoLauncher {

    private static final String NODE_CLASS = "eu.kryocloud.node.KryoNode";
    private static final String WRAPPER_CLASS = "eu.kryocloud.wrapper.KryoWrapper";

    private KryoLauncher() {
    }

    public static void main(String[] args) {
        try {
            LauncherArguments arguments = LauncherArguments.parse(args);

            if (arguments.help()) {
                printHelp();
                return;
            }

            launch(arguments.mode());
        } catch (Exception exception) {
            System.err.println("Failed to start KryoCloud Launcher: " + exception.getMessage());
            exception.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void launch(LauncherMode mode) {
        Path projectRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path cache = projectRoot.resolve(".kryocloud").resolve("libs");
        ClasspathLoader loader = new ClasspathLoader(projectRoot, cache, mode);
        List<Object> instances = new ArrayList<>();
        AtomicBoolean closed = new AtomicBoolean(false);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> close(loader, instances, closed), "kryocloud-launcher-shutdown"));

        switch (mode) {
            case NODE -> instances.add(loader.create(NODE_CLASS));
            case WRAPPER -> instances.add(loader.create(WRAPPER_CLASS));
            case ALL -> {
                instances.add(loader.create(NODE_CLASS));
                instances.add(loader.create(WRAPPER_CLASS));
            }
        }
    }

    private static void close(ClasspathLoader loader, List<Object> instances, AtomicBoolean closed) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        for (int index = instances.size() - 1; index >= 0; index--) {
            loader.shutdown(instances.get(index));
        }

        try {
            loader.close();
        } catch (Exception exception) {
            System.err.println("Failed to close launcher classpath: " + exception.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("KryoCloud Launcher");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar kryocloud-launcher.jar");
        System.out.println("  java -jar kryocloud-launcher.jar node");
        System.out.println("  java -jar kryocloud-launcher.jar wrapper");
        System.out.println("  java -jar kryocloud-launcher.jar all");
        System.out.println();
        System.out.println("Default mode: all");
        System.out.println("Dependency cache: .kryocloud/libs");
    }

}
