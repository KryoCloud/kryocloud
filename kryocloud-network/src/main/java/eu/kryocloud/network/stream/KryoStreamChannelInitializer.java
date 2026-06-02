package eu.kryocloud.network.stream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public final class KryoStreamChannelInitializer extends ChannelInitializer<Channel> {

    private final KryoStreamListener listener;

    public KryoStreamChannelInitializer(KryoStreamListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }

        this.listener = listener;
    }

    @Override
    protected void initChannel(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }

        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("stream-frame-decoder", new LengthFieldBasedFrameDecoder(StreamingProtocol.MAX_FRAME_SIZE, 0, Integer.BYTES, 0, Integer.BYTES));
        pipeline.addLast("stream-decoder", new StreamFrameDecoder());
        pipeline.addLast("stream-frame-encoder", new LengthFieldPrepender(Integer.BYTES));
        pipeline.addLast("stream-encoder", new StreamFrameEncoder());
        pipeline.addLast("stream-handler", new KryoStreamHandler(listener));
    }
}