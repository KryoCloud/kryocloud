package eu.kryocloud.network.packet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class PacketRegistry {

    private static final Map<Integer, PacketDefinition<? extends Packet>> PACKETS = new ConcurrentHashMap<>();

    private PacketRegistry() {
    }

    public static <T extends Packet> void register(int id, Class<T> type, Supplier<T> supplier) {
        validatePacketId(id);

        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        if (supplier == null) {
            throw new IllegalArgumentException("supplier must not be null");
        }

        T instance = supplier.get();

        if (instance == null) {
            throw new IllegalStateException("Packet supplier returned null for id " + id);
        }

        if (instance.getId() != id) {
            throw new IllegalStateException(
                    "Packet id mismatch for " + instance.getClass().getName() +
                            ": register(" + id + "), but packet returns getId() = " + instance.getId()
            );
        }

        PacketDefinition<T> definition = new PacketDefinition<>(id, type, supplier);
        PacketDefinition<? extends Packet> existing = PACKETS.putIfAbsent(id, definition);

        if (existing != null && existing.type() != type) {
            throw new IllegalStateException(
                    "Packet id " + id + " is already registered by " + existing.type().getName()
            );
        }
    }

    public static Packet createOrThrow(int id) {
        PacketDefinition<? extends Packet> definition = PACKETS.get(id);

        if (definition == null) {
            throw new IllegalStateException("Unknown packet id: " + id);
        }

        Packet packet = definition.supplier().get();

        if (packet == null) {
            throw new IllegalStateException("Packet supplier returned null for id " + id);
        }

        return packet;
    }

    public static boolean isRegistered(int id) {
        return PACKETS.containsKey(id);
    }

    public static int size() {
        return PACKETS.size();
    }

    public static Set<Integer> registeredIds() {
        return Set.copyOf(PACKETS.keySet());
    }

    static void clearForTests() {
        PACKETS.clear();
    }

    private static void validatePacketId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("Packet id must not be negative: " + id);
        }
    }

    private record PacketDefinition<T extends Packet>(
            int id,
            Class<T> type,
            Supplier<T> supplier
    ) {
        private PacketDefinition {
            if (type == null) {
                throw new IllegalArgumentException("type must not be null");
            }

            if (supplier == null) {
                throw new IllegalArgumentException("supplier must not be null");
            }
        }
    }
}