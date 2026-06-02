package eu.kryocloud.launcher;

import eu.kryocloud.launcher.argument.LauncherArguments;
import eu.kryocloud.launcher.argument.LauncherMode;
import eu.kryocloud.node.KryoNode;
import eu.kryocloud.wrapper.KryoWrapper;

import java.util.concurrent.atomic.AtomicBoolean;

public final class KryoLauncher {

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
        switch (mode) {
            case NODE -> launchNode();
            case WRAPPER -> launchWrapper();
            case ALL -> launchAll();
        }
    }

    private static void launchNode() {
        KryoNode node = new KryoNode();
        AtomicBoolean closed = new AtomicBoolean(false);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeNode(node, closed), "kryocloud-node-shutdown"));
    }

    private static void launchWrapper() {
        KryoWrapper wrapper = new KryoWrapper();
        AtomicBoolean closed = new AtomicBoolean(false);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeWrapper(wrapper, closed), "kryocloud-wrapper-shutdown"));
    }

    private static void launchAll() {
        KryoNode node = new KryoNode();
        KryoWrapper wrapper = new KryoWrapper();
        AtomicBoolean closed = new AtomicBoolean(false);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeAll(node, wrapper, closed), "kryocloud-local-shutdown"));
    }

    private static void closeAll(KryoNode node, KryoWrapper wrapper, AtomicBoolean closed) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        wrapper.shutdown();
        node.shutdown();
    }

    private static void closeNode(KryoNode node, AtomicBoolean closed) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        node.shutdown();
    }

    private static void closeWrapper(KryoWrapper wrapper, AtomicBoolean closed) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        wrapper.shutdown();
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
        System.out.println("Default mode: node");
    }

}
