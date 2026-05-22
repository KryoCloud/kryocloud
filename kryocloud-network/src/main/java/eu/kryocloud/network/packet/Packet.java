package eu.kryocloud.network.packet;

import eu.kryocloud.network.connection.KryoConnection;

public abstract class Packet {

    public abstract int getId();

    public abstract void write(PacketByteBuffer buffer);

    public abstract void read(PacketByteBuffer buffer);

    @Deprecated
    public void handle(KryoConnection connection) {
    }
}