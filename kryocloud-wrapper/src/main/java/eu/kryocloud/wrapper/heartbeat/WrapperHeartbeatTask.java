package eu.kryocloud.wrapper.heartbeat;

import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.KryoProtocolClient;
import eu.kryocloud.network.packet.type.wrapper.WrapperHeartbeatPacket;
import eu.kryocloud.network.protocol.WrapperState;
import eu.kryocloud.wrapper.instance.InstanceManager;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
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

    public WrapperHeartbeatTask(String wrapperId, KryoProtocolClient protocolClient, InstanceManager instanceManager, ScheduledExecutorService executor) {
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

        this.wrapperId = wrapperId;
        this.protocolClient = protocolClient;
        this.instanceManager = instanceManager;
        this.executor = executor;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::sendHeartbeatSafely, KryoProtocol.HEARTBEAT_INTERVAL_MILLIS, KryoProtocol.HEARTBEAT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeatSafely() {
        try {
            if (!protocolClient.isConnected()) {
                return;
            }

            protocolClient.send(new WrapperHeartbeatPacket(wrapperId, WrapperState.AVAILABLE, sequence.incrementAndGet(), usedMemoryMb(), maxMemoryMb(), instanceManager.runningInstanceCount()));
        } catch (Exception exception) {
            LOGGER.warn("Failed to send wrapper heartbeat: " + exception.getMessage());
        }
    }

    private int usedMemoryMb() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long usedBytes = memory.getHeapMemoryUsage().getUsed();
        return Math.toIntExact(usedBytes / 1024L / 1024L);
    }

    private int maxMemoryMb() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long maxBytes = memory.getHeapMemoryUsage().getMax();

        if (maxBytes < 1) {
            return 1;
        }

        return Math.toIntExact(maxBytes / 1024L / 1024L);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
