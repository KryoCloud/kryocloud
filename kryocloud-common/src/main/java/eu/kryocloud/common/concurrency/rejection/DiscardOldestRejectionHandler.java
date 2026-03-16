package eu.kryocloud.common.concurrency.rejection;

import eu.kryocloud.common.concurrency.exception.TaskRejectedException;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class DiscardOldestRejectionHandler implements SchedulerRejectionHandler {

    private static final Logger LOG = Logger.getLogger(DiscardOldestRejectionHandler.class.getName());

    @Override
    public void handle(Runnable task, CompletableFuture<?> future, String taskName) {
        LOG.warning("Pool full, discarding oldest and rejecting current task: " + taskName);
        future.completeExceptionally(
                new TaskRejectedException("Rejected after discard-oldest for: " + taskName));
    }
}
