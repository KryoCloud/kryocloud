package eu.kryocloud.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) return;
        in.markReaderIndex();
        int id = in.readInt();
        Packet packet = PacketManager.create(id);
        if (packet == null) {
            return;
        }
        packet.read(in);
        out.add(packet);
    }
}
