package eu.kryocloud.network.connection;

import eu.kryocloud.network.packet.Packet;
import io.netty.channel.Channel;

public class Connection {

    private final Channel channel;

    public Connection(Channel channel) {
        this.channel = channel;
    }

    public void send(Packet packet) {
        channel.writeAndFlush(packet);
    }

    public Channel getChannel() {
        return channel;
    }
}