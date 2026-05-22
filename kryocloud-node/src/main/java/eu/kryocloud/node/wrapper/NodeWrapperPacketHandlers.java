package eu.kryocloud.node.wrapper;

import eu.kryocloud.network.packet.bus.KryoPacketBus;
import eu.kryocloud.network.packet.bus.PacketContext;
import eu.kryocloud.network.packet.bus.PacketSubscription;
import eu.kryocloud.network.packet.type.wrapper.WrapperHeartbeatPacket;
import eu.kryocloud.network.packet.type.wrapper.WrapperRegisterPacket;
import eu.kryocloud.network.protocol.PeerType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NodeWrapperPacketHandlers implements AutoCloseable {

    private final NodeWrapperRegistry wrapperRegistry;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final List<PacketSubscription> subscriptions = new ArrayList<>();

    public NodeWrapperPacketHandlers(NodeWrapperRegistry wrapperRegistry) {
        if (wrapperRegistry == null) {
            throw new IllegalArgumentException("wrapperRegistry must not be null");
        }

        this.wrapperRegistry = wrapperRegistry;
    }

    public void register() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }

        subscriptions.add(KryoPacketBus.listen(WrapperRegisterPacket.class, this::handleWrapperRegister));
        subscriptions.add(KryoPacketBus.listen(WrapperHeartbeatPacket.class, this::handleWrapperHeartbeat));
    }

    private void handleWrapperRegister(PacketContext context, WrapperRegisterPacket packet) {
        if (!isWrapperConnection(context)) {
            context.connection().close();
            return;
        }

        wrapperRegistry.register(context.connection(), packet);
    }

    private void handleWrapperHeartbeat(PacketContext context, WrapperHeartbeatPacket packet) {
        if (!isWrapperConnection(context)) {
            context.connection().close();
            return;
        }

        wrapperRegistry.heartbeat(context.connection(), packet)
                .orElseGet(() -> {
                    System.out.println("Heartbeat from unknown wrapper " + packet.wrapperId());
                    context.connection().close();
                    return null;
                });
    }

    private boolean isWrapperConnection(PacketContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        if (!context.authenticated()) {
            return false;
        }

        return context.connection().peerType() == PeerType.WRAPPER;
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