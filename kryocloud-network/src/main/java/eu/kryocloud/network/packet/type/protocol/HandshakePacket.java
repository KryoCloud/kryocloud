package eu.kryocloud.network.packet.type.protocol;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.protocol.PeerType;

public final class HandshakePacket extends Packet {

    private int protocolVersion;
    private PeerType peerType;
    private String identity;
    private String token;

    public HandshakePacket() {
    }

    public HandshakePacket(PeerType peerType, String identity, String token) {
        this(KryoProtocol.VERSION, peerType, identity, token);
    }

    public HandshakePacket(int protocolVersion, PeerType peerType, String identity, String token) {
        if (peerType == null) {
            throw new IllegalArgumentException("peerType must not be null");
        }

        if (identity == null) {
            throw new IllegalArgumentException("identity must not be null");
        }

        if (identity.isBlank()) {
            throw new IllegalArgumentException("identity must not be blank");
        }

        this.protocolVersion = protocolVersion;
        this.peerType = peerType;
        this.identity = identity;
        this.token = token;
    }

    @Override
    public int getId() {
        return KryoProtocol.HANDSHAKE_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }

        if (peerType == null) {
            throw new IllegalStateException("peerType must not be null while writing handshake packet");
        }

        if (identity == null || identity.isBlank()) {
            throw new IllegalStateException("identity must not be blank while writing handshake packet");
        }

        buffer.writeInt(protocolVersion);
        buffer.writeEnum(peerType);
        buffer.writeString(identity);
        buffer.writeString(token);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }

        protocolVersion = buffer.readInt();
        peerType = buffer.readEnum(PeerType.class);
        identity = buffer.readString();
        token = buffer.readString();
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public PeerType peerType() {
        return peerType;
    }

    public String identity() {
        return identity;
    }

    public String token() {
        return token;
    }
}