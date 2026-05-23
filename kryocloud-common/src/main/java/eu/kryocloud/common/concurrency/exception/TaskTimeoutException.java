package eu.kryocloud.common.concurrency.exception;

public final class TaskTimeoutException extends RuntimeException {

    private final String taskId;

    public TaskTimeoutException(String taskId, String message) {
        super(message);
        this.taskId = taskId;
    }

    public String taskId() {
        return taskId;
    }
}
