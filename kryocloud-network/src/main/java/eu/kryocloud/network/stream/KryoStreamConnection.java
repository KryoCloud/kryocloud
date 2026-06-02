package eu.kryocloud.network.stream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.SocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class KryoStreamConnection {

    private final UUID id = UUID.randomUUID();
    private final Channel channel;
    private final Instant connectedAt = Instant.now();
    private final ConcurrentMap<UUID, KryoStream> streams = new ConcurrentHashMap<>();

    public KryoStreamConnection(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }

        this.channel = channel;
    }

    public UUID id() {
        return id;
    }

    public Instant connectedAt() {
        return connectedAt;
    }

    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    public boolean active() {
        return channel.isActive();
    }

    public Map<UUID, KryoStream> streams() {
        return Map.copyOf(streams);
    }

    public ChannelFuture open(String streamChannel, byte[] payload) {
        KryoStream stream = createStream(streamChannel);
        return send(StreamFrame.open(stream.streamId(), stream.nextSequence(), stream.channel(), payload));
    }

    public ChannelFuture data(UUID streamId, byte[] payload) {
        KryoStream stream = stream(streamId);
        return send(StreamFrame.data(stream.streamId(), stream.nextSequence(), stream.channel(), payload));
    }

    public ChannelFuture end(UUID streamId, byte[] payload) {
        KryoStream stream = stream(streamId);
        streams.remove(streamId);
        return send(StreamFrame.end(stream.streamId(), stream.nextSequence(), stream.channel(), payload));
    }

    public ChannelFuture reset(UUID streamId, String reason) {
        KryoStream stream = streams.remove(streamId);
        String channelName = stream == null ? "" : stream.channel();
        long sequence = stream == null ? 0L : stream.nextSequence();
        return send(StreamFrame.reset(streamId, sequence, channelName, reason));
    }

    public ChannelFuture heartbeat() {
        return send(StreamFrame.heartbeat());
    }

    public KryoStream acceptOpen(StreamFrame frame) {
        if (frame.type() != StreamFrameType.OPEN) {
            throw new IllegalArgumentException("frame must be OPEN");
        }

        KryoStream stream = new KryoStream(frame.streamId(), frame.channel());
        stream.acceptSequence(frame.sequence());

        KryoStream existing = streams.putIfAbsent(stream.streamId(), stream);

        if (existing != null) {
            throw new IllegalStateException("Stream already exists: " + stream.streamId());
        }

        return stream;
    }

    public KryoStream acceptFrame(StreamFrame frame) {
        if (frame.type() == StreamFrameType.OPEN) {
            return acceptOpen(frame);
        }

        KryoStream stream = streams.get(frame.streamId());

        if (stream == null) {
            throw new IllegalStateException("Unknown stream: " + frame.streamId());
        }

        stream.acceptSequence(frame.sequence());

        if (frame.type() == StreamFrameType.END || frame.type() == StreamFrameType.RESET) {
            streams.remove(frame.streamId());
        }

        return stream;
    }

    public void close() {
        streams.clear();

        if (!channel.isOpen()) {
            return;
        }

        channel.close();
    }

    private KryoStream createStream(String streamChannel) {
        if (streamChannel == null || streamChannel.isBlank()) {
            throw new IllegalArgumentException("streamChannel must not be blank");
        }

        KryoStream stream = new KryoStream(UUID.randomUUID(), streamChannel);
        streams.put(stream.streamId(), stream);
        return stream;
    }

    private KryoStream stream(UUID streamId) {
        if (streamId == null) {
            throw new IllegalArgumentException("streamId must not be null");
        }

        KryoStream stream = streams.get(streamId);

        if (stream == null) {
            throw new IllegalStateException("Unknown stream: " + streamId);
        }

        return stream;
    }

    private ChannelFuture send(StreamFrame frame) {
        if (!active()) {
            throw new IllegalStateException("Stream connection is not active: " + id);
        }

        return channel.writeAndFlush(frame);
    }
}