package eu.kryocloud.network.packet.bus;

import eu.kryocloud.network.packet.Packet;

import java.util.concurrent.atomic.AtomicBoolean;

public final class PacketSubscription implements AutoCloseable {

    private final Class<? extends Packet> packetType;
    private final PacketListener<? extends Packet> listener;
    private final AtomicBoolean active = new AtomicBoolean(true);

    PacketSubscription(Class<? extends Packet> packetType, PacketListener<? extends Packet> listener) {
        if (packetType == null) {
            throw new IllegalArgumentException("packetType must not be null");
        }

        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }

        this.packetType = packetType;
        this.listener = listener;
    }

    public boolean active() {
        return active.get();
    }

    @Override
    public void close() {
        if (!active.compareAndSet(true, false)) {
            return;
        }

        KryoPacketBus.unregister(packetType, listener);
    }
}