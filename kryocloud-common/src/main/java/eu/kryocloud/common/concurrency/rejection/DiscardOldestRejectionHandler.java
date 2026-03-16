package eu.kryocloud.common.concurrency.rejection;

import eu.kryocloud.common.concurrency.BoundedPriorityQueue;
import eu.kryocloud.common.concurrency.exception.TaskRejectedException;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiscardOldestRejectionHandler implements SchedulerRejectionHandler {

    private static final Logger LOG = Logger.getLogger(DiscardOldestRejectionHandler.class.getName());

    private final BoundedPriorityQueue<Runnable> queue;

    public DiscardOldestRejectionHandler(BoundedPriorityQueue<Runnable> queue) {
        this.queue = queue;
    }

    @Override
    public <T> void handle(Runnable task, CompletableFuture<T> future, String taskName) {
        Runnable evicted = queue.pollHead();

        if (evicted != null) {
            LOG.log(Level.WARNING,
                    "DISCARD_OLDEST: evicted head element to make room for task: {0}", taskName);
        }

        boolean inserted = queue.offer(task);

        if (!inserted) {
            TaskRejectedException ex = new TaskRejectedException(
                    taskName,
                    "queue full even after evicting head, policy=DISCARD_OLDEST"
            );
            future.completeExceptionally(ex);
            LOG.log(Level.WARNING,
                    "DISCARD_OLDEST: still no room after eviction for task: {0}", taskName);
        }
    }
}
