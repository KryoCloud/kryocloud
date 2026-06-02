package eu.kryocloud.api.plugin.bootstrap;

import eu.kryocloud.api.plugin.context.PluginContext;
import eu.kryocloud.api.plugin.description.PluginDescription;
import eu.kryocloud.api.plugin.identity.CloudServiceIdentity;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public interface CloudPluginSession extends AutoCloseable {

    PluginDescription description();

    CloudServiceIdentity identity();

    PluginContext context();

    CloudPluginConnectionStatus status();

    Instant connectedAt();

    boolean connected();

    CompletableFuture<Void> ready();

    CompletableFuture<Void> disconnect();

    @Override
    default void close() {
        disconnect().join();
    }

}
