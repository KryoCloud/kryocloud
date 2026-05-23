package eu.kryocloud.network.packet.type.service;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;

import java.util.UUID;

public final class ServiceStopRequestPacket extends Packet {

    private UUID requestId;
    private String serviceId;
    private boolean force;
    private String reason;

    public ServiceStopRequestPacket() {
    }

    public ServiceStopRequestPacket(UUID requestId, String serviceId, boolean force, String reason) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.serviceId = PacketValidation.nonBlankString(serviceId, "serviceId");
        this.force = force;
        this.reason = PacketValidation.string(reason, "reason");
    }

    @Override
    public int getId() {
        return KryoProtocol.SERVICE_STOP_REQUEST_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(requestId);
        buffer.writeString(serviceId);
        buffer.writeBoolean(force);
        buffer.writeString(reason);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        serviceId = buffer.readString();
        force = buffer.readBoolean();
        reason = buffer.readString();
    }

    public UUID requestId() {
        return requestId;
    }

    public String serviceId() {
        return serviceId;
    }

    public boolean force() {
        return force;
    }

    public String reason() {
        return reason;
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
        PacketValidation.nonBlankString(serviceId, "serviceId");
        PacketValidation.string(reason, "reason");
    }
}