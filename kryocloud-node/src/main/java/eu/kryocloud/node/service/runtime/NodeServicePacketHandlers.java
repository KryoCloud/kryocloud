package eu.kryocloud.node.service.runtime;

import eu.kryocloud.network.packet.bus.KryoPacketBus;
import eu.kryocloud.network.packet.bus.PacketContext;
import eu.kryocloud.network.packet.bus.PacketSubscription;
import eu.kryocloud.network.packet.type.service.ServiceStatePacket;
import eu.kryocloud.network.protocol.PeerType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NodeServicePacketHandlers implements AutoCloseable {

    private final NodeServiceRegistry serviceRegistry;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final List<PacketSubscription> subscriptions = new ArrayList<>();

    public NodeServicePacketHandlers(NodeServiceRegistry serviceRegistry) {
        if (serviceRegistry == null) {
            throw new IllegalArgumentException("serviceRegistry must not be null");
        }

        this.serviceRegistry = serviceRegistry;
    }

    public void register() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }

        subscriptions.add(KryoPacketBus.listen(ServiceStatePacket.class, this::handleServiceState));
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

    private void handleServiceState(PacketContext context, ServiceStatePacket packet) {
        if (!validWrapper(context)) {
            context.connection().close();
            return;
        }

        NodeServiceSnapshot snapshot = serviceRegistry.update(packet);
        context.printState("Service " + snapshot.serviceId() + " is now " + snapshot.state() + " on " + snapshot.wrapperId());
    }

    private boolean validWrapper(PacketContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        if (!context.authenticated()) {
            return false;
        }

        return context.connection().peerType() == PeerType.WRAPPER;
    }
}