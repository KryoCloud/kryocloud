package eu.kryocloud.common.concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class ScheduledTask<T> {

    private final String id;
    private final String name;
    private final TaskKind kind;
    private final TaskPriority priority;
    private final Duration timeout;
    private final CompletableFuture<T> future;
    private final Instant submittedAt;

    private final AtomicReference<Instant> startedAt = new AtomicReference<>();
    private final AtomicReference<Instant> completedAt = new AtomicReference<>();
    private final AtomicReference<Thread> executingThread = new AtomicReference<>();

    public ScheduledTask(String name, TaskKind kind, TaskPriority priority,
                         Duration timeout, CompletableFuture<T> future) {
        this.id = UUID.randomUUID().toString();
        this.name = name != null ? name : "task-" + id.substring(0, 8);
        this.kind = kind;
        this.priority = priority;
        this.timeout = timeout;
        this.future = future;
        this.submittedAt = Instant.now();
    }

    public String id() { return id; }
    public String name() { return name; }
    public TaskKind kind() { return kind; }
    public TaskPriority priority() { return priority; }
    public Duration timeout() { return timeout; }
    public CompletableFuture<T> future() { return future; }
    public Instant submittedAt() { return submittedAt; }

    public Thread executingThread() {
        return executingThread.get();
    }

    public void markStarted() {
        startedAt.compareAndSet(null, Instant.now());
        executingThread.compareAndSet(null, Thread.currentThread());
    }

    public boolean markCompleted() {
        return completedAt.compareAndSet(null, Instant.now());
    }

    public Duration queueDuration() {
        Instant started = startedAt.get();
        if (started == null) return Duration.ZERO;
        return Duration.between(submittedAt, started);
    }

    public Duration executionDuration() {
        Instant started = startedAt.get();
        Instant completed = completedAt.get();
        if (started == null) return Duration.ZERO;
        if (completed == null) return Duration.between(started, Instant.now());
        return Duration.between(started, completed);
    }

    public Duration totalDuration() {
        Instant completed = completedAt.get();
        if (completed == null) return Duration.between(submittedAt, Instant.now());
        return Duration.between(submittedAt, completed);
    }

    public TaskResult<T> toResult(T value, Throwable error, boolean timedOut) {
        return new TaskResult<>(
                id, name, kind, priority,
                value, error, timedOut,
                future.isCancelled(),
                queueDuration(),
                executionDuration(),
                totalDuration()
        );
    }

    @Override
    public String toString() {
        return "ScheduledTask{id=%s, name=%s, kind=%s, priority=%s}"
                .formatted(id, name, kind, priority);
    }
}
