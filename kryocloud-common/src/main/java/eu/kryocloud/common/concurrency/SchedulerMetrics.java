package eu.kryocloud.common.concurrency;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class SchedulerMetrics {

    private final boolean enabled;

    private final Map<TaskKind, LongAdder> submitted = new ConcurrentHashMap<>();
    private final Map<TaskKind, LongAdder> started = new ConcurrentHashMap<>();
    private final Map<TaskKind, LongAdder> completed = new ConcurrentHashMap<>();
    private final Map<TaskKind, LongAdder> failed = new ConcurrentHashMap<>();
    private final Map<TaskKind, LongAdder> timedOut = new ConcurrentHashMap<>();
    private final Map<TaskKind, LongAdder> cancelled = new ConcurrentHashMap<>();
    private final Map<TaskKind, LongAdder> rejected = new ConcurrentHashMap<>();

    private final Map<TaskKind, LongAdder> totalQueueNanos = new ConcurrentHashMap<>();
    private final Map<TaskKind, LongAdder> totalExecNanos = new ConcurrentHashMap<>();

    public SchedulerMetrics(boolean enabled) {
        this.enabled = enabled;
        for (TaskKind kind : TaskKind.values()) {
            submitted.put(kind, new LongAdder());
            started.put(kind, new LongAdder());
            completed.put(kind, new LongAdder());
            failed.put(kind, new LongAdder());
            timedOut.put(kind, new LongAdder());
            cancelled.put(kind, new LongAdder());
            rejected.put(kind, new LongAdder());
            totalQueueNanos.put(kind, new LongAdder());
            totalExecNanos.put(kind, new LongAdder());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void recordSubmitted(TaskKind kind) {
        if (!enabled) return;
        submitted.get(kind).increment();
    }

    public void recordStarted(TaskKind kind) {
        if (!enabled) return;
        started.get(kind).increment();
    }

    public void recordRejected(TaskKind kind) {
        if (!enabled) return;
        rejected.get(kind).increment();
    }

    public <T> void recordCompleted(TaskResult<T> result) {
        if (!enabled) return;

        TaskKind kind = result.kind();
        completed.get(kind).increment();

        if (result.timedOut()) timedOut.get(kind).increment();
        if (result.cancelled()) cancelled.get(kind).increment();
        if (result.failed() && !result.timedOut() && !result.cancelled()) {
            failed.get(kind).increment();
        }

        totalQueueNanos.get(kind).add(result.queueDuration().toNanos());
        totalExecNanos.get(kind).add(result.executionDuration().toNanos());
    }

    public long submitted(TaskKind kind) { return submitted.get(kind).sum(); }
    public long started(TaskKind kind) { return started.get(kind).sum(); }
    public long completed(TaskKind kind) { return completed.get(kind).sum(); }
    public long failed(TaskKind kind) { return failed.get(kind).sum(); }
    public long timedOut(TaskKind kind) { return timedOut.get(kind).sum(); }
    public long cancelled(TaskKind kind) { return cancelled.get(kind).sum(); }
    public long rejected(TaskKind kind) { return rejected.get(kind).sum(); }

    public Duration avgQueueDuration(TaskKind kind) {
        long count = completed.get(kind).sum();
        if (count == 0) return Duration.ZERO;
        return Duration.ofNanos(totalQueueNanos.get(kind).sum() / count);
    }

    public Duration avgExecDuration(TaskKind kind) {
        long count = completed.get(kind).sum();
        if (count == 0) return Duration.ZERO;
        return Duration.ofNanos(totalExecNanos.get(kind).sum() / count);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SchedulerMetrics{\n");
        for (TaskKind kind : TaskKind.values()) {
            sb.append("  %s: submitted=%d, started=%d, completed=%d, failed=%d, timedOut=%d, cancelled=%d, rejected=%d, avgQueue=%s, avgExec=%s\n".formatted(kind, submitted(kind), started(kind), completed(kind), failed(kind), timedOut(kind), cancelled(kind), rejected(kind), avgQueueDuration(kind), avgExecDuration(kind)));
        }
        sb.append("}");
        return sb.toString();
    }
}
