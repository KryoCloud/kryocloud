// kryocloud-wrapper/src/main/java/eu/kryocloud/wrapper/heartbeat/WrapperHeartbeatTask.java
package eu.kryocloud.wrapper.heartbeat;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.KryoProtocolClient;
import eu.kryocloud.network.packet.type.wrapper.WrapperHeartbeatPacket;
import eu.kryocloud.network.protocol.WrapperState;
import eu.kryocloud.wrapper.service.WrapperServiceManager;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class WrapperHeartbeatTask implements AutoCloseable {

    private final String wrapperId;
    private final KryoProtocolClient protocolClient;
    private final WrapperServiceManager serviceManager;
    private final ScheduledExecutorService executor;
    private final AtomicLong sequence = new AtomicLong();

    public WrapperHeartbeatTask(String wrapperId, KryoProtocolClient protocolClient, WrapperServiceManager serviceManager, ScheduledExecutorService executor) {
        if (wrapperId == null || wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }

        if (protocolClient == null) {
            throw new IllegalArgumentException("protocolClient must not be null");
        }

        if (serviceManager == null) {
            throw new IllegalArgumentException("serviceManager must not be null");
        }

        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }

        this.wrapperId = wrapperId;
        this.protocolClient = protocolClient;
        this.serviceManager = serviceManager;
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

            protocolClient.send(new WrapperHeartbeatPacket(wrapperId, WrapperState.AVAILABLE, sequence.incrementAndGet(), usedMemoryMb(), maxMemoryMb(), serviceManager.runningServiceCount()));
        } catch (Exception exception) {
            exception.printStackTrace();
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