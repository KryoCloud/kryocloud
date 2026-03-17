package eu.kryocloud.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {

    private static final int MAX_PACKET_SIZE = 1024 * 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if(in.readableBytes() < 4) {
            return;
        }

        in.markReaderIndex();

        int frameLength = in.readInt();

        if (frameLength <= 0) {
            ctx.close();
            throw new IllegalStateException("Invalid frame length: " + frameLength);
        }

        if (frameLength > MAX_PACKET_SIZE) {
            ctx.close();
            throw new IllegalStateException("Frame length exceeds maximum allowed size: " + frameLength);
        }

        if (in.readableBytes() < frameLength) {
            in.resetReaderIndex();
            return;
        }

        ByteBuf frame = in.readSlice(frameLength);
        PacketByteBuffer buffer = new PacketByteBuffer(frame);

        int packetId = buffer.readInt();
        Packet packet = PacketRegistry.create(packetId);

        if (packet == null) {
            ctx.close();
            throw new IllegalStateException("Unknown packet id: " + packetId);
        }

        packet.read(buffer);

        if (buffer.readableBytes() != 0) {
            ctx.close();
            throw new IllegalStateException("Packet " + packet.getClass().getSimpleName() + " has unread bytes left: " + buffer.readableBytes());
        }

        out.add(packet);
    }
}
