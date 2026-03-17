package eu.kryocloud.network.packet.impl;

import eu.kryocloud.network.auth.AuthManager;
import eu.kryocloud.network.connection.Connection;
import eu.kryocloud.network.packet.Packet;
import io.netty.buffer.ByteBuf;

public class AuthPacket extends Packet {

    private String token;

    public AuthPacket() {}

    public AuthPacket(String token) {
        this.token = token;
    }

    @Override
    public void write(ByteBuf buf) {
        byte[] data = token.getBytes();
        buf.writeInt(data.length);
        buf.writeBytes(data);
    }

    @Override
    public void read(ByteBuf buf) {
        int length = buf.readInt();
        byte[] data = new byte[length];
        buf.readBytes(data);
        token = new String(data);
    }

    @Override
    public void handle(Connection connection) {
        boolean valid = AuthManager.validate(token);
        if (!valid) {
            connection.getChannel().close();
            return;
        }
        System.out.println("Client authed: " + token);
    }

    public String getToken() {
        return token;
    }
}