package eu.kryocloud.api.plugin.network;

import eu.kryocloud.api.plugin.event.IEventListener;
import eu.kryocloud.api.plugin.event.network.NetworkChannelMessageEvent;
import eu.kryocloud.api.plugin.messaging.PluginChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public interface ICloudNetworkChannel {

    PluginChannel channel();

    CompletableFuture<Void> publish(byte[] payload);

    CompletableFuture<Void> publish(byte[] payload, NetworkMessageTarget target);

    default CompletableFuture<Void> publishText(String message) {
        return publish((message == null ? "" : message).getBytes(StandardCharsets.UTF_8));
    }

    default CompletableFuture<Void> publishText(String message, NetworkMessageTarget target) {
        return publish((message == null ? "" : message).getBytes(StandardCharsets.UTF_8), target);
    }

    default CompletableFuture<Void> publishToService(String service, byte[] payload) {
        return publish(payload, NetworkMessageTarget.service(service));
    }

    default CompletableFuture<Void> publishToGroup(String group, byte[] payload) {
        return publish(payload, NetworkMessageTarget.group(group));
    }

    default CompletableFuture<Void> publishToWrapper(String wrapper, byte[] payload) {
        return publish(payload, NetworkMessageTarget.wrapper(wrapper));
    }

    AutoCloseable listen(IEventListener<NetworkChannelMessageEvent> listener);

}
