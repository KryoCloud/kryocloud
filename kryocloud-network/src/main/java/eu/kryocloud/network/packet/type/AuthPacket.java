package eu.kryocloud.network.packet.type;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;

@Deprecated
public final class AuthPacket extends Packet {

    private String token;

    public AuthPacket() {
    }

    public AuthPacket(String token) {
        this.token = token;
    }

    @Override
    public int getId() {
        return KryoProtocol.LEGACY_AUTH_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }

        buffer.writeString(token);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }

        token = buffer.readString();
    }

    public String token() {
        return token;
    }
}