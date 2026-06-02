package eu.kryocloud.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public final class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf input, List<Object> output) throws Exception {
        if (input.readableBytes() < Integer.BYTES) {
            throw new IllegalStateException("Malformed packet frame: missing packet id");
        }

        PacketByteBuffer buffer = new PacketByteBuffer(input);
        int packetId = buffer.readInt();
        Packet packet = PacketRegistry.createOrThrow(packetId);

        packet.read(buffer);

        if (buffer.readableBytes() != 0) {
            throw new IllegalStateException(
                    "Packet " + packet.getClass().getSimpleName() +
                            " has unread bytes left: " + buffer.readableBytes()
            );
        }

        output.add(packet);
    }
}
