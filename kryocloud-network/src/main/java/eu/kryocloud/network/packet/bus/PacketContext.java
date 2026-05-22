package eu.kryocloud.network.packet.bus;

import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.Packet;
import io.netty.channel.ChannelFuture;

public record PacketContext(KryoConnection connection, long receivedAtMillis) {

    public PacketContext {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }

        if (receivedAtMillis < 1) {
            throw new IllegalArgumentException("receivedAtMillis must be greater than 0");
        }
    }

    public boolean authenticated() {
        return connection.isAuthenticated();
    }

    public ChannelFuture reply(Packet packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        return connection.send(packet);
    }

    public void printState(String message) {
        System.out.println(message);
    }
}