package eu.kryocloud.api.plugin.messaging;

import java.time.Instant;
import java.util.Arrays;

public record PluginMessage(PluginChannel channel, String source, byte[] payload, Instant receivedAt) {

    public PluginMessage {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }

        source = source == null ? "" : source.trim();
        payload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }

    public static PluginMessage of(PluginChannel channel, String source, byte[] payload) {
        return new PluginMessage(channel, source, payload, Instant.now());
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }

}
