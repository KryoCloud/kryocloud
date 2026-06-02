package eu.kryocloud.network.stream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public final class StreamFrameDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static final int MINIMUM_HEADER_BYTES = Integer.BYTES + 1 + 1 + Short.BYTES + Long.BYTES + Long.BYTES + Long.BYTES + Short.BYTES + Integer.BYTES;

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf input, List<Object> output) {
        if (input.readableBytes() < MINIMUM_HEADER_BYTES) {
            throw new IllegalStateException("Malformed stream frame: missing header");
        }

        int magic = input.readInt();

        if (magic != StreamingProtocol.MAGIC) {
            throw new IllegalStateException("Invalid stream frame magic: " + Integer.toHexString(magic));
        }

        int version = input.readUnsignedByte();

        if (!StreamingProtocol.compatible(version)) {
            throw new IllegalStateException("Unsupported stream protocol version: " + version);
        }

        StreamFrameType type = StreamFrameType.fromWireId(input.readUnsignedByte());
        int flags = input.readUnsignedShort();
        UUID streamId = new UUID(input.readLong(), input.readLong());
        long sequence = input.readLong();
        int channelLength = input.readUnsignedShort();

        if (channelLength > StreamingProtocol.MAX_CHANNEL_BYTES) {
            throw new IllegalStateException("Stream channel is too long: " + channelLength);
        }

        if (input.readableBytes() < channelLength + Integer.BYTES) {
            throw new IllegalStateException("Malformed stream frame: missing channel or payload length");
        }

        String channel = input.readCharSequence(channelLength, StandardCharsets.UTF_8).toString();
        int payloadLength = input.readInt();

        if (payloadLength < 0) {
            throw new IllegalStateException("payloadLength must not be negative");
        }

        if (payloadLength > StreamingProtocol.MAX_PAYLOAD_BYTES) {
            throw new IllegalStateException("payloadLength is too large: " + payloadLength);
        }

        if (input.readableBytes() != payloadLength) {
            throw new IllegalStateException("Malformed stream frame payload length. Expected " + payloadLength + " but had " + input.readableBytes());
        }

        byte[] payload = new byte[payloadLength];
        input.readBytes(payload);
        output.add(new StreamFrame(type, streamId, sequence, channel, flags, payload));
    }
}
