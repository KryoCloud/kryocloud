package eu.kryocloud.node.console.stats;

import java.util.List;

public record ClusterStatsSnapshot(long timestamp, int wrappersOnline, int wrappersTimedOut, int wrapperMemoryUsedMb, int wrapperMemoryMaxMb, int knownServices, int runningServices, int startingServices, int failedServices, List<GroupStatsSnapshot> groups) {

    public ClusterStatsSnapshot {
        if (timestamp < 1) {
            throw new IllegalArgumentException("timestamp must be greater than 0");
        }

        if (wrappersOnline < 0) {
            throw new IllegalArgumentException("wrappersOnline must not be negative");
        }

        if (wrappersTimedOut < 0) {
            throw new IllegalArgumentException("wrappersTimedOut must not be negative");
        }

        if (wrapperMemoryUsedMb < 0) {
            throw new IllegalArgumentException("wrapperMemoryUsedMb must not be negative");
        }

        if (wrapperMemoryMaxMb < 0) {
            throw new IllegalArgumentException("wrapperMemoryMaxMb must not be negative");
        }

        if (knownServices < 0) {
            throw new IllegalArgumentException("knownServices must not be negative");
        }

        if (runningServices < 0) {
            throw new IllegalArgumentException("runningServices must not be negative");
        }

        if (startingServices < 0) {
            throw new IllegalArgumentException("startingServices must not be negative");
        }

        if (failedServices < 0) {
            throw new IllegalArgumentException("failedServices must not be negative");
        }

        if (groups == null) {
            throw new IllegalArgumentException("groups must not be null");
        }

        groups = List.copyOf(groups);
    }

    public double wrapperMemoryRatio() {
        if (wrapperMemoryMaxMb < 1) {
            return 0.0D;
        }

        return Math.min(1.0D, (double) wrapperMemoryUsedMb / (double) wrapperMemoryMaxMb);
    }

    public int groupCount() {
        return groups.size();
    }
}