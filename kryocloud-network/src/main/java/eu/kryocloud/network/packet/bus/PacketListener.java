package eu.kryocloud.network.packet.bus;

import eu.kryocloud.network.packet.Packet;

@FunctionalInterface
public interface PacketListener<T extends Packet> {

    void handle(PacketContext context, T packet);
}