package eu.kryocloud.api.plugin.internal.context;

import eu.kryocloud.api.plugin.bootstrap.CloudPluginSession;
import eu.kryocloud.api.plugin.cloud.IPluginCloud;
import eu.kryocloud.api.plugin.context.PluginContext;
import eu.kryocloud.api.plugin.description.PluginDescription;
import eu.kryocloud.api.plugin.event.IEventBus;
import eu.kryocloud.api.plugin.identity.CloudServiceIdentity;
import eu.kryocloud.api.plugin.logging.IPluginLogger;
import eu.kryocloud.api.plugin.messaging.IPluginMessenger;
import eu.kryocloud.api.plugin.scheduler.IPluginScheduler;
import java.nio.file.Path;

public final class RemotePluginContext implements PluginContext {

    private final PluginDescription description;
    private final CloudServiceIdentity identity;
    private final IPluginCloud cloud;
    private final IEventBus events;
    private final IPluginMessenger messages;
    private final IPluginScheduler scheduler;
    private final IPluginLogger logger;
    private final Path dataDirectory;
    private final CloudPluginSession session;

    public RemotePluginContext(PluginDescription description, CloudServiceIdentity identity, IPluginCloud cloud, IEventBus events, IPluginMessenger messages, IPluginScheduler scheduler, IPluginLogger logger, Path dataDirectory, CloudPluginSession session) {
        this.description = description;
        this.identity = identity;
        this.cloud = cloud;
        this.events = events;
        this.messages = messages;
        this.scheduler = scheduler;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.session = session;
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
    public IPluginCloud cloud() {
        return cloud;
    }

    @Override
    public IEventBus events() {
        return events;
    }

    @Override
    public IPluginMessenger messages() {
        return messages;
    }

    @Override
    public IPluginScheduler scheduler() {
        return scheduler;
    }

    @Override
    public IPluginLogger logger() {
        return logger;
    }

    @Override
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Override
    public CloudPluginSession session() {
        return session;
    }

}
