package eu.kryocloud.network.packet;

import eu.kryocloud.network.connection.Connection;
import io.netty.buffer.ByteBuf;

public abstract class Packet {

    public abstract void write(ByteBuf buf);
    public abstract void read(ByteBuf buf);

    public void handle(Connection connection) {

    }
}
