package eu.kryocloud.network.stream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

public final class StreamFrameEncoder extends MessageToByteEncoder<StreamFrame> {

    @Override
    protected void encode(ChannelHandlerContext context, StreamFrame frame, ByteBuf output) {
        if (frame == null) {
            throw new IllegalArgumentException("frame must not be null");
        }

        byte[] channelBytes = frame.channel().getBytes(StandardCharsets.UTF_8);
        byte[] payload = frame.payload();

        if (channelBytes.length > StreamingProtocol.MAX_CHANNEL_BYTES) {
            throw new IllegalArgumentException("channel is too long: " + frame.channel());
        }

        if (payload.length > StreamingProtocol.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("payload is too large: " + payload.length);
        }

        output.writeInt(StreamingProtocol.MAGIC);
        output.writeByte(StreamingProtocol.VERSION);
        output.writeByte(frame.type().wireId());
        output.writeShort(frame.flags());
        output.writeLong(frame.streamId().getMostSignificantBits());
        output.writeLong(frame.streamId().getLeastSignificantBits());
        output.writeLong(frame.sequence());
        output.writeShort(channelBytes.length);
        output.writeBytes(channelBytes);
        output.writeInt(payload.length);
        output.writeBytes(payload);
    }
}