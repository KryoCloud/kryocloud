package eu.kryocloud.network.packet.type.protocol;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.protocol.HandshakeStatus;

public final class HandshakeResponsePacket extends Packet {

    private boolean accepted;
    private HandshakeStatus status;
    private int protocolVersion;
    private String remoteIdentity;

    public HandshakeResponsePacket() {
    }

    private HandshakeResponsePacket(
            boolean accepted,
            HandshakeStatus status,
            int protocolVersion,
            String remoteIdentity
    ) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (remoteIdentity == null) {
            throw new IllegalArgumentException("remoteIdentity must not be null");
        }

        if (remoteIdentity.isBlank()) {
            throw new IllegalArgumentException("remoteIdentity must not be blank");
        }

        this.accepted = accepted;
        this.status = status;
        this.protocolVersion = protocolVersion;
        this.remoteIdentity = remoteIdentity;
    }

    public static HandshakeResponsePacket accepted(String remoteIdentity) {
        return new HandshakeResponsePacket(true, HandshakeStatus.ACCEPTED, KryoProtocol.VERSION, remoteIdentity);
    }

    public static HandshakeResponsePacket rejected(HandshakeStatus status, String remoteIdentity) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (status == HandshakeStatus.ACCEPTED) {
            throw new IllegalArgumentException("rejected response cannot use ACCEPTED status");
        }

        return new HandshakeResponsePacket(false, status, KryoProtocol.VERSION, remoteIdentity);
    }

    @Override
    public int getId() {
        return KryoProtocol.HANDSHAKE_RESPONSE_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }

        if (status == null) {
            throw new IllegalStateException("status must not be null while writing handshake response packet");
        }

        if (remoteIdentity == null || remoteIdentity.isBlank()) {
            throw new IllegalStateException("remoteIdentity must not be blank while writing handshake response packet");
        }

        buffer.writeBoolean(accepted);
        buffer.writeEnum(status);
        buffer.writeInt(protocolVersion);
        buffer.writeString(remoteIdentity);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }

        accepted = buffer.readBoolean();
        status = buffer.readEnum(HandshakeStatus.class);
        protocolVersion = buffer.readInt();
        remoteIdentity = buffer.readString();
    }

    public boolean accepted() {
        return accepted;
    }

    public HandshakeStatus status() {
        return status;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public String remoteIdentity() {
        return remoteIdentity;
    }
}