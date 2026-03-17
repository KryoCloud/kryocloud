package eu.kryocloud.network.packet;

import eu.kryocloud.network.connection.Connection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

public class PacketHandler extends SimpleChannelInboundHandler<Packet> {

    public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().attr(CONNECTION).set(new Connection(ctx.channel()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        Connection connection = ctx.channel().attr(CONNECTION).get();

        if (connection == null) {
            connection = new Connection(ctx.channel());
            ctx.channel().attr(CONNECTION).set(connection);
        }

        packet.handle(connection);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
