package eu.kryocloud.api.plugin.internal.scheduler;

import eu.kryocloud.api.plugin.logging.IPluginLogger;
import eu.kryocloud.api.plugin.scheduler.IPluginScheduler;
import eu.kryocloud.api.plugin.scheduler.IPluginTask;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public final class VirtualPluginScheduler implements IPluginScheduler, AutoCloseable {

    private final IPluginLogger logger;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public VirtualPluginScheduler(IPluginLogger logger) {
        this.logger = logger;
    }

    @Override
    public IPluginTask async(Runnable task) {
        return submit(task);
    }

    @Override
    public IPluginTask delay(Duration delay, Runnable task) {
        return submit(() -> {
            sleep(delay);
            task.run();
        });
    }

    @Override
    public IPluginTask repeat(Duration delay, Duration interval, Runnable task) {
        return submit(() -> {
            sleep(delay);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    logger.error("Scheduled plugin task failed", throwable);
                }

                sleep(interval);
            }
        });
    }

    @Override
    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private IPluginTask submit(Runnable task) {
        Future<?> future = executor.submit(() -> {
            try {
                task.run();
            } catch (Throwable throwable) {
                logger.error("Plugin task failed", throwable);
            }
        });

        return new VirtualPluginTask(future);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

}
