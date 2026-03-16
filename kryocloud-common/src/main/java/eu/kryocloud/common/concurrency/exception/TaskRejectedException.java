package eu.kryocloud.common.concurrency.exception;

public final class TaskRejectedException extends RuntimeException {

    private final String taskName;
    private final String reason;

    public TaskRejectedException(String taskName, String reason) {
        super("Task rejected [%s]: %s".formatted(taskName, reason));
        this.taskName = taskName;
        this.reason = reason;
    }

    public String taskName() {
        return taskName;
    }

    public String reason() {
        return reason;
    }
}
