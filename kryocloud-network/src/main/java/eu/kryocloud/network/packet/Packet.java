package eu.kryocloud.network.packet;

import eu.kryocloud.network.connection.Connection;

public abstract class Packet {

    public abstract int getId();

    public abstract void write(PacketByteBuffer buf);

    public abstract void read(PacketByteBuffer buf);

    public void handle(Connection connection) {
        // optional Override
    }

}
