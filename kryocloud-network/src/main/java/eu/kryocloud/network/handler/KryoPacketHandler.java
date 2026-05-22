package eu.kryocloud.network.handler;

import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.bus.KryoPacketBus;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.util.function.Consumer;

public final class KryoPacketHandler extends SimpleChannelInboundHandler<Packet> {

    public static final AttributeKey<KryoConnection> CONNECTION = AttributeKey.valueOf("kryocloud.connection");

    private final Consumer<KryoConnection> connectionOpened;
    private final Consumer<KryoConnection> connectionClosed;

    public KryoPacketHandler(Consumer<KryoConnection> connectionOpened, Consumer<KryoConnection> connectionClosed) {
        if (connectionOpened == null) {
            throw new IllegalArgumentException("connectionOpened must not be null");
        }

        if (connectionClosed == null) {
            throw new IllegalArgumentException("connectionClosed must not be null");
        }

        this.connectionOpened = connectionOpened;
        this.connectionClosed = connectionClosed;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        KryoConnection connection = new KryoConnection(context.channel());
        context.channel().attr(CONNECTION).set(connection);
        connectionOpened.accept(connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        if (context == null) {
            return;
        }

        KryoConnection connection = context.channel().attr(CONNECTION).getAndSet(null);

        if (connection != null) {
            connection.close();
            connectionClosed.accept(connection);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Packet packet) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        KryoConnection connection = context.channel().attr(CONNECTION).get();

        if (connection == null || !connection.isActive()) {
            context.close();
            return;
        }

        KryoPacketBus.dispatch(connection, packet);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        if (cause != null) {
            cause.printStackTrace();
        }

        if (context != null) {
            context.close();
        }
    }
}