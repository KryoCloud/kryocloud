package eu.kryocloud.network.packet.type.protocol;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;

public final class HeartbeatPacket extends Packet {

    private long timestamp;
    private long sequence;

    public HeartbeatPacket() {
    }

    public HeartbeatPacket(long sequence) {
        this(System.currentTimeMillis(), sequence);
    }

    public HeartbeatPacket(long timestamp, long sequence) {
        if (timestamp < 1) {
            throw new IllegalArgumentException("timestamp must be greater than 0");
        }

        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }

        this.timestamp = timestamp;
        this.sequence = sequence;
    }

    @Override
    public int getId() {
        return KryoProtocol.HEARTBEAT_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }

        buffer.writeLong(timestamp);
        buffer.writeLong(sequence);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }

        timestamp = buffer.readLong();
        sequence = buffer.readLong();
    }

    public long timestamp() {
        return timestamp;
    }

    public long sequence() {
        return sequence;
    }
}