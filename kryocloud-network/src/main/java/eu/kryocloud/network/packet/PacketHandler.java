package eu.kryocloud.network.packet;

import eu.kryocloud.network.connection.Connection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PacketHandler extends SimpleChannelInboundHandler<Packet> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        Connection connection = new Connection(ctx.channel());
        packet.handle(connection);
    }
}
