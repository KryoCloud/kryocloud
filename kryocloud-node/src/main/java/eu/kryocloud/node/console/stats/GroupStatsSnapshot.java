package eu.kryocloud.node.console.stats;

import eu.kryocloud.api.service.ServiceType;

public record GroupStatsSnapshot(String groupName, ServiceType serviceType, String software, String softwareVersion, int knownServices, int runningServices, int failedServices, int serviceCount, int minCount, int maxCount, int reservedMemoryMb, int processMemoryMb, int maxMemoryCapacityMb, int cpuLoadPermille, boolean staticServices) {

    public GroupStatsSnapshot {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }

        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType must not be null");
        }

        if (software == null || software.isBlank()) {
            throw new IllegalArgumentException("software must not be blank");
        }

        if (softwareVersion == null || softwareVersion.isBlank()) {
            throw new IllegalArgumentException("softwareVersion must not be blank");
        }

        if (knownServices < 0 || runningServices < 0 || failedServices < 0 || serviceCount < 0 || minCount < 0) {
            throw new IllegalArgumentException("service counters must not be negative");
        }

        if (maxCount < minCount) {
            throw new IllegalArgumentException("maxCount must be greater than or equal to minCount");
        }

        if (reservedMemoryMb < 0 || processMemoryMb < 0 || maxMemoryCapacityMb < 0 || cpuLoadPermille < 0) {
            throw new IllegalArgumentException("usage values must not be negative");
        }
    }

    public double serviceUsageRatio() {
        if (maxCount < 1) {
            return 0.0D;
        }

        return Math.min(1.0D, (double) runningServices / (double) maxCount);
    }

    public double memoryUsageRatio() {
        if (maxMemoryCapacityMb < 1) {
            return 0.0D;
        }

        return Math.min(1.0D, (double) processMemoryMb / (double) maxMemoryCapacityMb);
    }

    public double cpuLoadRatio() {
        return Math.max(0.0D, Math.min(1.0D, cpuLoadPermille / 1000.0D));
    }

    public boolean metricsAvailable() {
        return processMemoryMb > 0 || cpuLoadPermille > 0;
    }
}
