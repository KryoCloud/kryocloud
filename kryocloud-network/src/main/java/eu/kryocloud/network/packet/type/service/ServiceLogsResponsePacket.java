package eu.kryocloud.network.packet.type.service;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;

import java.util.UUID;

public final class ServiceLogsResponsePacket extends Packet {

    private UUID requestId;
    private String serviceId;
    private boolean success;
    private String logs;
    private String message;

    public ServiceLogsResponsePacket() {
    }

    public ServiceLogsResponsePacket(UUID requestId, String serviceId, boolean success, String logs, String message) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.serviceId = PacketValidation.nonBlankString(serviceId, "serviceId");
        this.success = success;
        this.logs = PacketValidation.string(logs, "logs");
        this.message = PacketValidation.string(message, "message");
    }

    @Override
    public int getId() {
        return KryoProtocol.SERVICE_LOGS_RESPONSE_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(requestId);
        buffer.writeString(serviceId);
        buffer.writeBoolean(success);
        buffer.writeString(logs);
        buffer.writeString(message);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        serviceId = buffer.readString();
        success = buffer.readBoolean();
        logs = buffer.readString();
        message = buffer.readString();
    }

    public UUID requestId() {
        return requestId;
    }

    public String serviceId() {
        return serviceId;
    }

    public boolean success() {
        return success;
    }

    public String logs() {
        return logs;
    }

    public String message() {
        return message;
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
        PacketValidation.nonBlankString(serviceId, "serviceId");
        PacketValidation.string(logs, "logs");
        PacketValidation.string(message, "message");
    }
}
