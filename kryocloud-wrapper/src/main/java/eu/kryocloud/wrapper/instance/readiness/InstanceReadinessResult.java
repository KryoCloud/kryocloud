package eu.kryocloud.wrapper.instance.readiness;

public record InstanceReadinessResult(boolean ready, String message, String logs) {

    public InstanceReadinessResult {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        if (logs == null) {
            throw new IllegalArgumentException("logs must not be null");
        }
    }

    public static InstanceReadinessResult ready(String message, String logs) {
        return new InstanceReadinessResult(true, message, logs);
    }

    public static InstanceReadinessResult failed(String message, String logs) {
        return new InstanceReadinessResult(false, message, logs);
    }
}
