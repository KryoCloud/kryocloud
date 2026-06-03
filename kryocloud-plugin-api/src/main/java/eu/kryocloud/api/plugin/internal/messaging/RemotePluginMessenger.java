package eu.kryocloud.api.plugin.internal.messaging;

import eu.kryocloud.api.plugin.event.network.NetworkChannelMessageEvent;
import eu.kryocloud.api.plugin.messaging.IPluginMessageListener;
import eu.kryocloud.api.plugin.messaging.IPluginMessenger;
import eu.kryocloud.api.plugin.messaging.PluginChannel;
import eu.kryocloud.api.plugin.messaging.PluginMessage;
import eu.kryocloud.api.plugin.network.ICloudNetwork;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class RemotePluginMessenger implements IPluginMessenger {

    private final ICloudNetwork network;

    public RemotePluginMessenger(ICloudNetwork network) {
        this.network = network;
    }

    @Override
    public CompletableFuture<Void> send(PluginChannel channel, byte[] payload) {
        return network.channel(channel).publish(payload);
    }

    @Override
    public AutoCloseable listen(PluginChannel channel, IPluginMessageListener listener) {
        return network.channel(channel).listen(event -> listener.handle(message(event)));
    }

    public CompletableFuture<Void> sendText(PluginChannel channel, String message) {
        return send(channel, (message == null ? "" : message).getBytes(StandardCharsets.UTF_8));
    }

    private PluginMessage message(NetworkChannelMessageEvent event) {
        String source = event.sourceService().isBlank() ? event.sourcePlugin() : event.sourceService();
        return PluginMessage.of(event.channel(), source, event.payload());
    }

}
