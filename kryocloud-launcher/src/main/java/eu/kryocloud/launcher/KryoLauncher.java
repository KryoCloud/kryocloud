package eu.kryocloud.launcher;

import eu.kryocloud.launcher.argument.LauncherArguments;
import eu.kryocloud.launcher.argument.LauncherMode;
import eu.kryocloud.launcher.classpath.ClasspathLoader;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

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

    private static void launch(LauncherMode mode) throws Exception {
        ClasspathLoader loader = ClasspathLoader.create();
        Thread.currentThread().setContextClassLoader(loader.classLoader());

        switch (mode) {
            case NODE -> launchNode(loader);
            case WRAPPER -> launchWrapper(loader);
            case ALL -> launchAll(loader);
        }
    }

    private static void launchNode(ClasspathLoader loader) throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<Object> node = new AtomicReference<>();

        try (InterruptShutdownGuard ignored = InterruptShutdownGuard.install(() -> close(loader, node.get(), closed))) {
            Object instance = loader.create("eu.kryocloud.node.KryoNode");
            node.set(instance);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> close(loader, node.get(), closed), "kryocloud-node-shutdown"));
            waitWhileRunning(loader, instance);
        } finally {
            close(loader, node.get(), closed);
        }
    }

    private static void launchWrapper(ClasspathLoader loader) throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<Object> wrapper = new AtomicReference<>();

        try (InterruptShutdownGuard ignored = InterruptShutdownGuard.install(() -> close(loader, wrapper.get(), closed))) {
            Object instance = loader.create("eu.kryocloud.wrapper.KryoWrapper");
            wrapper.set(instance);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> close(loader, wrapper.get(), closed), "kryocloud-wrapper-shutdown"));
            waitWhileRunning(loader, instance);
        } finally {
            close(loader, wrapper.get(), closed);
        }
    }

    private static void launchAll(ClasspathLoader loader) throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<Object> node = new AtomicReference<>();
        AtomicReference<Object> wrapper = new AtomicReference<>();

        try (InterruptShutdownGuard ignored = InterruptShutdownGuard.install(() -> closeAll(loader, wrapper.get(), node.get(), closed))) {
            Object nodeInstance = loader.create("eu.kryocloud.node.KryoNode");
            node.set(nodeInstance);

            Object wrapperInstance = loader.create("eu.kryocloud.wrapper.KryoWrapper");
            wrapper.set(wrapperInstance);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> closeAll(loader, wrapper.get(), node.get(), closed), "kryocloud-local-shutdown"));
            waitWhileAnyRunning(loader, nodeInstance, wrapperInstance);
        } finally {
            closeAll(loader, wrapper.get(), node.get(), closed);
        }
    }

    private static void waitWhileRunning(ClasspathLoader loader, Object instance) throws Exception {
        while (isRunning(loader, instance)) {
            LockSupport.parkNanos(Duration.ofMillis(250).toNanos());
        }
    }

    private static void waitWhileAnyRunning(ClasspathLoader loader, Object first, Object second) throws Exception {
        while (isRunning(loader, first) || isRunning(loader, second)) {
            LockSupport.parkNanos(Duration.ofMillis(250).toNanos());
        }
    }

    private static boolean isRunning(ClasspathLoader loader, Object instance) throws Exception {
        if (instance == null) {
            return false;
        }

        Object value = loader.invokeResult(instance, "running");

        if (value instanceof Boolean active) {
            return active;
        }

        return false;
    }

    private static void close(ClasspathLoader loader, Object instance, AtomicBoolean closed) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        closeQuietly(loader, instance);
    }

    private static void closeAll(ClasspathLoader loader, Object wrapper, Object node, AtomicBoolean closed) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        closeQuietly(loader, node);
        closeQuietly(loader, wrapper);
    }

    private static void closeQuietly(ClasspathLoader loader, Object instance) {
        try {
            loader.invoke(instance, "shutdown");
        } catch (Exception ignored) {
        }
    }

    private static final class InterruptShutdownGuard implements AutoCloseable {

        private static final long CONFIRM_WINDOW_MILLIS = 5_000L;

        private final AtomicLong lastInterruptAtMillis = new AtomicLong(0L);
        private final Runnable shutdown;
        private final sun.misc.Signal signal;
        private final sun.misc.SignalHandler previous;
        private final boolean installed;

        private InterruptShutdownGuard(Runnable shutdown, sun.misc.Signal signal, sun.misc.SignalHandler previous, boolean installed) {
            this.shutdown = shutdown;
            this.signal = signal;
            this.previous = previous;
            this.installed = installed;
        }

        static InterruptShutdownGuard install(Runnable shutdown) {
            try {
                sun.misc.Signal signal = new sun.misc.Signal("INT");
                AtomicReference<InterruptShutdownGuard> reference = new AtomicReference<>();
                sun.misc.SignalHandler previous = sun.misc.Signal.handle(signal, ignored -> {
                    InterruptShutdownGuard guard = reference.get();

                    if (guard == null) {
                        return;
                    }

                    guard.handleInterrupt();
                });
                InterruptShutdownGuard guard = new InterruptShutdownGuard(shutdown, signal, previous, true);
                reference.set(guard);
                return guard;
            } catch (Throwable ignored) {
                return new InterruptShutdownGuard(shutdown, null, null, false);
            }
        }

        private void handleInterrupt() {
            long now = System.currentTimeMillis();
            long previousAt = lastInterruptAtMillis.getAndSet(now);

            if (previousAt < 1 || now - previousAt > CONFIRM_WINDOW_MILLIS) {
                System.out.println();
                System.out.println("❄ Press CTRL+C again within 5 seconds to shutdown KryoCloud.");
                return;
            }

            System.out.println();
            System.out.println("❄ Shutting down KryoCloud...");
            shutdown.run();
        }

        @Override
        public void close() {
            if (!installed || signal == null || previous == null) {
                return;
            }

            try {
                sun.misc.Signal.handle(signal, previous);
            } catch (Throwable ignored) {
            }
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
    }
}
