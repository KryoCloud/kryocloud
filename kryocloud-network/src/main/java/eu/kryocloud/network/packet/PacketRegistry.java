package eu.kryocloud.network.packet;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.Map;
import java.util.function.Supplier;

public final class PacketRegistry {

    private static final Int2ObjectMap<Supplier<? extends Packet>> PACKETS = new Int2ObjectArrayMap<>();

    private PacketRegistry() {}

    public static synchronized <T extends Packet> void register(int id, Supplier<T> supplier) {
        if (PACKETS.containsKey(id)) {
           throw new IllegalStateException("Packet id already registered: " + id);
        }

        T instance = supplier.get();
        if (instance.getId() != id) {
            throw new IllegalStateException("Packet id mismatch for " + instance.getClass().getName() + ": register(" + id + "), but packet returns getId() = " + instance.getId());
        }

        PACKETS.put(id, supplier);
    }

    public static Packet create(int id) {
        Supplier<? extends Packet> supplier = PACKETS.get(id);
        if (supplier == null) {
           return null;
        }
        return supplier.get();
    }

    public static boolean isRegistered(int id) {
        return PACKETS.containsKey(id);
    }

}
