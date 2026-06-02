package eu.kryocloud.network.stream;

import java.util.Arrays;
import java.util.UUID;

public record StreamFrame(StreamFrameType type, UUID streamId, long sequence, String channel, int flags, byte[] payload) {

    public StreamFrame {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        if (streamId == null) {
            throw new IllegalArgumentException("streamId must not be null");
        }

        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }

        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }

        if (channel.isBlank() && type == StreamFrameType.OPEN) {
            throw new IllegalArgumentException("channel must not be blank for OPEN frames");
        }

        if (payload == null) {
            payload = new byte[0];
        }

        if (payload.length > StreamingProtocol.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("payload is too large: " + payload.length);
        }

        payload = Arrays.copyOf(payload, payload.length);
     }

    public static StreamFrame open(UUID streamId, long sequence, String channel, byte[] payload) {
        return new StreamFrame(StreamFrameType.OPEN, streamId, sequence, channel, StreamFrameFlags.NONE, payload);
    }

    public static StreamFrame data(UUID streamId, long sequence, String channel, byte[] payload) {
        return new StreamFrame(StreamFrameType.DATA, streamId, sequence, channel, StreamFrameFlags.NONE, payload);
    }

    public static StreamFrame end(UUID streamId, long sequence, String channel, byte[] payload) {
        return new StreamFrame(StreamFrameType.END, streamId, sequence, channel, StreamFrameFlags.FINAL, payload);
    }

    public static StreamFrame reset(UUID streamId, long sequence, String channel, String reason) {
        String message = reason == null ? "" : reason;
        return new StreamFrame(StreamFrameType.RESET, streamId, sequence, channel, StreamFrameFlags.FINAL, message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static StreamFrame heartbeat() {
        return new StreamFrame(StreamFrameType.HEARTBEAT, new UUID(0L, 0L), 0L, "", StreamFrameFlags.NONE, new byte[0]);
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }

    public String payloadAsString() {
        return new String(payload, java.nio.charset.StandardCharsets.UTF_8);
    }
}
