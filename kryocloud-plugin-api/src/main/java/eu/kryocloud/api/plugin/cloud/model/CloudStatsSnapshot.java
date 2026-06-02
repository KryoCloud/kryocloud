package eu.kryocloud.api.plugin.cloud.model;

public record CloudStatsSnapshot(int wrappers, int onlineWrappers, int services, int runningServices, int groups, int usedMemoryMb, int maxMemoryMb) {
}
