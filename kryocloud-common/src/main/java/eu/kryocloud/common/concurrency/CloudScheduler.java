package eu.kryocloud.common.concurrency;

import eu.kryocloud.common.concurrency.exception.*;
import eu.kryocloud.common.concurrency.rejection.*;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CloudScheduler implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(CloudScheduler.class.getName());

    private static final Comparator<Runnable> PRIORITY_COMPARATOR = (a, b) -> {
        int pa = (a instanceof PrioritizedRunnable pr) ? pr.priority() : 0;
        int pb = (b instanceof PrioritizedRunnable pr) ? pr.priority() : 0;
        return Integer.compare(pb, pa);
    };

    private final SchedulerConfig config;
    private final SchedulerMetrics metrics;
    private final SchedulerRejectionHandler cpuRejectionHandler;
    private final SchedulerRejectionHandler bgRejectionHandler;

    private final ThreadPoolExecutor cpuPool;
    private final ExecutorService ioPool;
    private final ThreadPoolExecutor backgroundPool;
    private final ScheduledExecutorService timeoutWatcher;

    private final BoundedPriorityQueue<Runnable> cpuQueue;
    private final BoundedPriorityQueue<Runnable> bgQueue;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public CloudScheduler() {
        this(SchedulerConfig.defaults());
    }

    public CloudScheduler(SchedulerConfig config) {
        this.config = Objects.requireNonNull(config);
        this.metrics = new SchedulerMetrics(config.metricsEnabled());

        this.cpuQueue = new BoundedPriorityQueue<>(
                config.maxQueuedTasksPerPool(), PRIORITY_COMPARATOR);

        this.bgQueue = new BoundedPriorityQueue<>(
                config.maxQueuedTasksPerPool(), PRIORITY_COMPARATOR);

        this.cpuRejectionHandler = createRejectionHandler(config.rejectionPolicy(), cpuQueue, config);
        this.bgRejectionHandler = createRejectionHandler(config.rejectionPolicy(), bgQueue, config);

        Thread.UncaughtExceptionHandler uncaughtHandler = (thread, throwable) ->
                LOG.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), throwable);

        this.cpuPool = new ThreadPoolExecutor(
                config.cpuPoolSize(), config.cpuPoolSize(),
                0L, TimeUnit.MILLISECONDS,
                cpuQueue,
                Thread.ofPlatform()
                        .name("kryocloud-cpu-", 0)
                        .uncaughtExceptionHandler(uncaughtHandler)
                        .factory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        this.ioPool = Executors.newVirtualThreadPerTaskExecutor();

        this.backgroundPool = new ThreadPoolExecutor(
                config.backgroundPoolSize(), config.backgroundPoolSize(),
                0L, TimeUnit.MILLISECONDS,
                bgQueue,
                Thread.ofPlatform().name("kryocloud-bg-", 0).daemon(true).priority(Thread.MIN_PRIORITY).uncaughtExceptionHandler(uncaughtHandler).factory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        this.timeoutWatcher = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("kryocloud-timeout-watcher").daemon(true).uncaughtExceptionHandler(uncaughtHandler).factory()
        );
    }

    private static SchedulerRejectionHandler createRejectionHandler(RejectionPolicy policy, BlockingQueue<Runnable> queue, SchedulerConfig config) {
        return switch (policy) {
            case ABORT -> new AbortRejectionHandler();
            case CALLER_RUNS -> new CallerRunsRejectionHandler(config.callerRunsAllowedOnEventLoop());
            case DISCARD -> new DiscardRejectionHandler();
            case DISCARD_OLDEST -> new DiscardOldestRejectionHandler((BoundedPriorityQueue<Runnable>) queue);
        };
    }

    public <T> ScheduledTask<T> submit(TaskKind kind, Supplier<T> task) {
        return submit(kind, null, TaskPriority.NORMAL, config.defaultTimeout(), task);
    }

    public <T> ScheduledTask<T> submit(TaskKind kind, Duration timeout, Supplier<T> task) {
        return submit(kind, null, TaskPriority.NORMAL, timeout, task);
    }

    public <T> ScheduledTask<T> submit(TaskKind kind, TaskPriority priority, Supplier<T> task) {
        return submit(kind, null, priority, config.defaultTimeout(), task);
    }

    public <T> ScheduledTask<T> submit(TaskKind kind, String name,
                                       TaskPriority priority, Duration timeout,
                                       Supplier<T> task) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(task);

        if (shutdown.get()) {
            throw new SchedulerShutdownException();
        }

        Duration effectiveTimeout = timeout != null ? timeout : config.defaultTimeout();
        TaskPriority effectivePriority = priority != null ? priority : TaskPriority.NORMAL;

        CompletableFuture<T> future = new CompletableFuture<>();
        ScheduledTask<T> scheduledTask = new ScheduledTask<>(
                name, kind, effectivePriority, effectiveTimeout, future);

        metrics.recordSubmitted(kind);

        Runnable wrappedWork = () -> executeTask(scheduledTask, task);

        switch (kind) {
            case BLOCKING_IO -> submitToIo(wrappedWork, scheduledTask);
            case CPU_BOUND -> submitToBounded(cpuPool, cpuQueue, cpuRejectionHandler, scheduledTask, wrappedWork);
            case BACKGROUND -> submitToBounded(backgroundPool, bgQueue, bgRejectionHandler, scheduledTask, wrappedWork);
        }

        if (!future.isDone()) {
            scheduleTimeout(scheduledTask, effectiveTimeout);
        }

        return scheduledTask;
    }

    private <T> void submitToIo(Runnable work, ScheduledTask<T> scheduledTask) {
        try {
            ioPool.execute(work);
        } catch (RejectedExecutionException e) {
            metrics.recordRejected(scheduledTask.kind());
            scheduledTask.future().completeExceptionally(
                    new TaskRejectedException(scheduledTask.name(), "I/O pool rejected"));
        }
    }

    private <T> void submitToBounded(ThreadPoolExecutor pool, BoundedPriorityQueue<Runnable> queue,
                                     SchedulerRejectionHandler handler, ScheduledTask<T> scheduledTask,
                                     Runnable work) {
        PrioritizedRunnable prioritized = new PrioritizedRunnable(
                scheduledTask.priority().weight(), work);

        try {
            pool.execute(prioritized);
        } catch (RejectedExecutionException e) {
            metrics.recordRejected(scheduledTask.kind());
            handler.handle(prioritized, scheduledTask.future(), scheduledTask.name());
        }
    }

    public ScheduledTask<Void> run(TaskKind kind, Runnable task) {
        return submit(kind, () -> {
            task.run();
            return null;
        });
    }

    public ScheduledTask<Void> run(TaskKind kind, String name,
                                   TaskPriority priority, Duration timeout,
                                   Runnable task) {
        return submit(kind, name, priority, timeout, () -> {
            task.run();
            return null;
        });
    }

    private <T> void scheduleTimeout(ScheduledTask<T> scheduledTask, Duration timeout) {
        if (timeout == null) return;
        if (timeout.isZero()) return;
        if (timeout.isNegative()) return;

        CompletableFuture<T> future = scheduledTask.future();

        timeoutWatcher.schedule(() -> {
            if (future.isDone()) return;

            Thread t = scheduledTask.executingThread();
            if (t != null) {
                t.interrupt();
            }

            TaskTimeoutException ex = new TaskTimeoutException(
                    scheduledTask.id(),
                    "Task '%s' timed out after %dms (cooperative interrupt sent)"
                            .formatted(scheduledTask.name(), timeout.toMillis()));

            boolean won = future.completeExceptionally(ex);

            if (won) {
                scheduledTask.markCompleted();
                TaskResult<T> result = scheduledTask.toResult(null, ex, true);
                metrics.recordCompleted(result);
                LOG.warning("Task timed out: " + scheduledTask);
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private <T> void executeTask(ScheduledTask<T> scheduledTask, Supplier<T> work) {
        CompletableFuture<T> future = scheduledTask.future();

        if (future.isDone()) return;

        scheduledTask.markStarted();
        metrics.recordStarted(scheduledTask.kind());

        T value = null;
        Throwable error = null;
        boolean isCancellation = false;

        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Task interrupted before execution");
            }
            value = work.get();
        } catch (CancellationException e) {
            isCancellation = true;
            error = e;
        } catch (InterruptedException e) {
            isCancellation = true;
            error = e;
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            error = t;
        }

        boolean won;
        if (isCancellation) {
            won = future.cancel(true);
        }
        if (!isCancellation && error != null) {
            won = future.completeExceptionally(error);
            if (won) {
                LOG.log(Level.WARNING, "Task failed: " + scheduledTask, error);
            }
        }
        if (!isCancellation && error == null) {
            won = future.complete(value);
        }
        if (isCancellation) {
            won = future.isCancelled();
        }
        if (!isCancellation && error != null) {
            won = future.isCompletedExceptionally();
        }
        if (!isCancellation && error == null) {
            won = future.isDone() && !future.isCompletedExceptionally();
        }

        boolean completionWon = scheduledTask.markCompleted();

        if (completionWon) {
            TaskResult<T> result = scheduledTask.toResult(value, error, false);
            metrics.recordCompleted(result);
        }
    }

    public SchedulerMetrics metrics() {
        return metrics;
    }

    public SchedulerConfig config() {
        return config;
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void close() {
        close(config.shutdownGracePeriod());
    }

    public void close(Duration gracePeriod) {
        if (!shutdown.compareAndSet(false, true)) return;

        LOG.info("CloudScheduler shutting down (grace=" + gracePeriod.toMillis() + "ms)...");

        List<ExecutorService> pools = List.of(cpuPool, ioPool, backgroundPool, timeoutWatcher);
        pools.forEach(ExecutorService::shutdown);

        long deadlineMs = System.currentTimeMillis() + gracePeriod.toMillis();
        for (ExecutorService pool : pools) {
            long remaining = deadlineMs - System.currentTimeMillis();

            if (remaining <= 0) {
                pool.shutdownNow();
                continue;
            }

            try {
                if (!pool.awaitTermination(remaining, TimeUnit.MILLISECONDS)) {
                    LOG.warning("Pool did not terminate in time, forcing: " + pool);
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        LOG.info("CloudScheduler shutdown complete.");
        if (metrics.isEnabled()) {
            LOG.info(metrics.toString());
        }
    }

    public void shutdownNow() {
        if (!shutdown.compareAndSet(false, true)) return;

        LOG.warning("CloudScheduler forced shutdown!");
        cpuPool.shutdownNow();
        ioPool.shutdownNow();
        backgroundPool.shutdownNow();
        timeoutWatcher.shutdownNow();
    }

    record PrioritizedRunnable(int priority, Runnable delegate) implements Runnable {

        @Override
        public void run() {
            delegate.run();
        }
    }
}
