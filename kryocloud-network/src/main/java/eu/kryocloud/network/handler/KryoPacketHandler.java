package eu.kryocloud.network.handler;

import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.connection.ProtocolSide;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.bus.KryoPacketBus;
import eu.kryocloud.network.packet.type.AuthPacket;
import eu.kryocloud.network.packet.type.protocol.HandshakePacket;
import eu.kryocloud.network.packet.type.protocol.HandshakeResponsePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.util.function.Consumer;

public final class KryoPacketHandler extends SimpleChannelInboundHandler<Packet> {

    public static final AttributeKey<KryoConnection> CONNECTION = AttributeKey.valueOf("kryocloud.connection");
    private static final KryoLogger LOGGER = KryoLogger.logger("Protocol");

    private final ProtocolSide side;
    private final Consumer<KryoConnection> connectionOpened;
    private final Consumer<KryoConnection> connectionClosed;

    public KryoPacketHandler(Consumer<KryoConnection> connectionOpened, Consumer<KryoConnection> connectionClosed) {
        this(ProtocolSide.SERVER, connectionOpened, connectionClosed);
    }

    public KryoPacketHandler(ProtocolSide side, Consumer<KryoConnection> connectionOpened, Consumer<KryoConnection> connectionClosed) {
        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }

        if (connectionOpened == null) {
            throw new IllegalArgumentException("connectionOpened must not be null");
        }

        if (connectionClosed == null) {
            throw new IllegalArgumentException("connectionClosed must not be null");
        }

        this.side = side;
        this.connectionOpened = connectionOpened;
        this.connectionClosed = connectionClosed;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        KryoConnection connection = new KryoConnection(context.channel(), side);
        context.channel().attr(CONNECTION).set(connection);
        connectionOpened.accept(connection);
        LOGGER.debug("Connection opened: " + connection.remoteAddress() + " as " + side);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        if (context == null) {
            return;
        }

        KryoConnection connection = context.channel().attr(CONNECTION).getAndSet(null);

        if (connection == null) {
            return;
        }

        connection.close();
        connectionClosed.accept(connection);
        LOGGER.debug("Connection closed: " + connection.remoteAddress());
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

        if (!allowed(connection, packet)) {
            LOGGER.warn("Rejected unauthenticated " + packet.getClass().getSimpleName() + " from " + connection.remoteAddress() + " on " + side);
            context.close();
            return;
        }

        KryoPacketBus.dispatch(connection, packet);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        if (cause != null) {
            LOGGER.warn("Protocol exception: " + cause.getMessage());
        }

        if (context != null) {
            context.close();
        }
    }

    private boolean allowed(KryoConnection connection, Packet packet) {
        if (connection.isAuthenticated()) {
            return true;
        }

        if (side == ProtocolSide.SERVER) {
            return packet instanceof HandshakePacket || packet instanceof AuthPacket;
        }

        if (side == ProtocolSide.CLIENT) {
            return packet instanceof HandshakeResponsePacket;
        }

        return false;
    }
}
