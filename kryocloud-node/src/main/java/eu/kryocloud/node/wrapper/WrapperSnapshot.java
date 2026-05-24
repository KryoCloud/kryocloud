package eu.kryocloud.node.wrapper;

import eu.kryocloud.network.protocol.WrapperState;

import java.util.UUID;

public record WrapperSnapshot(String wrapperId, UUID connectionId, String hostname, String address, String osName, int availableProcessors, int maxMemoryMb, int usedMemoryMb, int runningServices, int cpuLoadPermille, WrapperState state, long registeredAtMillis, long lastHeartbeatAtMillis, String remoteAddress) {

    public WrapperSnapshot {
        if (wrapperId == null || wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }

        if (connectionId == null) {
            throw new IllegalArgumentException("connectionId must not be null");
        }

        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("hostname must not be blank");
        }

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank");
        }

        if (osName == null || osName.isBlank()) {
            throw new IllegalArgumentException("osName must not be blank");
        }

        if (availableProcessors < 1) {
            throw new IllegalArgumentException("availableProcessors must be greater than 0");
        }

        if (maxMemoryMb < 1) {
            throw new IllegalArgumentException("maxMemoryMb must be greater than 0");
        }

        if (usedMemoryMb < 0) {
            throw new IllegalArgumentException("usedMemoryMb must not be negative");
        }

        if (runningServices < 0) {
            throw new IllegalArgumentException("runningServices must not be negative");
        }

        if (cpuLoadPermille < 0) {
            throw new IllegalArgumentException("cpuLoadPermille must not be negative");
        }

        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }

        if (registeredAtMillis < 1) {
            throw new IllegalArgumentException("registeredAtMillis must be greater than 0");
        }

        if (lastHeartbeatAtMillis < 1) {
            throw new IllegalArgumentException("lastHeartbeatAtMillis must be greater than 0");
        }

        if (remoteAddress == null) {
            throw new IllegalArgumentException("remoteAddress must not be null");
        }
    }

    public int availableMemoryMb() {
        return Math.max(0, maxMemoryMb - usedMemoryMb);
    }

    public double cpuLoadRatio() {
        return Math.max(0.0D, Math.min(1.0D, cpuLoadPermille / 1000.0D));
    }

    public boolean timedOut(long timeoutMillis) {
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeoutMillis must be greater than 0");
        }

        return System.currentTimeMillis() - lastHeartbeatAtMillis > timeoutMillis;
    }

    public WrapperSnapshot withHeartbeat(WrapperState newState, long heartbeatTimestamp, int newUsedMemoryMb, int newMaxMemoryMb, int newRunningServices, int newCpuLoadPermille) {
        if (newState == null) {
            throw new IllegalArgumentException("newState must not be null");
        }

        if (heartbeatTimestamp < 1) {
            throw new IllegalArgumentException("heartbeatTimestamp must be greater than 0");
        }

        if (newUsedMemoryMb < 0) {
            throw new IllegalArgumentException("newUsedMemoryMb must not be negative");
        }

        if (newMaxMemoryMb < 1) {
            throw new IllegalArgumentException("newMaxMemoryMb must be greater than 0");
        }

        if (newRunningServices < 0) {
            throw new IllegalArgumentException("newRunningServices must not be negative");
        }

        if (newCpuLoadPermille < 0) {
            throw new IllegalArgumentException("newCpuLoadPermille must not be negative");
        }

        return new WrapperSnapshot(wrapperId, connectionId, hostname, address, osName, availableProcessors, newMaxMemoryMb, newUsedMemoryMb, newRunningServices, newCpuLoadPermille, newState, registeredAtMillis, heartbeatTimestamp, remoteAddress);
    }

    public WrapperSnapshot offline() {
        return new WrapperSnapshot(wrapperId, connectionId, hostname, address, osName, availableProcessors, maxMemoryMb, usedMemoryMb, runningServices, cpuLoadPermille, WrapperState.OFFLINE, registeredAtMillis, System.currentTimeMillis(), remoteAddress);
    }
}
