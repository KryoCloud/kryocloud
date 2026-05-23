package eu.kryocloud.network.packet.type.service;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;
import eu.kryocloud.network.protocol.CloudServiceState;
import eu.kryocloud.network.protocol.CloudServiceType;

public final class ServiceStatePacket extends Packet {

    private String serviceId;
    private String groupName;
    private CloudServiceType serviceType;
    private CloudServiceState state;
    private String wrapperId;
    private String host;
    private int port;
    private long timestamp;
    private String message;

    public ServiceStatePacket() {
    }

    public ServiceStatePacket(String serviceId, String groupName, CloudServiceType serviceType, CloudServiceState state, String wrapperId, String host, int port, String message) {
        this(serviceId, groupName, serviceType, state, wrapperId, host, port, System.currentTimeMillis(), message);
    }

    public ServiceStatePacket(String serviceId, String groupName, CloudServiceType serviceType, CloudServiceState state, String wrapperId, String host, int port, long timestamp, String message) {
        this.serviceId = PacketValidation.nonBlankString(serviceId, "serviceId");
        this.groupName = PacketValidation.nonBlankString(groupName, "groupName");
        this.serviceType = PacketValidation.value(serviceType, "serviceType");
        this.state = PacketValidation.value(state, "state");
        this.wrapperId = PacketValidation.nonBlankString(wrapperId, "wrapperId");
        this.host = PacketValidation.nonBlankString(host, "host");
        this.port = PacketValidation.port(port, "port");
        this.timestamp = PacketValidation.positiveLong(timestamp, "timestamp");
        this.message = PacketValidation.string(message, "message");
    }

    @Override
    public int getId() {
        return KryoProtocol.SERVICE_STATE_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeString(serviceId);
        buffer.writeString(groupName);
        buffer.writeEnum(serviceType);
        buffer.writeEnum(state);
        buffer.writeString(wrapperId);
        buffer.writeString(host);
        buffer.writeInt(port);
        buffer.writeLong(timestamp);
        buffer.writeString(message);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        serviceId = buffer.readString();
        groupName = buffer.readString();
        serviceType = buffer.readEnum(CloudServiceType.class);
        state = buffer.readEnum(CloudServiceState.class);
        wrapperId = buffer.readString();
        host = buffer.readString();
        port = buffer.readInt();
        timestamp = buffer.readLong();
        message = buffer.readString();
    }

    public String serviceId() {
        return serviceId;
    }

    public String groupName() {
        return groupName;
    }

    public CloudServiceType serviceType() {
        return serviceType;
    }

    public CloudServiceState state() {
        return state;
    }

    public String wrapperId() {
        return wrapperId;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public long timestamp() {
        return timestamp;
    }

    public String message() {
        return message;
    }

    private void validateWritable() {
        PacketValidation.nonBlankString(serviceId, "serviceId");
        PacketValidation.nonBlankString(groupName, "groupName");
        PacketValidation.value(serviceType, "serviceType");
        PacketValidation.value(state, "state");
        PacketValidation.nonBlankString(wrapperId, "wrapperId");
        PacketValidation.nonBlankString(host, "host");
        PacketValidation.port(port, "port");
        PacketValidation.positiveLong(timestamp, "timestamp");
        PacketValidation.string(message, "message");
    }
}