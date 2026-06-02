package eu.kryocloud.api.plugin.messaging;

import java.util.concurrent.CompletableFuture;

public interface IPluginMessenger {

    CompletableFuture<Void> send(PluginChannel channel, byte[] payload);

    AutoCloseable listen(PluginChannel channel, IPluginMessageListener listener);

}
