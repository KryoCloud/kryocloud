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
    private String javaVersion;
    private String bindAddress;
    private CloudServiceType serviceType;
    private int port;
    private int maxMemoryMb;
    private boolean staticService;
    private boolean onlineMode;
    private String forwardingMode;
    private String forwardingSecret;

    public ServiceStartRequestPacket() {
    }

    public ServiceStartRequestPacket(UUID requestId, String serviceId, String groupName, String templateName, String javaVersion, String bindAddress, CloudServiceType serviceType, int port, int maxMemoryMb, boolean staticService, boolean onlineMode, String forwardingMode, String forwardingSecret) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.serviceId = PacketValidation.nonBlankString(serviceId, "serviceId");
        this.groupName = PacketValidation.nonBlankString(groupName, "groupName");
        this.templateName = PacketValidation.nonBlankString(templateName, "templateName");
        this.javaVersion = PacketValidation.nonBlankString(javaVersion, "javaVersion");
        this.bindAddress = PacketValidation.nonBlankString(bindAddress, "bindAddress");
        this.serviceType = PacketValidation.value(serviceType, "serviceType");
        this.port = PacketValidation.port(port, "port");
        this.maxMemoryMb = PacketValidation.positiveInt(maxMemoryMb, "maxMemoryMb");
        this.staticService = staticService;
        this.onlineMode = onlineMode;
        this.forwardingMode = normalizeForwardingMode(forwardingMode);
        this.forwardingSecret = normalizeForwardingSecret(forwardingSecret);
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
        buffer.writeString(javaVersion);
        buffer.writeString(bindAddress);
        buffer.writeEnum(serviceType);
        buffer.writeInt(port);
        buffer.writeInt(maxMemoryMb);
        buffer.writeBoolean(staticService);
        buffer.writeBoolean(onlineMode);
        buffer.writeString(forwardingMode);
        buffer.writeString(forwardingSecret);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        serviceId = buffer.readString();
        groupName = buffer.readString();
        templateName = buffer.readString();
        javaVersion = buffer.readString();
        bindAddress = buffer.readString();
        serviceType = buffer.readEnum(CloudServiceType.class);
        port = buffer.readInt();
        maxMemoryMb = buffer.readInt();
        staticService = buffer.readBoolean();
        onlineMode = buffer.readBoolean();
        forwardingMode = normalizeForwardingMode(buffer.readString());
        forwardingSecret = normalizeForwardingSecret(buffer.readString());
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

    public String javaVersion() {
        return javaVersion;
    }

    public String bindAddress() {
        return bindAddress;
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

    public boolean onlineMode() {
        return onlineMode;
    }

    public String forwardingMode() {
        return forwardingMode;
    }

    public String forwardingSecret() {
        return forwardingSecret;
    }

    public boolean forwardingEnabled() {
        return !"NONE".equalsIgnoreCase(forwardingMode);
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
        PacketValidation.nonBlankString(serviceId, "serviceId");
        PacketValidation.nonBlankString(groupName, "groupName");
        PacketValidation.nonBlankString(templateName, "templateName");
        PacketValidation.nonBlankString(javaVersion, "javaVersion");
        PacketValidation.nonBlankString(bindAddress, "bindAddress");
        PacketValidation.value(serviceType, "serviceType");
        PacketValidation.port(port, "port");
        PacketValidation.positiveInt(maxMemoryMb, "maxMemoryMb");
        forwardingMode = normalizeForwardingMode(forwardingMode);
        forwardingSecret = normalizeForwardingSecret(forwardingSecret);
    }

    private String normalizeForwardingMode(String value) {
        if (value == null || value.isBlank()) {
            return "NONE";
        }

        return value.trim().toUpperCase();
    }

    private String normalizeForwardingSecret(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
