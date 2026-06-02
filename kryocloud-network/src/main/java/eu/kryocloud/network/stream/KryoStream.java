package eu.kryocloud.network.stream;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class KryoStream {

    private final UUID streamId;
    private final String channel;
    private final Instant openedAt = Instant.now();
    private final AtomicLong outgoingSequence = new AtomicLong();
    private final AtomicLong incomingSequence = new AtomicLong();

    public KryoStream(UUID streamId, String channel) {
        if (streamId == null) {
            throw new IllegalArgumentException("streamId must not be null");
        }

        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel must not be blank");
        }

        this.streamId = streamId;
        this.channel = channel;
    }

    public UUID streamId() {
        return streamId;
    }

    public String channel() {
        return channel;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public long nextSequence() {
        return outgoingSequence.incrementAndGet();
    }

    public long lastIncomingSequence() {
        return incomingSequence.get();
    }

    public void acceptSequence(long sequence) {
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be greater than 0");
        }

        long current = incomingSequence.get();

        if (sequence <= current) {
            throw new IllegalStateException("Duplicate or stale stream sequence: " + sequence + " <= " + current);
        }

        incomingSequence.set(sequence);
    }
}
