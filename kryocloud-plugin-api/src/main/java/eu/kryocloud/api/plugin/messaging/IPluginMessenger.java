package eu.kryocloud.api.plugin.messaging;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public interface IPluginMessenger {

    CompletableFuture<Void> send(PluginChannel channel, byte[] payload);

    default CompletableFuture<Void> sendText(PluginChannel channel, String message) {
        return send(channel, (message == null ? "" : message).getBytes(StandardCharsets.UTF_8));
    }

    AutoCloseable listen(PluginChannel channel, IPluginMessageListener listener);

}
