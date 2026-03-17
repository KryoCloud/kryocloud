package eu.kryocloud.network;

import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketDecoder;
import eu.kryocloud.network.packet.PacketEncoder;
import eu.kryocloud.network.packet.PacketHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NetClient {

    private final String host;
    private final int port;
    private Channel channel;

    public NetClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws Exception {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {

                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new PacketDecoder());
                        pipeline.addLast(new PacketEncoder());
                        pipeline.addLast(new PacketHandler());
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
        System.out.println("NetClient connected to " + host + ":" + port);
        channel.closeFuture().addListener(_ -> group.shutdownGracefully());
    }

    public void send(Packet packet) {
        if (channel == null) return;
        channel.writeAndFlush(packet);
    }
}