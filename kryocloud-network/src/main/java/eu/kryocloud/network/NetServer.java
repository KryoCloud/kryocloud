package eu.kryocloud.network;

import eu.kryocloud.network.packet.PacketDecoder;
import eu.kryocloud.network.packet.PacketEncoder;
import eu.kryocloud.network.packet.PacketHandler;
import eu.kryocloud.network.packet.PacketRegistry;
import eu.kryocloud.network.packet.type.AuthPacket;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NetServer implements AutoCloseable {
    EventLoopGroup boss = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    EventLoopGroup worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    private final int port;

    public NetServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(boss, worker)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {

                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new PacketDecoder());
                        pipeline.addLast(new PacketEncoder());
                        pipeline.addLast(new PacketHandler());
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();
        System.out.println("NetServer is running on ::" + port);
        future.channel().closeFuture().sync();
    }

    private void registerPackets() {
        PacketRegistry.register(0x01, AuthPacket::new);
    }

    @Override
    public void close() {
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }
}
