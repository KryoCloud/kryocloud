package eu.kryocloud.network.channel;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.handler.KryoPacketHandler;
import eu.kryocloud.network.packet.PacketDecoder;
import eu.kryocloud.network.packet.PacketEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.util.function.Consumer;

public final class KryoChannelInitializer extends ChannelInitializer<Channel> {

    private final Consumer<KryoConnection> connectionOpened;
    private final Consumer<KryoConnection> connectionClosed;

    public KryoChannelInitializer(Consumer<KryoConnection> connectionOpened, Consumer<KryoConnection> connectionClosed) {
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
    protected void initChannel(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }

        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("frame-decoder", new LengthFieldBasedFrameDecoder(
                KryoProtocol.MAX_FRAME_SIZE,
                0,
                Integer.BYTES,
                0,
                Integer.BYTES
        ));
        pipeline.addLast("packet-decoder", new PacketDecoder());

        pipeline.addLast("frame-encoder", new LengthFieldPrepender(Integer.BYTES));
        pipeline.addLast("packet-encoder", new PacketEncoder());

        pipeline.addLast("packet-handler", new KryoPacketHandler(connectionOpened, connectionClosed));
    }
}