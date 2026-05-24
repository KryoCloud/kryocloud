package eu.kryocloud.wrapper.heartbeat;

import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.KryoProtocolClient;
import eu.kryocloud.network.packet.type.service.ServiceMetricsPacket;
import eu.kryocloud.network.packet.type.wrapper.WrapperHeartbeatPacket;
import eu.kryocloud.network.protocol.WrapperState;
import eu.kryocloud.wrapper.instance.InstanceManager;
import eu.kryocloud.wrapper.instance.metrics.InstanceMetrics;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class WrapperHeartbeatTask implements AutoCloseable {

    private static final KryoLogger LOGGER = KryoLogger.logger("WrapperHeartbeat");

    private final String wrapperId;
    private final KryoProtocolClient protocolClient;
    private final InstanceManager instanceManager;
    private final ScheduledExecutorService executor;
    private final AtomicLong sequence = new AtomicLong();
    private final int maxMemoryMb;

    public WrapperHeartbeatTask(String wrapperId, KryoProtocolClient protocolClient, InstanceManager instanceManager, ScheduledExecutorService executor, int maxMemoryMb) {
        if (wrapperId == null || wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }

        if (protocolClient == null) {
            throw new IllegalArgumentException("protocolClient must not be null");
        }

        if (instanceManager == null) {
            throw new IllegalArgumentException("instanceManager must not be null");
        }

        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }

        if (maxMemoryMb < 1) {
            throw new IllegalArgumentException("maxMemoryMb must be greater than 0");
        }

        this.wrapperId = wrapperId;
        this.protocolClient = protocolClient;
        this.instanceManager = instanceManager;
        this.executor = executor;
        this.maxMemoryMb = maxMemoryMb;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::sendHeartbeatSafely, 0L, KryoProtocol.HEARTBEAT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeatSafely() {
        try {
            if (!protocolClient.isConnected()) {
                return;
            }

            List<InstanceMetrics> metrics = instanceManager.metrics();
            protocolClient.send(new WrapperHeartbeatPacket(wrapperId, WrapperState.AVAILABLE, sequence.incrementAndGet(), usedMemoryMb(metrics), maxMemoryMb, instanceManager.runningInstanceCount(), cpuLoadPermille(metrics)));

            for (InstanceMetrics metric : metrics) {
                protocolClient.send(new ServiceMetricsPacket(metric.serviceId(), wrapperId, metric.memoryMb(), metric.cpuLoadPermille(), metric.uptimeMillis()));
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to send wrapper heartbeat: " + exception.getMessage());
        }
    }

    private int usedMemoryMb(List<InstanceMetrics> metrics) {
        return Math.min(maxMemoryMb, metrics.stream().mapToInt(InstanceMetrics::memoryMb).sum());
    }

    private int cpuLoadPermille(List<InstanceMetrics> metrics) {
        return Math.min(1000, metrics.stream().mapToInt(InstanceMetrics::cpuLoadPermille).sum());
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
