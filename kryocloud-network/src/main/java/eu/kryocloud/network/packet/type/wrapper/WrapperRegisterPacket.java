package eu.kryocloud.network.packet.type.wrapper;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;

public final class WrapperRegisterPacket extends Packet {

    private String wrapperId;
    private String hostname;
    private String address;
    private String osName;
    private int availableProcessors;
    private int maxMemoryMb;

    public WrapperRegisterPacket() {
    }

    public WrapperRegisterPacket(String wrapperId, String hostname, String address, String osName, int availableProcessors, int maxMemoryMb) {
        this.wrapperId = PacketValidation.nonBlankString(wrapperId, "wrapperId");
        this.hostname = PacketValidation.nonBlankString(hostname, "hostname");
        this.address = PacketValidation.nonBlankString(address, "address");
        this.osName = PacketValidation.nonBlankString(osName, "osName");
        this.availableProcessors = PacketValidation.positiveInt(availableProcessors, "availableProcessors");
        this.maxMemoryMb = PacketValidation.positiveInt(maxMemoryMb, "maxMemoryMb");
    }

    @Override
    public int getId() {
        return KryoProtocol.WRAPPER_REGISTER_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeString(wrapperId);
        buffer.writeString(hostname);
        buffer.writeString(address);
        buffer.writeString(osName);
        buffer.writeInt(availableProcessors);
        buffer.writeInt(maxMemoryMb);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        wrapperId = buffer.readString();
        hostname = buffer.readString();
        address = buffer.readString();
        osName = buffer.readString();
        availableProcessors = buffer.readInt();
        maxMemoryMb = buffer.readInt();
    }

    public String wrapperId() {
        return wrapperId;
    }

    public String hostname() {
        return hostname;
    }

    public String address() {
        return address;
    }

    public String osName() {
        return osName;
    }

    public int availableProcessors() {
        return availableProcessors;
    }

    public int maxMemoryMb() {
        return maxMemoryMb;
    }

    private void validateWritable() {
        PacketValidation.nonBlankString(wrapperId, "wrapperId");
        PacketValidation.nonBlankString(hostname, "hostname");
        PacketValidation.nonBlankString(address, "address");
        PacketValidation.nonBlankString(osName, "osName");
        PacketValidation.positiveInt(availableProcessors, "availableProcessors");
        PacketValidation.positiveInt(maxMemoryMb, "maxMemoryMb");
    }
}