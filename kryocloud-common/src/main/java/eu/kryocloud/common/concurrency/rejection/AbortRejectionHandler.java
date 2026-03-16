package eu.kryocloud.common.concurrency.rejection;

import eu.kryocloud.common.concurrency.exception.TaskRejectedException;

import java.util.concurrent.CompletableFuture;

public final class AbortRejectionHandler implements SchedulerRejectionHandler {

    @Override
    public <T> void handle(Runnable task, CompletableFuture<T> future, String taskName) {
        TaskRejectedException ex = new TaskRejectedException(taskName, "queue full, policy=ABORT");
        future.completeExceptionally(ex);
    }
}
