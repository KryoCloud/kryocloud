package eu.kryocloud.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) throws Exception {
        ByteBuf payloadBuf = ctx.alloc().buffer();
        try {
            PacketByteBuffer buffer = new PacketByteBuffer(payloadBuf);

            buffer.writeInt(packet.getId());
            packet.write(buffer);

            int length = payloadBuf.readableBytes();
            out.writeInt(length);
            out.writeBytes(payloadBuf);
        } finally {
            payloadBuf.release();
        }
    }
}
