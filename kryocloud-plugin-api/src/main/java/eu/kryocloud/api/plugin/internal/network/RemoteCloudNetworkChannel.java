package eu.kryocloud.api.plugin.internal.network;

import eu.kryocloud.api.plugin.event.EventPriority;
import eu.kryocloud.api.plugin.event.IEventListener;
import eu.kryocloud.api.plugin.event.network.NetworkChannelMessageEvent;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.event.RemoteEventBus;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import eu.kryocloud.api.plugin.messaging.PluginChannel;
import eu.kryocloud.api.plugin.network.ICloudNetworkChannel;
import eu.kryocloud.api.plugin.network.NetworkMessageTarget;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public final class RemoteCloudNetworkChannel implements ICloudNetworkChannel {

    private final PluginChannel channel;
    private final PluginRequestClient client;
    private final RemoteEventBus events;

    public RemoteCloudNetworkChannel(PluginChannel channel, PluginRequestClient client, RemoteEventBus events) {
        this.channel = channel;
        this.client = client;
        this.events = events;
    }

    @Override
    public PluginChannel channel() {
        return channel;
    }

    @Override
    public CompletableFuture<Void> publish(byte[] payload) {
        return publish(payload, NetworkMessageTarget.all());
    }

    @Override
    public CompletableFuture<Void> publish(byte[] payload, NetworkMessageTarget target) {
        byte[] data = payload == null ? new byte[0] : payload;
        NetworkMessageTarget activeTarget = target == null ? NetworkMessageTarget.all() : target;

        return client.message("network.message.publish", Payload.create()
                .put("channel", channel.key())
                .put("payload", Base64.getEncoder().encodeToString(data))
                .putAll(activeTarget.payload())
                .map()).thenApply(response -> null);
    }

    @Override
    public AutoCloseable listen(IEventListener<NetworkChannelMessageEvent> listener) {
        return events.listen(NetworkChannelMessageEvent.class, EventPriority.NORMAL, event -> {
            if (!event.channel().equals(channel)) {
                return;
            }

            listener.handle(event);
        });
    }

}
