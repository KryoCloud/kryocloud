package eu.kryocloud.api.plugin.scheduler;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface IPluginScheduler {

    IPluginTask async(Runnable task);

    IPluginTask delay(Duration delay, Runnable task);

    IPluginTask repeat(Duration delay, Duration interval, Runnable task);

    <T> CompletableFuture<T> supply(Supplier<T> supplier);

}
