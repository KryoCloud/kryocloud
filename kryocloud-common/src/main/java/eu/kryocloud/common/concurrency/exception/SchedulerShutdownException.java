package eu.kryocloud.common.concurrency.exception;

public final class SchedulerShutdownException extends RuntimeException {

    public SchedulerShutdownException() {
        super("CloudScheduler is shut down, no new tasks accepted");
    }
}
