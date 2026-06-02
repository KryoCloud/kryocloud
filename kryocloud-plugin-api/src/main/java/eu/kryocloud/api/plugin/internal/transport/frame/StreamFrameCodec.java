package eu.kryocloud.api.plugin.internal.transport.frame;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class StreamFrameCodec {

    private static final int MAGIC = 0x4B535452;
    private static final int VERSION = 1;
    private static final int MAX_FRAME_SIZE = 8 * 1024 * 1024;
    private static final int MAX_PAYLOAD_BYTES = MAX_FRAME_SIZE - 128;
    private static final int MAX_CHANNEL_BYTES = 192;

    private StreamFrameCodec() {
    }

    public static void write(DataOutputStream output, StreamFrame frame) throws IOException {
        byte[] channelBytes = frame.channel().getBytes(StandardCharsets.UTF_8);
        byte[] payload = frame.payload();
        int bodyLength = Integer.BYTES + 1 + 1 + Short.BYTES + Long.BYTES + Long.BYTES + Long.BYTES + Short.BYTES + channelBytes.length + Integer.BYTES + payload.length;

        if (bodyLength > MAX_FRAME_SIZE) {
            throw new IllegalArgumentException("frame is too large: " + bodyLength);
        }

        if (channelBytes.length > MAX_CHANNEL_BYTES) {
            throw new IllegalArgumentException("channel is too long: " + frame.channel());
        }

        if (payload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("payload is too large: " + payload.length);
        }

        output.writeInt(bodyLength);
        output.writeInt(MAGIC);
        output.writeByte(VERSION);
        output.writeByte(frame.type().wireId());
        output.writeShort(frame.flags());
        output.writeLong(frame.streamId().getMostSignificantBits());
        output.writeLong(frame.streamId().getLeastSignificantBits());
        output.writeLong(frame.sequence());
        output.writeShort(channelBytes.length);
        output.write(channelBytes);
        output.writeInt(payload.length);
        output.write(payload);
        output.flush();
    }

    public static StreamFrame read(DataInputStream input) throws IOException {
        int length = input.readInt();

        if (length < 0 || length > MAX_FRAME_SIZE) {
            throw new IllegalStateException("Invalid stream frame length: " + length);
        }

        byte[] bytes = input.readNBytes(length);

        if (bytes.length != length) {
            throw new IllegalStateException("Unexpected end of stream frame");
        }

        DataInputStream frame = new DataInputStream(new java.io.ByteArrayInputStream(bytes));
        int magic = frame.readInt();

        if (magic != MAGIC) {
            throw new IllegalStateException("Invalid stream frame magic: " + Integer.toHexString(magic));
        }

        int version = frame.readUnsignedByte();

        if (version != VERSION) {
            throw new IllegalStateException("Unsupported stream protocol version: " + version);
        }

        StreamFrameType type = StreamFrameType.fromWireId(frame.readUnsignedByte());
        int flags = frame.readUnsignedShort();
        UUID streamId = new UUID(frame.readLong(), frame.readLong());
        long sequence = frame.readLong();
        int channelLength = frame.readUnsignedShort();

        if (channelLength > MAX_CHANNEL_BYTES) {
            throw new IllegalStateException("Stream channel is too long: " + channelLength);
        }

        byte[] channelBytes = frame.readNBytes(channelLength);

        if (channelBytes.length != channelLength) {
            throw new IllegalStateException("Unexpected end of stream channel");
        }

        String channel = new String(channelBytes, StandardCharsets.UTF_8);
        int payloadLength = frame.readInt();

        if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_BYTES) {
            throw new IllegalStateException("Invalid stream payload length: " + payloadLength);
        }

        byte[] payload = frame.readNBytes(payloadLength);

        if (payload.length != payloadLength) {
            throw new IllegalStateException("Unexpected end of stream payload");
        }

        return new StreamFrame(type, streamId, sequence, channel, flags, payload);
    }

}
