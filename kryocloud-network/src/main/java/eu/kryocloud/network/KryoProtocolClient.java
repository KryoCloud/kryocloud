package eu.kryocloud.network;

import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.channel.KryoChannelInitializer;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.connection.ProtocolSide;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.type.protocol.HandshakePacket;
import eu.kryocloud.network.protocol.PeerType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public final class KryoProtocolClient implements AutoCloseable {

    private static final KryoLogger LOGGER = KryoLogger.logger("Protocol");

    private final String host;
    private final int port;
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicReference<KryoConnection> connection = new AtomicReference<>();

    private volatile EventLoopGroup group;
    private volatile Channel channel;

    public KryoProtocolClient(String host, int port) {
        if (host == null) {
            throw new IllegalArgumentException("host must not be null");
        }

        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }

        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        this.host = host;
        this.port = port;
    }

    public void connect() throws InterruptedException {
        lifecycleLock.lock();

        try {
            if (connected.get()) {
                throw new IllegalStateException("KryoProtocolClient is already connected to " + host + ":" + port);
            }

            KryoPackets.registerDefaults();
            KryoProtocolHandlers.registerDefaults();

            EventLoopGroup newGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(newGroup).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true).handler(new KryoChannelInitializer(ProtocolSide.CLIENT, this::setConnection, this::clearConnection));

                ChannelFuture future = bootstrap.connect(host, port).sync();

                channel = future.channel();
                group = newGroup;
                connected.set(true);

                channel.closeFuture().addListener(ignored -> {
                    connected.set(false);
                    connection.set(null);
                    newGroup.shutdownGracefully();
                });

                LOGGER.success("Client connected to " + host + ":" + port);
            } catch (InterruptedException exception) {
                newGroup.shutdownGracefully().syncUninterruptibly();
                Thread.currentThread().interrupt();
                throw exception;
            } catch (RuntimeException exception) {
                newGroup.shutdownGracefully().syncUninterruptibly();
                throw exception;
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    public KryoConnection connect(PeerType peerType, String identity, String token) throws InterruptedException {
        return connect(peerType, identity, token, Duration.ofSeconds(10));
    }

    public KryoConnection connect(PeerType peerType, String identity, String token, Duration timeout) throws InterruptedException {
        if (peerType == null) {
            throw new IllegalArgumentException("peerType must not be null");
        }

        if (identity == null || identity.isBlank()) {
            throw new IllegalArgumentException("identity must not be blank");
        }

        if (timeout == null) {
            throw new IllegalArgumentException("timeout must not be null");
        }

        try {
            connect();
            send(new HandshakePacket(peerType, identity, token));
            return awaitAuthenticated(timeout);
        } catch (InterruptedException exception) {
            close();
            throw exception;
        } catch (RuntimeException exception) {
            close();
            throw exception;
        }
    }

    @Deprecated
    public KryoConnection connect(String token) throws InterruptedException {
        return connect(PeerType.WRAPPER, "legacy-wrapper", token);
    }

    public KryoConnection awaitAuthenticated(Duration timeout) throws InterruptedException {
        if (timeout == null) {
            throw new IllegalArgumentException("timeout must not be null");
        }

        long timeoutMillis = timeout.toMillis();

        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeout must be greater than 0ms");
        }

        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() <= deadline) {
            KryoConnection activeConnection = connection.get();

            if (activeConnection != null && activeConnection.isAuthenticated()) {
                return activeConnection;
            }

            Channel activeChannel = channel;

            if (activeChannel == null || !activeChannel.isActive()) {
                throw new IllegalStateException("KryoProtocolClient connection closed before authentication completed");
            }

            Thread.sleep(10L);
        }

        throw new IllegalStateException("Timed out while waiting for Kryo protocol authentication");
    }

    public boolean isConnected() {
        Channel activeChannel = channel;
        return connected.get() && activeChannel != null && activeChannel.isActive();
    }

    public KryoConnection connection() {
        KryoConnection activeConnection = connection.get();

        if (activeConnection == null || !activeConnection.isActive()) {
            throw new IllegalStateException("KryoProtocolClient is not connected");
        }

        return activeConnection;
    }

    public void send(Packet packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        KryoConnection activeConnection = connection.get();

        if (activeConnection != null && activeConnection.isActive()) {
            activeConnection.send(packet);
            return;
        }

        Channel activeChannel = channel;

        if (activeChannel != null && activeChannel.isActive()) {
            activeChannel.writeAndFlush(packet);
            return;
        }

        throw new IllegalStateException("KryoProtocolClient is not connected");
    }

    @Override
    public void close() {
        lifecycleLock.lock();

        try {
            if (!connected.getAndSet(false)) {
                return;
            }

            Channel currentChannel = channel;
            EventLoopGroup currentGroup = group;

            channel = null;
            group = null;
            connection.set(null);

            if (currentChannel != null) {
                currentChannel.close().syncUninterruptibly();
            }

            if (currentGroup != null) {
                currentGroup.shutdownGracefully().syncUninterruptibly();
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    private void setConnection(KryoConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }

        this.connection.set(connection);
    }

    private void clearConnection(KryoConnection connection) {
        if (connection == null) {
            return;
        }

        this.connection.compareAndSet(connection, null);
    }
}