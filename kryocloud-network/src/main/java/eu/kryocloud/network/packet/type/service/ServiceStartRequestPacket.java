// kryocloud-network/src/main/java/eu/kryocloud/network/packet/type/service/ServiceStartRequestPacket.java
package eu.kryocloud.network.packet.type.service;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;
import eu.kryocloud.network.protocol.CloudServiceType;

import java.util.UUID;

public final class ServiceStartRequestPacket extends Packet {

    private UUID requestId;
    private String serviceId;
    private String groupName;
    private String templateName;
    private CloudServiceType serviceType;
    private int port;
    private int maxMemoryMb;
    private boolean staticService;

    public ServiceStartRequestPacket() {
    }

    public ServiceStartRequestPacket(UUID requestId, String serviceId, String groupName, String templateName, CloudServiceType serviceType, int port, int maxMemoryMb, boolean staticService) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.serviceId = PacketValidation.nonBlankString(serviceId, "serviceId");
        this.groupName = PacketValidation.nonBlankString(groupName, "groupName");
        this.templateName = PacketValidation.nonBlankString(templateName, "templateName");
        this.serviceType = PacketValidation.value(serviceType, "serviceType");
        this.port = PacketValidation.port(port, "port");
        this.maxMemoryMb = PacketValidation.positiveInt(maxMemoryMb, "maxMemoryMb");
        this.staticService = staticService;
    }

    @Override
    public int getId() {
        return KryoProtocol.SERVICE_START_REQUEST_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(requestId);
        buffer.writeString(serviceId);
        buffer.writeString(groupName);
        buffer.writeString(templateName);
        buffer.writeEnum(serviceType);
        buffer.writeInt(port);
        buffer.writeInt(maxMemoryMb);
        buffer.writeBoolean(staticService);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        serviceId = buffer.readString();
        groupName = buffer.readString();
        templateName = buffer.readString();
        serviceType = buffer.readEnum(CloudServiceType.class);
        port = buffer.readInt();
        maxMemoryMb = buffer.readInt();
        staticService = buffer.readBoolean();
    }

    public UUID requestId() {
        return requestId;
    }

    public String serviceId() {
        return serviceId;
    }

    public String groupName() {
        return groupName;
    }

    public String templateName() {
        return templateName;
    }

    public CloudServiceType serviceType() {
        return serviceType;
    }

    public int port() {
        return port;
    }

    public int maxMemoryMb() {
        return maxMemoryMb;
    }

    public boolean staticService() {
        return staticService;
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
        PacketValidation.nonBlankString(serviceId, "serviceId");
        PacketValidation.nonBlankString(groupName, "groupName");
        PacketValidation.nonBlankString(templateName, "templateName");
        PacketValidation.value(serviceType, "serviceType");
        PacketValidation.port(port, "port");
        PacketValidation.positiveInt(maxMemoryMb, "maxMemoryMb");
    }
}