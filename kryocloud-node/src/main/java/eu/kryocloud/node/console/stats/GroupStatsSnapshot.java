package eu.kryocloud.node.console.stats;

import eu.kryocloud.api.service.ServiceType;

public record GroupStatsSnapshot(String groupName, ServiceType serviceType, String software, String softwareVersion, int knownServices, int runningServices, int failedServices, int serviceCount, int minCount, int maxCount, int configuredMemoryMb, int maxMemoryCapacityMb, boolean staticServices) {

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

        if (knownServices < 0) {
            throw new IllegalArgumentException("knownServices must not be negative");
        }

        if (runningServices < 0) {
            throw new IllegalArgumentException("runningServices must not be negative");
        }

        if (failedServices < 0) {
            throw new IllegalArgumentException("failedServices must not be negative");
        }

        if (serviceCount < 0) {
            throw new IllegalArgumentException("serviceCount must not be negative");
        }

        if (minCount < 0) {
            throw new IllegalArgumentException("minCount must not be negative");
        }

        if (maxCount < minCount) {
            throw new IllegalArgumentException("maxCount must be greater than or equal to minCount");
        }

        if (configuredMemoryMb < 0) {
            throw new IllegalArgumentException("configuredMemoryMb must not be negative");
        }

        if (maxMemoryCapacityMb < 0) {
            throw new IllegalArgumentException("maxMemoryCapacityMb must not be negative");
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

        return Math.min(1.0D, (double) configuredMemoryMb / (double) maxMemoryCapacityMb);
    }
}