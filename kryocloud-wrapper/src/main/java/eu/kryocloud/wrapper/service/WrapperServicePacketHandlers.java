package eu.kryocloud.wrapper.service;

import eu.kryocloud.network.packet.bus.KryoPacketBus;
import eu.kryocloud.network.packet.bus.PacketContext;
import eu.kryocloud.network.packet.bus.PacketSubscription;
import eu.kryocloud.network.packet.type.service.ServiceStartRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStopRequestPacket;
import eu.kryocloud.network.protocol.PeerType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WrapperServicePacketHandlers implements AutoCloseable {

    private final WrapperServiceManager serviceManager;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final List<PacketSubscription> subscriptions = new ArrayList<>();

    public WrapperServicePacketHandlers(WrapperServiceManager serviceManager) {
        if (serviceManager == null) {
            throw new IllegalArgumentException("serviceManager must not be null");
        }

        this.serviceManager = serviceManager;
    }

    public void register() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }

        subscriptions.add(KryoPacketBus.listen(ServiceStartRequestPacket.class, this::handleServiceStart));
        subscriptions.add(KryoPacketBus.listen(ServiceStopRequestPacket.class, this::handleServiceStop));
    }

    private void handleServiceStart(PacketContext context, ServiceStartRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        serviceManager.start(context.connection(), packet);
    }

    private void handleServiceStop(PacketContext context, ServiceStopRequestPacket packet) {
        if (!isNodeConnection(context)) {
            context.connection().close();
            return;
        }

        serviceManager.stop(context.connection(), packet).orElseGet(() -> {
            System.out.println("Stop requested for unknown service " + packet.serviceId());
            return null;
        });
    }

    private boolean isNodeConnection(PacketContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        return context.authenticated() && context.connection().peerType() == PeerType.NODE;
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