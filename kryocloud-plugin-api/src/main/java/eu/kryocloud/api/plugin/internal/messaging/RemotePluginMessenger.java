package eu.kryocloud.api.plugin.internal.messaging;

import eu.kryocloud.api.plugin.event.EventPriority;
import eu.kryocloud.api.plugin.event.IEventSubscription;
import eu.kryocloud.api.plugin.event.remote.RemoteCloudEvent;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.event.RemoteEventBus;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import eu.kryocloud.api.plugin.messaging.IPluginMessageListener;
import eu.kryocloud.api.plugin.messaging.IPluginMessenger;
import eu.kryocloud.api.plugin.messaging.PluginChannel;
import eu.kryocloud.api.plugin.messaging.PluginMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RemotePluginMessenger implements IPluginMessenger {

    private final PluginRequestClient client;
    private final RemoteEventBus events;

    public RemotePluginMessenger(PluginRequestClient client, RemoteEventBus events) {
        this.client = client;
        this.events = events;
    }

    @Override
    public CompletableFuture<Void> send(PluginChannel channel, byte[] payload) {
        return client.message("plugin.message", Payload.create()
                .put("channel", channel.key())
                .put("payload", Base64.getEncoder().encodeToString(payload == null ? new byte[0] : payload))
                .map()).thenApply(message -> null);
    }

    @Override
    public AutoCloseable listen(PluginChannel channel, IPluginMessageListener listener) {
        IEventSubscription subscription = events.listen(RemoteCloudEvent.class, EventPriority.NORMAL, event -> {
            if (!event.name().equals("plugin.message")) {
                return;
            }

            if (!event.payload().getOrDefault("channel", "").equals(channel.key())) {
                return;
            }

            byte[] payload = Base64.getDecoder().decode(event.payload().getOrDefault("payload", ""));
            String source = event.payload().getOrDefault("source", "");
            listener.handle(new PluginMessage(channel, source, payload, Instant.now()));
        });

        client.subscribe("plugin.message." + channel.key());
        return subscription;
    }

    public CompletableFuture<Void> sendText(PluginChannel channel, String message) {
        return send(channel, (message == null ? "" : message).getBytes(StandardCharsets.UTF_8));
    }

}
