package eu.kryocloud.wrapper.instance.metrics;

public record InstanceMetrics(String serviceId, long processId, int memoryMb, int cpuLoadPermille, long uptimeMillis) {

    public InstanceMetrics {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        if (processId < 0) {
            throw new IllegalArgumentException("processId must not be negative");
        }

        if (memoryMb < 0) {
            throw new IllegalArgumentException("memoryMb must not be negative");
        }

        if (cpuLoadPermille < 0) {
            throw new IllegalArgumentException("cpuLoadPermille must not be negative");
        }

        if (uptimeMillis < 0) {
            throw new IllegalArgumentException("uptimeMillis must not be negative");
        }
    }

    public static InstanceMetrics unavailable(String serviceId) {
        return new InstanceMetrics(serviceId, 0L, 0, 0, 0L);
    }
}
