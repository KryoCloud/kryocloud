package eu.kryocloud.common.concurrency;

import java.time.Duration;

public record TaskResult<T>(String taskId, String taskName, TaskKind kind, TaskPriority priority, T value,
                            Throwable error, boolean timedOut, boolean cancelled, Duration queueDuration,
                            Duration executionDuration, Duration totalDuration) {

    public boolean succeeded() {
        return error == null && !timedOut && !cancelled;
    }

    public boolean failed() {
        return error != null;
    }
}
