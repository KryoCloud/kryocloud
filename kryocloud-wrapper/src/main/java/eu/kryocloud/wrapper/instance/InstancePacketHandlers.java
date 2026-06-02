package eu.kryocloud.wrapper.instance;

import eu.kryocloud.common.concurrency.CloudScheduler;
import eu.kryocloud.common.concurrency.TaskKind;
import eu.kryocloud.common.concurrency.TaskPriority;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.packet.bus.KryoPacketBus;
import eu.kryocloud.network.packet.bus.PacketContext;
import eu.kryocloud.network.packet.bus.PacketSubscription;
import eu.kryocloud.network.packet.type.service.ServiceCleanupRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceCommandRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceLogsRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStartRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStopRequestPacket;
import eu.kryocloud.network.protocol.PeerType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InstancePacketHandlers implements AutoCloseable {

    private static final KryoLogger LOGGER = KryoLogger.logger("Instances");

    private final InstanceManager instanceManager;
    private final CloudScheduler scheduler;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final List<PacketSubscription> subscriptions = new ArrayList<>();

    public InstancePacketHandlers(InstanceManager instanceManager, CloudScheduler scheduler) {
        if (instanceManager == null) {
            throw new IllegalArgumentException("instanceManager must not be null");
        }

        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler must not be null");
        }

        this.instanceManager = instanceManager;
        this.scheduler = scheduler;
    }

    public void register() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }

        subscriptions.add(KryoPacketBus.listen(ServiceStartRequestPacket.class, this::handleServiceStart));
        subscriptions.add(KryoPacketBus.listen(ServiceStopRequestPacket.class, this::handleServiceStop));
        subscriptions.add(KryoPacketBus.listen(ServiceCommandRequestPacket.class, this::handleServiceCommand));
        subscriptions.add(KryoPacketBus.listen(ServiceLogsRequestPacket.class, this::handleServiceLogs));
        subscriptions.add(KryoPacketBus.listen(ServiceCleanupRequestPacket.class, this::handleServiceCleanup));
    }

    private void handleServiceStart(PacketContext context, ServiceStartRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        offload("start " + packet.serviceId(), () -> instanceManager.start(context.connection(), packet));
    }

    private void handleServiceStop(PacketContext context, ServiceStopRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        offload("stop " + packet.serviceId(), () -> {
            if (instanceManager.stop(context.connection(), packet).isPresent()) {
                return;
            }

            LOGGER.warn("Stop requested for unknown Minecraft instance " + packet.serviceId());
        });
    }

    private void handleServiceCommand(PacketContext context, ServiceCommandRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        offload("command " + packet.serviceId(), () -> instanceManager.command(context.connection(), packet));
    }

    private void handleServiceLogs(PacketContext context, ServiceLogsRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        offload("logs " + packet.serviceId(), () -> instanceManager.logs(context.connection(), packet));
    }

    private void handleServiceCleanup(PacketContext context, ServiceCleanupRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        offload("cleanup " + packet.requestId(), () -> instanceManager.cleanup(context.connection(), packet));
    }

    private void offload(String name, Runnable work) {
        scheduler.run(TaskKind.BLOCKING_IO, "instance " + name, TaskPriority.HIGH, Duration.ofMinutes(10), work)
                .future()
                .exceptionally(error -> {
                    LOGGER.warn("Instance task failed: " + error.getMessage());
                    return null;
                });
    }

    private boolean isNodeConnection(PacketContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        if (!context.authenticated()) {
            return false;
        }

        return context.connection().peerType() == PeerType.NODE;
    }

    @Override
    public void close() {
        if (!registered.compareAndSet(true, false)) {
            return;
        }

        for (PacketSubscription subscription : subscriptions) {
            subscription.close();
        }

        subscriptions.clear();
    }
}
