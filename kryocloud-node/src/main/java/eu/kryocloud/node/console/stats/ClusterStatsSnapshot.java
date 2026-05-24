package eu.kryocloud.node.console.stats;

import java.util.List;

public record ClusterStatsSnapshot(long timestamp, int wrappersOnline, int wrappersTimedOut, int processMemoryMb, int wrapperMemoryMaxMb, int cpuLoadPermille, int knownServices, int runningServices, int startingServices, int failedServices, List<GroupStatsSnapshot> groups) {

    public ClusterStatsSnapshot {
        if (timestamp < 1) {
            throw new IllegalArgumentException("timestamp must be greater than 0");
        }

        if (wrappersOnline < 0 || wrappersTimedOut < 0 || processMemoryMb < 0 || wrapperMemoryMaxMb < 0 || cpuLoadPermille < 0 || knownServices < 0 || runningServices < 0 || startingServices < 0 || failedServices < 0) {
            throw new IllegalArgumentException("stats values must not be negative");
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

        return Math.min(1.0D, (double) processMemoryMb / (double) wrapperMemoryMaxMb);
    }

    public double cpuLoadRatio() {
        return Math.max(0.0D, Math.min(1.0D, cpuLoadPermille / 1000.0D));
    }

    public int groupCount() {
        return groups.size();
    }
}
