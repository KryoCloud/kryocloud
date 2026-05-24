package eu.kryocloud.network.packet.type.service;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;

import java.util.UUID;

public final class ServiceCleanupRequestPacket extends Packet {

    private UUID requestId;
    private boolean dryRun;

    public ServiceCleanupRequestPacket() {
    }

    public ServiceCleanupRequestPacket(UUID requestId, boolean dryRun) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.dryRun = dryRun;
    }

    @Override
    public int getId() {
        return KryoProtocol.SERVICE_CLEANUP_REQUEST_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(requestId);
        buffer.writeBoolean(dryRun);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        dryRun = buffer.readBoolean();
    }

    public UUID requestId() {
        return requestId;
    }

    public boolean dryRun() {
        return dryRun;
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
    }
}
