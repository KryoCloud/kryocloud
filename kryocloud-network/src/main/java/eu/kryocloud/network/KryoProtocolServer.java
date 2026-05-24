package eu.kryocloud.network;

import eu.kryocloud.network.channel.KryoChannelInitializer;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.Packet;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public final class KryoProtocolServer implements AutoCloseable {

    private final String host;
    private final int port;
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<KryoConnection> connections = ConcurrentHashMap.newKeySet();

    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel serverChannel;

    public KryoProtocolServer(int port) {
        this("0.0.0.0", port);
    }

    public KryoProtocolServer(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }

        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        this.host = host;
        this.port = port;
    }

    public void start() throws InterruptedException {
        lifecycleLock.lock();

        try {
            if (running.get()) {
                throw new IllegalStateException("KryoProtocolServer is already running on " + host + ":" + port);
            }

            KryoPackets.registerDefaults();
            KryoProtocolHandlers.registerDefaults();

            EventLoopGroup newBossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            EventLoopGroup newWorkerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(newBossGroup, newWorkerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childHandler(new KryoChannelInitializer(this::registerConnection, this::unregisterConnection));

                ChannelFuture future = bootstrap.bind(new InetSocketAddress(host, port)).sync();

                bossGroup = newBossGroup;
                workerGroup = newWorkerGroup;
                serverChannel = future.channel();
                running.set(true);

                System.out.println("KryoProtocolServer listening on " + host + ":" + port);
            } catch (InterruptedException exception) {
                shutdownGroups(newBossGroup, newWorkerGroup);
                Thread.currentThread().interrupt();
                throw exception;
            } catch (RuntimeException exception) {
                shutdownGroups(newBossGroup, newWorkerGroup);
                throw exception;
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public boolean isRunning() {
        Channel channel = serverChannel;
        return running.get() && channel != null && channel.isOpen();
    }

    public int activeConnectionCount() {
        return connections.size();
    }

    public Set<KryoConnection> connections() {
        return Set.copyOf(connections);
    }

    public void broadcast(Packet packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        for (KryoConnection connection : connections) {
            if (connection.isActive()) {
                connection.send(packet);
            }
        }
    }

    private void registerConnection(KryoConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }

        connections.add(connection);
    }

    private void unregisterConnection(KryoConnection connection) {
        if (connection == null) {
            return;
        }

        connections.remove(connection);
    }

    @Override
    public void close() {
        lifecycleLock.lock();

        try {
            if (!running.getAndSet(false)) {
                return;
            }

            Channel channel = serverChannel;
            EventLoopGroup boss = bossGroup;
            EventLoopGroup worker = workerGroup;

            serverChannel = null;
            bossGroup = null;
            workerGroup = null;

            for (KryoConnection connection : connections) {
                connection.close();
            }

            connections.clear();

            if (channel != null) {
                channel.close().syncUninterruptibly();
            }

            shutdownGroups(boss, worker);
        } finally {
            lifecycleLock.unlock();
        }
    }

    private void shutdownGroups(EventLoopGroup boss, EventLoopGroup worker) {
        if (boss != null) {
            boss.shutdownGracefully().syncUninterruptibly();
        }

        if (worker != null) {
            worker.shutdownGracefully().syncUninterruptibly();
        }
    }
}
