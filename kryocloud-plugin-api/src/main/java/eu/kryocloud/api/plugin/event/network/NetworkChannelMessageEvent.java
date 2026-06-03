package eu.kryocloud.api.plugin.event.network;

import eu.kryocloud.api.plugin.event.ICloudEvent;
import eu.kryocloud.api.plugin.messaging.PluginChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

public record NetworkChannelMessageEvent(PluginChannel channel, byte[] payload, String sourcePlugin, String sourceService, String sourceGroup, String sourceWrapper, Instant receivedAt) implements ICloudEvent {

    public NetworkChannelMessageEvent {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }

        payload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
        sourcePlugin = sourcePlugin == null ? "" : sourcePlugin.trim();
        sourceService = sourceService == null ? "" : sourceService.trim();
        sourceGroup = sourceGroup == null ? "" : sourceGroup.trim();
        sourceWrapper = sourceWrapper == null ? "" : sourceWrapper.trim();
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }

    public String text() {
        return new String(payload, StandardCharsets.UTF_8);
    }

}
