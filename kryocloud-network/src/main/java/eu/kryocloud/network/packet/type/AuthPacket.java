package eu.kryocloud.network.packet.type;

import eu.kryocloud.network.auth.AuthManager;
import eu.kryocloud.network.connection.Connection;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;

public class AuthPacket extends Packet {

    private String token;

    public AuthPacket() {}

    public AuthPacket(String token) {
        this.token = token;
    }

    @Override
    public int getId() {
        return 0x01;
    }

    @Override
    public void write(PacketByteBuffer buf) {
        buf.writeString(token);
    }

    @Override
    public void read(PacketByteBuffer buf) {
        this.token = buf.readString();
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
