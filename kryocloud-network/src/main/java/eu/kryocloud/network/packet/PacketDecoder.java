package eu.kryocloud.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public final class PacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf input, List<Object> output) throws Exception {
        if(input.readableBytes() < Integer.BYTES) {
            return;
        }

        PacketByteBuffer buffer = new PacketByteBuffer(input);
        int packetId = buffer.readInt();
        Packet packet = PacketRegistry.createOrThrow(packetId);

        packet.read(buffer);

        if(buffer.readableBytes() != 0) {
            throw new IllegalStateException(
                    "Packet " + packet.getClass().getSimpleName() +
                            " has unread bytes left: " + buffer.readableBytes()
            );
        }

        output.add(packet);
    }
}
