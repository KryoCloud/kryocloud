package eu.kryocloud.api.plugin.internal.transport.frame;

import java.util.Arrays;
import java.util.UUID;

public record StreamFrame(StreamFrameType type, UUID streamId, long sequence, String channel, int flags, byte[] payload) {

    public StreamFrame {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        streamId = streamId == null ? new UUID(0L, 0L) : streamId;
        channel = channel == null ? "" : channel;
        payload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
    }

    public static StreamFrame open(UUID streamId, String channel, byte[] payload) {
        return new StreamFrame(StreamFrameType.OPEN, streamId, 1L, channel, 0, payload);
    }

    public static StreamFrame end(UUID streamId, String channel, byte[] payload) {
        return new StreamFrame(StreamFrameType.END, streamId, 2L, channel, 1, payload);
    }

    public static StreamFrame reset(UUID streamId, String channel, String reason) {
        return new StreamFrame(StreamFrameType.RESET, streamId, 2L, channel, 1, reason == null ? new byte[0] : reason.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static StreamFrame heartbeat() {
        return new StreamFrame(StreamFrameType.HEARTBEAT, new UUID(0L, 0L), 0L, "", 0, new byte[0]);
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }

}
