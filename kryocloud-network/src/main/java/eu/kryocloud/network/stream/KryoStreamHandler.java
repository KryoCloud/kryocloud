package eu.kryocloud.network.stream;

import eu.kryocloud.common.logging.KryoLogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

public final class KryoStreamHandler extends SimpleChannelInboundHandler<StreamFrame> {

    public static final AttributeKey<KryoStreamConnection> CONNECTION = AttributeKey.valueOf("kryocloud.stream.connection");

    private static final KryoLogger LOGGER = KryoLogger.logger("Stream");

    private final KryoStreamListener listener;

    public KryoStreamHandler(KryoStreamListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }

        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        KryoStreamConnection connection = new KryoStreamConnection(context.channel());
        context.channel().attr(CONNECTION).set(connection);
        listener.connected(connection);
        LOGGER.debug("Stream connection opened: " + connection.remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        if (context == null) {
            return;
        }

        KryoStreamConnection connection = context.channel().attr(CONNECTION).getAndSet(null);

        if (connection == null) {
            return;
        }

        connection.close();
        listener.disconnected(connection);
        LOGGER.debug("Stream connection closed: " + connection.remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, StreamFrame frame) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        if (frame == null) {
            throw new IllegalArgumentException("frame must not be null");
        }

        KryoStreamConnection connection = context.channel().attr(CONNECTION).get();

        if (connection == null || !connection.active()) {
            context.close();
            return;
        }

        dispatch(connection, frame);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        if (cause != null) {
            LOGGER.warn("Stream exception: " + cause.getMessage());
        }

        if (context != null) {
            context.close();
        }
    }

    private void dispatch(KryoStreamConnection connection, StreamFrame frame) {
        if (frame.type() == StreamFrameType.HEARTBEAT) {
            return;
        }

        if (frame.type() == StreamFrameType.OPEN) {
            KryoStream stream = connection.acceptOpen(frame);
            listener.opened(connection, stream, frame.payload());
            return;
        }

        KryoStream stream = connection.acceptFrame(frame);

        if (frame.type() == StreamFrameType.DATA) {
            listener.received(connection, stream, frame.payload());
            return;
        }

        if (frame.type() == StreamFrameType.END) {
            listener.ended(connection, stream, frame.payload());
            return;
        }

        if (frame.type() == StreamFrameType.RESET) {
            listener.reset(connection, stream, frame.payloadAsString());
        }
    }
}