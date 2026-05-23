package eu.kryocloud.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class PacketEncoder extends MessageToByteEncoder<Packet> {

    @Override
    protected void encode(ChannelHandlerContext context, Packet packet, ByteBuf output) {
        PacketByteBuffer buffer = new PacketByteBuffer(output);

        buffer.writeInt(packet.getId());
        packet.write(buffer);
    }
}