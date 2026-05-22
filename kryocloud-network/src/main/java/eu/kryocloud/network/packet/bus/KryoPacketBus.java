package eu.kryocloud.network.packet.bus;

import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.Packet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KryoPacketBus {

    private static final KryoLogger LOGGER = KryoLogger.logger("PacketBus");
    private static final Map<Class<? extends Packet>, CopyOnWriteArrayList<RegisteredPacketListener<? extends Packet>>> LISTENERS = new ConcurrentHashMap<>();

    private KryoPacketBus() {
    }

    public static <T extends Packet> PacketSubscription listen(Class<T> packetType, PacketListener<T> listener) {
        if (packetType == null) {
            throw new IllegalArgumentException("packetType must not be null");
        }

        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }

        RegisteredPacketListener<T> registeredListener = new RegisteredPacketListener<>(listener);
        LISTENERS.computeIfAbsent(packetType, ignored -> new CopyOnWriteArrayList<>()).add(registeredListener);
        return new PacketSubscription(packetType, listener);
    }

    public static void dispatch(KryoConnection connection, Packet packet) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        PacketContext context = new PacketContext(connection, System.currentTimeMillis());
        dispatchTo(Packet.class, context, packet);

        if (packet.getClass() != Packet.class) {
            dispatchTo(packet.getClass().asSubclass(Packet.class), context, packet);
        }
    }

    public static int listenerCount(Class<? extends Packet> packetType) {
        if (packetType == null) {
            throw new IllegalArgumentException("packetType must not be null");
        }

        List<RegisteredPacketListener<? extends Packet>> listeners = LISTENERS.get(packetType);
        return listeners == null ? 0 : listeners.size();
    }

    public static Set<Class<? extends Packet>> listenedPacketTypes() {
        return Set.copyOf(LISTENERS.keySet());
    }

    static void unregister(Class<? extends Packet> packetType, PacketListener<? extends Packet> listener) {
        CopyOnWriteArrayList<RegisteredPacketListener<? extends Packet>> listeners = LISTENERS.get(packetType);

        if (listeners == null) {
            return;
        }

        listeners.removeIf(registeredListener -> registeredListener.sameListener(listener));

        if (listeners.isEmpty()) {
            LISTENERS.remove(packetType, listeners);
        }
    }

    static void clearForTests() {
        LISTENERS.clear();
    }

    private static void dispatchTo(Class<? extends Packet> packetType, PacketContext context, Packet packet) {
        CopyOnWriteArrayList<RegisteredPacketListener<? extends Packet>> listeners = LISTENERS.get(packetType);

        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        for (RegisteredPacketListener<? extends Packet> listener : listeners) {
            try {
                listener.invoke(context, packet);
            } catch (Exception exception) {
                LOGGER.error("Packet listener failed for " + packet.getClass().getSimpleName() + " from " + context.connection().remoteAddress(), exception);
            }
        }
    }

    private record RegisteredPacketListener<T extends Packet>(PacketListener<T> listener) {

        private RegisteredPacketListener {
            if (listener == null) {
                throw new IllegalArgumentException("listener must not be null");
            }
        }

        private boolean sameListener(PacketListener<? extends Packet> other) {
            return listener == other;
        }

        @SuppressWarnings("unchecked")
        private void invoke(PacketContext context, Packet packet) {
            listener.handle(context, (T) packet);
        }
    }
}