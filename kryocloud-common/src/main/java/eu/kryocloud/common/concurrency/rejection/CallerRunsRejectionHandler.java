package eu.kryocloud.common.concurrency.rejection;

import eu.kryocloud.common.concurrency.exception.TaskRejectedException;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CallerRunsRejectionHandler implements SchedulerRejectionHandler {

    private static final Logger LOG = Logger.getLogger(CallerRunsRejectionHandler.class.getName());

    private final boolean allowOnEventLoop;

    public CallerRunsRejectionHandler() {
        this(false);
    }

    public CallerRunsRejectionHandler(boolean allowOnEventLoop) {
        this.allowOnEventLoop = allowOnEventLoop;
    }

    private static boolean isNettyEventLoop() {
        String threadName = Thread.currentThread().getName().toLowerCase();
        return threadName.contains("nioevento") || threadName.contains("epoll") || threadName.contains("kqueue") || threadName.contains("event-loop");
    }

    @Override
    public <T> void handle(Runnable task, CompletableFuture<T> future, String taskName) {
        if (!allowOnEventLoop && isNettyEventLoop()) {
            TaskRejectedException ex = new TaskRejectedException(taskName, "queue full, policy=CALLER_RUNS blocked on EventLoop, falling back to ABORT");
            future.completeExceptionally(ex);
            LOG.log(Level.WARNING, "CallerRuns rejected on Netty EventLoop for task: " + taskName);
            return;
        }

        LOG.log(Level.FINE, "CallerRuns executing task in calling thread: " + taskName);
        task.run();
    }
}
