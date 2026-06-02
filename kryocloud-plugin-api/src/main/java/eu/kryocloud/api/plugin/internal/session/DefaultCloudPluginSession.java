package eu.kryocloud.api.plugin.internal.session;

import eu.kryocloud.api.plugin.ICloudPlugin;
import eu.kryocloud.api.plugin.bootstrap.CloudPluginConnectionStatus;
import eu.kryocloud.api.plugin.bootstrap.CloudPluginEndpoint;
import eu.kryocloud.api.plugin.bootstrap.CloudPluginSession;
import eu.kryocloud.api.plugin.context.PluginContext;
import eu.kryocloud.api.plugin.description.PluginDescription;
import eu.kryocloud.api.plugin.identity.CloudServiceIdentity;
import eu.kryocloud.api.plugin.event.lifecycle.CloudConnectedEvent;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.context.RemotePluginContext;
import eu.kryocloud.api.plugin.internal.event.RemoteEventBus;
import eu.kryocloud.api.plugin.internal.messaging.RemotePluginMessenger;
import eu.kryocloud.api.plugin.internal.remote.RemotePluginCloud;
import eu.kryocloud.api.plugin.internal.scheduler.VirtualPluginScheduler;
import eu.kryocloud.api.plugin.logging.IPluginLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultCloudPluginSession implements CloudPluginSession {

    private final ICloudPlugin plugin;
    private final PluginDescription description;
    private final CloudServiceIdentity identity;
    private final CloudPluginEndpoint endpoint;
    private final Duration timeout;
    private final Path dataDirectory;
    private final IPluginLogger logger;
    private final boolean autoEnable;
    private final AtomicReference<CloudPluginConnectionStatus> status = new AtomicReference<>(CloudPluginConnectionStatus.NEW);
    private final CompletableFuture<Void> ready = new CompletableFuture<>();

    private Instant connectedAt = Instant.EPOCH;
    private PluginRequestClient client;
    private RemotePluginContext context;
    private RemoteEventBus eventBus;
    private VirtualPluginScheduler scheduler;

    public DefaultCloudPluginSession(ICloudPlugin plugin, PluginDescription description, CloudServiceIdentity identity, CloudPluginEndpoint endpoint, Duration timeout, Path dataDirectory, IPluginLogger logger, boolean autoEnable) {
        this.plugin = plugin;
        this.description = description;
        this.identity = identity;
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.autoEnable = autoEnable;
    }

    public CompletableFuture<Void> connect() {
        status.set(CloudPluginConnectionStatus.CONNECTING);

        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(dataDirectory);
                this.eventBus = new RemoteEventBus(logger);
                this.client = new PluginRequestClient(description, identity, endpoint, timeout, eventBus);
                this.scheduler = new VirtualPluginScheduler(logger);
                RemotePluginCloud cloud = new RemotePluginCloud(client);
                RemotePluginMessenger messenger = new RemotePluginMessenger(client, eventBus);
                this.context = new RemotePluginContext(description, identity, cloud, eventBus, messenger, scheduler, logger, dataDirectory, this);
                eventBus.client(client);
                plugin.load(context);
                client.connect().join();
                connectedAt = Instant.now();
                status.set(CloudPluginConnectionStatus.CONNECTED);
                eventBus.publish(new CloudConnectedEvent()).join();

                if (autoEnable) {
                    plugin.enable(context);
                }

                ready.complete(null);
            } catch (Throwable throwable) {
                status.set(CloudPluginConnectionStatus.FAILED);
                ready.completeExceptionally(throwable);
                throw new IllegalStateException("Could not start KryoCloud plugin session", throwable);
            }
        });
    }

    @Override
    public PluginDescription description() {
        return description;
    }

    @Override
    public CloudServiceIdentity identity() {
        return identity;
    }

    @Override
    public PluginContext context() {
        return context;
    }

    @Override
    public CloudPluginConnectionStatus status() {
        return status.get();
    }

    @Override
    public Instant connectedAt() {
        return connectedAt;
    }

    @Override
    public boolean connected() {
        return status() == CloudPluginConnectionStatus.CONNECTED && client != null && client.connected();
    }

    @Override
    public CompletableFuture<Void> ready() {
        return ready;
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        if (status.get() == CloudPluginConnectionStatus.DISCONNECTED) {
            return CompletableFuture.completedFuture(null);
        }

        status.set(CloudPluginConnectionStatus.DISCONNECTING);

        return CompletableFuture.runAsync(() -> {
            try {
                plugin.disable(context);
            } catch (Throwable throwable) {
                logger.error("Could not disable plugin", throwable);
            }

            if (client != null) {
                client.close();
            }

            if (scheduler != null) {
                scheduler.close();
            }

            status.set(CloudPluginConnectionStatus.DISCONNECTED);
        });
    }

}
