package eu.kryocloud.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) {
        int id = PacketManager.getId(packet);
        out.writeInt(id);
        packet.write(out);
    }
}
