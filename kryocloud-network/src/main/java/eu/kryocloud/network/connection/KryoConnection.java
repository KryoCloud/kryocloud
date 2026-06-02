package eu.kryocloud.network.connection;

import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.protocol.PeerType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.SocketAddress;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class KryoConnection {

    private final UUID id = UUID.randomUUID();
    private final Channel channel;
    private final ProtocolSide side;
    private final Instant connectedAt = Instant.now();
    private final AtomicReference<ProtocolState> state = new AtomicReference<>(ProtocolState.HANDSHAKING);
    private final AtomicLong lastHeartbeatAt = new AtomicLong(System.currentTimeMillis());

    private volatile PeerType peerType;
    private volatile String identity;
    private volatile int protocolVersion = -1;

    public KryoConnection(Channel channel) {
        this(channel, ProtocolSide.SERVER);
    }

    public KryoConnection(Channel channel, ProtocolSide side) {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }

        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }

        this.channel = channel;
        this.side = side;
    }

    public UUID id() {
        return id;
    }

    public Channel channel() {
        return channel;
    }

    public ProtocolSide side() {
        return side;
    }

    public Instant connectedAt() {
        return connectedAt;
    }

    public ProtocolState state() {
        return state.get();
    }

    public PeerType peerType() {
        return peerType;
    }

    public String identity() {
        return identity;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public long lastHeartbeatAt() {
        return lastHeartbeatAt.get();
    }

    public boolean isActive() {
        return channel.isActive() && state.get() != ProtocolState.CLOSED;
    }

    public boolean isAuthenticated() {
        return state.get() == ProtocolState.AUTHENTICATED;
    }

    public boolean isTimedOut(long timeoutMillis) {
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeoutMillis must be greater than 0");
        }

        return System.currentTimeMillis() - lastHeartbeatAt.get() > timeoutMillis;
    }

    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    public ChannelFuture send(Packet packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        if (!isActive()) {
            throw new IllegalStateException("Connection is not active: " + id);
        }

        return channel.writeAndFlush(packet);
    }

    public void markAuthenticated(PeerType peerType, String identity, int protocolVersion) {
        if (peerType == null) {
            throw new IllegalArgumentException("peerType must not be null");
        }

        if (identity == null) {
            throw new IllegalArgumentException("identity must not be null");
        }

        if (identity.isBlank()) {
            throw new IllegalArgumentException("identity must not be blank");
        }

        this.peerType = peerType;
        this.identity = identity;
        this.protocolVersion = protocolVersion;
        this.lastHeartbeatAt.set(System.currentTimeMillis());
        this.state.set(ProtocolState.AUTHENTICATED);
    }

    @Deprecated
    public void markAuthenticated(String identity) {
        markAuthenticated(PeerType.WRAPPER, identity, -1);
    }

    public void markHeartbeat() {
        lastHeartbeatAt.set(System.currentTimeMillis());
    }

    public void close() {
        state.set(ProtocolState.CLOSED);

        if (channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public String toString() {
        return "KryoConnection{" +
                "id=" + id +
                ", side=" + side +
                ", state=" + state.get() +
                ", peerType=" + peerType +
                ", identity='" + identity + '\'' +
                ", protocolVersion=" + protocolVersion +
                ", remoteAddress=" + remoteAddress() +
                '}';
    }
}