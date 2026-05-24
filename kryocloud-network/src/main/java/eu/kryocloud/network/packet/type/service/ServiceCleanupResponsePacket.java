package eu.kryocloud.network.packet.type.service;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;

import java.util.UUID;

public final class ServiceCleanupResponsePacket extends Packet {

    private UUID requestId;
    private String wrapperId;
    private boolean dryRun;
    private int scanned;
    private int deleted;
    private int skipped;
    private int failed;
    private String details;

    public ServiceCleanupResponsePacket() {
    }

    public ServiceCleanupResponsePacket(UUID requestId, String wrapperId, boolean dryRun, int scanned, int deleted, int skipped, int failed, String details) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.wrapperId = PacketValidation.nonBlankString(wrapperId, "wrapperId");
        this.dryRun = dryRun;
        this.scanned = PacketValidation.nonNegativeInt(scanned, "scanned");
        this.deleted = PacketValidation.nonNegativeInt(deleted, "deleted");
        this.skipped = PacketValidation.nonNegativeInt(skipped, "skipped");
        this.failed = PacketValidation.nonNegativeInt(failed, "failed");
        this.details = PacketValidation.string(details, "details");
    }

    @Override
    public int getId() {
        return KryoProtocol.SERVICE_CLEANUP_RESPONSE_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(requestId);
        buffer.writeString(wrapperId);
        buffer.writeBoolean(dryRun);
        buffer.writeInt(scanned);
        buffer.writeInt(deleted);
        buffer.writeInt(skipped);
        buffer.writeInt(failed);
        buffer.writeString(details);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        wrapperId = buffer.readString();
        dryRun = buffer.readBoolean();
        scanned = buffer.readInt();
        deleted = buffer.readInt();
        skipped = buffer.readInt();
        failed = buffer.readInt();
        details = buffer.readString();
    }

    public UUID requestId() {
        return requestId;
    }

    public String wrapperId() {
        return wrapperId;
    }

    public boolean dryRun() {
        return dryRun;
    }

    public int scanned() {
        return scanned;
    }

    public int deleted() {
        return deleted;
    }

    public int skipped() {
        return skipped;
    }

    public int failed() {
        return failed;
    }

    public String details() {
        return details;
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
        PacketValidation.nonBlankString(wrapperId, "wrapperId");
        PacketValidation.nonNegativeInt(scanned, "scanned");
        PacketValidation.nonNegativeInt(deleted, "deleted");
        PacketValidation.nonNegativeInt(skipped, "skipped");
        PacketValidation.nonNegativeInt(failed, "failed");
        PacketValidation.string(details, "details");
    }
}
