package eu.kryocloud.network.packet.type.wrapper;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;
import eu.kryocloud.network.protocol.WrapperState;

public final class WrapperHeartbeatPacket extends Packet {

    private String wrapperId;
    private WrapperState state;
    private long timestamp;
    private long sequence;
    private int usedMemoryMb;
    private int maxMemoryMb;
    private int runningServices;

    public WrapperHeartbeatPacket() {
    }

    public WrapperHeartbeatPacket(String wrapperId, WrapperState state, long sequence, int usedMemoryMb, int maxMemoryMb, int runningServices) {
        this(wrapperId, state, System.currentTimeMillis(), sequence, usedMemoryMb, maxMemoryMb, runningServices);
    }

    public WrapperHeartbeatPacket(String wrapperId, WrapperState state, long timestamp, long sequence, int usedMemoryMb, int maxMemoryMb, int runningServices) {
        this.wrapperId = PacketValidation.nonBlankString(wrapperId, "wrapperId");
        this.state = PacketValidation.value(state, "state");
        this.timestamp = PacketValidation.positiveLong(timestamp, "timestamp");
        this.sequence = PacketValidation.nonNegativeLong(sequence, "sequence");
        this.usedMemoryMb = PacketValidation.nonNegativeInt(usedMemoryMb, "usedMemoryMb");
        this.maxMemoryMb = PacketValidation.positiveInt(maxMemoryMb, "maxMemoryMb");
        this.runningServices = PacketValidation.nonNegativeInt(runningServices, "runningServices");
    }

    @Override
    public int getId() {
        return KryoProtocol.WRAPPER_HEARTBEAT_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeString(wrapperId);
        buffer.writeEnum(state);
        buffer.writeLong(timestamp);
        buffer.writeLong(sequence);
        buffer.writeInt(usedMemoryMb);
        buffer.writeInt(maxMemoryMb);
        buffer.writeInt(runningServices);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        wrapperId = buffer.readString();
        state = buffer.readEnum(WrapperState.class);
        timestamp = buffer.readLong();
        sequence = buffer.readLong();
        usedMemoryMb = buffer.readInt();
        maxMemoryMb = buffer.readInt();
        runningServices = buffer.readInt();
    }

    public String wrapperId() {
        return wrapperId;
    }

    public WrapperState state() {
        return state;
    }

    public long timestamp() {
        return timestamp;
    }

    public long sequence() {
        return sequence;
    }

    public int usedMemoryMb() {
        return usedMemoryMb;
    }

    public int maxMemoryMb() {
        return maxMemoryMb;
    }

    public int runningServices() {
        return runningServices;
    }

    private void validateWritable() {
        PacketValidation.nonBlankString(wrapperId, "wrapperId");
        PacketValidation.value(state, "state");
        PacketValidation.positiveLong(timestamp, "timestamp");
        PacketValidation.nonNegativeLong(sequence, "sequence");
        PacketValidation.nonNegativeInt(usedMemoryMb, "usedMemoryMb");
        PacketValidation.positiveInt(maxMemoryMb, "maxMemoryMb");
        PacketValidation.nonNegativeInt(runningServices, "runningServices");
    }
}