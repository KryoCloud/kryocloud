package eu.kryocloud.common.concurrency.rejection;

import java.util.concurrent.CompletableFuture;

public interface SchedulerRejectionHandler {

    <T> void handle(Runnable task, CompletableFuture<T> future, String taskName);
}
