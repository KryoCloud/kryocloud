package eu.kryocloud.network.packet.type.service;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;

import java.util.UUID;

public final class ServiceCommandRequestPacket extends Packet {

    private UUID requestId;
    private String serviceId;
    private String command;

    public ServiceCommandRequestPacket() {
    }

    public ServiceCommandRequestPacket(UUID requestId, String serviceId, String command) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.serviceId = PacketValidation.nonBlankString(serviceId, "serviceId");
        this.command = PacketValidation.nonBlankString(command, "command");
    }

    @Override
    public int getId() {
        return KryoProtocol.SERVICE_COMMAND_REQUEST_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(requestId);
        buffer.writeString(serviceId);
        buffer.writeString(command);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        serviceId = buffer.readString();
        command = buffer.readString();
    }

    public UUID requestId() {
        return requestId;
    }

    public String serviceId() {
        return serviceId;
    }

    public String command() {
        return command;
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
        PacketValidation.nonBlankString(serviceId, "serviceId");
        PacketValidation.nonBlankString(command, "command");
    }
}
