package eu.kryocloud.network.packet.type.service;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;

import java.util.UUID;

public final class ServiceLogsRequestPacket extends Packet {

    private UUID requestId;
    private String serviceId;
    private int tailLines;

    public ServiceLogsRequestPacket() {
    }

    public ServiceLogsRequestPacket(UUID requestId, String serviceId, int tailLines) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.serviceId = PacketValidation.nonBlankString(serviceId, "serviceId");
        this.tailLines = PacketValidation.positiveInt(tailLines, "tailLines");
    }

    @Override
    public int getId() {
        return KryoProtocol.SERVICE_LOGS_REQUEST_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(requestId);
        buffer.writeString(serviceId);
        buffer.writeInt(tailLines);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        serviceId = buffer.readString();
        tailLines = buffer.readInt();
    }

    public UUID requestId() {
        return requestId;
    }

    public String serviceId() {
        return serviceId;
    }

    public int tailLines() {
        return tailLines;
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
        PacketValidation.nonBlankString(serviceId, "serviceId");
        PacketValidation.positiveInt(tailLines, "tailLines");
    }
}
