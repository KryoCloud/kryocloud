package eu.kryocloud.api.plugin.context;

import eu.kryocloud.api.plugin.bootstrap.CloudPluginSession;
import eu.kryocloud.api.plugin.cloud.IPluginCloud;
import eu.kryocloud.api.plugin.description.PluginDescription;
import eu.kryocloud.api.plugin.event.IEventBus;
import eu.kryocloud.api.plugin.identity.CloudServiceIdentity;
import eu.kryocloud.api.plugin.logging.IPluginLogger;
import eu.kryocloud.api.plugin.messaging.IPluginMessenger;
import eu.kryocloud.api.plugin.scheduler.IPluginScheduler;
import java.nio.file.Path;

public interface PluginContext {

    PluginDescription description();

    CloudServiceIdentity identity();

    IPluginCloud cloud();

    IEventBus events();

    IPluginMessenger messages();

    IPluginScheduler scheduler();

    IPluginLogger logger();

    Path dataDirectory();

    CloudPluginSession session();

}
