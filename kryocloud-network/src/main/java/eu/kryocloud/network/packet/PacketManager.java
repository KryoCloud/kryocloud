package eu.kryocloud.network.packet;

import java.util.HashMap;
import java.util.Map;

public class PacketManager {

    private static final Map<Integer, Class<? extends Packet>> idToPacket = new HashMap<>();
    private static final Map<Class<? extends Packet>, Integer> packetToId = new HashMap<>();

    public static void register(int id, Class<? extends Packet> packetClass) {
        idToPacket.put(id, packetClass);
        packetToId.put(packetClass, id);
    }

    public static Packet create(int id) {
        Class<? extends Packet> clazz = idToPacket.get(id);
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getId(Packet packet) {
        return packetToId.get(packet.getClass());
    }
}