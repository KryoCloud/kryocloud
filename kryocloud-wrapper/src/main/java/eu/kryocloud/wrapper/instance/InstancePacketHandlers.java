package eu.kryocloud.wrapper.instance;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InstancePacketHandlers implements AutoCloseable {

    private static final KryoLogger LOGGER = KryoLogger.logger("Instances");

    private final InstanceManager instanceManager;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final List<PacketSubscription> subscriptions = new ArrayList<>();

    public InstancePacketHandlers(InstanceManager instanceManager) {
        if (instanceManager == null) {
            throw new IllegalArgumentException("instanceManager must not be null");
        }

        this.instanceManager = instanceManager;
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

        instanceManager.start(context.connection(), packet);
    }

    private void handleServiceStop(PacketContext context, ServiceStopRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        instanceManager.stop(context.connection(), packet).orElseGet(() -> {
            LOGGER.warn("Stop requested for unknown Minecraft instance " + packet.serviceId());
            return null;
        });
    }

    private void handleServiceCommand(PacketContext context, ServiceCommandRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        instanceManager.command(context.connection(), packet);
    }

    private void handleServiceLogs(PacketContext context, ServiceLogsRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        instanceManager.logs(context.connection(), packet);
    }


    private void handleServiceCleanup(PacketContext context, ServiceCleanupRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        instanceManager.cleanup(context.connection(), packet);
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
