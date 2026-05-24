package eu.kryocloud.network.packet.type.service;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;

public final class ServiceMetricsPacket extends Packet {

    private String serviceId;
    private String wrapperId;
    private long timestamp;
    private int processMemoryMb;
    private int cpuLoadPermille;
    private long uptimeMillis;

    public ServiceMetricsPacket() {
    }

    public ServiceMetricsPacket(String serviceId, String wrapperId, int processMemoryMb, int cpuLoadPermille, long uptimeMillis) {
        this(serviceId, wrapperId, System.currentTimeMillis(), processMemoryMb, cpuLoadPermille, uptimeMillis);
    }

    public ServiceMetricsPacket(String serviceId, String wrapperId, long timestamp, int processMemoryMb, int cpuLoadPermille, long uptimeMillis) {
        this.serviceId = PacketValidation.nonBlankString(serviceId, "serviceId");
        this.wrapperId = PacketValidation.nonBlankString(wrapperId, "wrapperId");
        this.timestamp = PacketValidation.positiveLong(timestamp, "timestamp");
        this.processMemoryMb = PacketValidation.nonNegativeInt(processMemoryMb, "processMemoryMb");
        this.cpuLoadPermille = PacketValidation.nonNegativeInt(cpuLoadPermille, "cpuLoadPermille");
        this.uptimeMillis = PacketValidation.nonNegativeLong(uptimeMillis, "uptimeMillis");
    }

    @Override
    public int getId() {
        return KryoProtocol.SERVICE_METRICS_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeString(serviceId);
        buffer.writeString(wrapperId);
        buffer.writeLong(timestamp);
        buffer.writeInt(processMemoryMb);
        buffer.writeInt(cpuLoadPermille);
        buffer.writeLong(uptimeMillis);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        serviceId = buffer.readString();
        wrapperId = buffer.readString();
        timestamp = buffer.readLong();
        processMemoryMb = buffer.readInt();
        cpuLoadPermille = buffer.readInt();
        uptimeMillis = buffer.readLong();
    }

    public String serviceId() {
        return serviceId;
    }

    public String wrapperId() {
        return wrapperId;
    }

    public long timestamp() {
        return timestamp;
    }

    public int processMemoryMb() {
        return processMemoryMb;
    }

    public int cpuLoadPermille() {
        return cpuLoadPermille;
    }

    public long uptimeMillis() {
        return uptimeMillis;
    }

    private void validateWritable() {
        PacketValidation.nonBlankString(serviceId, "serviceId");
        PacketValidation.nonBlankString(wrapperId, "wrapperId");
        PacketValidation.positiveLong(timestamp, "timestamp");
        PacketValidation.nonNegativeInt(processMemoryMb, "processMemoryMb");
        PacketValidation.nonNegativeInt(cpuLoadPermille, "cpuLoadPermille");
        PacketValidation.nonNegativeLong(uptimeMillis, "uptimeMillis");
    }
}
