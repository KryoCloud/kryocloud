package eu.kryocloud.api.plugin;

import eu.kryocloud.api.plugin.bootstrap.CloudPluginBootstrap;
import eu.kryocloud.api.plugin.bootstrap.CloudPluginSession;
import eu.kryocloud.api.plugin.cloud.IPluginCloud;
import eu.kryocloud.api.plugin.cloud.controller.ICloudGroupController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudMaintenanceController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudServiceController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudTemplateController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudVersionController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudWrapperController;
import eu.kryocloud.api.plugin.context.PluginContext;
import eu.kryocloud.api.plugin.description.PluginDescription;
import eu.kryocloud.api.plugin.event.IEventBus;
import eu.kryocloud.api.plugin.identity.CloudServiceIdentity;
import eu.kryocloud.api.plugin.logging.IPluginLogger;
import eu.kryocloud.api.plugin.messaging.IPluginMessenger;
import eu.kryocloud.api.plugin.scheduler.IPluginScheduler;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class CloudAPI {

    private static final AtomicReference<CloudPluginSession> SESSION = new AtomicReference<>();

    private CloudAPI() {
    }

    public static CloudPluginSession init(Object plugin) {
        return bootstrap(plugin).connect();
    }

    public static CloudPluginSession init(Object platformPlugin, ICloudPlugin plugin) {
        return bootstrap(platformPlugin)
                .plugin(plugin)
                .connect();
    }

    public static CompletableFuture<CloudPluginSession> initAsync(Object plugin) {
        return bootstrap(plugin).connectAsync();
    }

    public static CompletableFuture<CloudPluginSession> initAsync(Object platformPlugin, ICloudPlugin plugin) {
        return bootstrap(platformPlugin)
                .plugin(plugin)
                .connectAsync();
    }

    public static CloudPluginBootstrap bootstrap(Object plugin) {
        return CloudPluginBootstrap.create(plugin);
    }

    public static Optional<CloudPluginSession> optionalSession() {
        return Optional.ofNullable(SESSION.get());
    }

    public static CloudPluginSession session() {
        CloudPluginSession session = SESSION.get();

        if (session != null) {
            return session;
        }

        throw new IllegalStateException("CloudAPI is not initialized");
    }

    public static PluginContext context() {
        return session().context();
    }

    public static PluginDescription description() {
        return session().description();
    }

    public static CloudServiceIdentity identity() {
        return session().identity();
    }

    public static boolean runningInCloud() {
        return identity().runningInCloud();
    }

    public static String serviceId() {
        return identity().serviceId();
    }

    public static String serviceName() {
        return identity().serviceName();
    }

    public static String groupName() {
        return identity().groupName();
    }

    public static String wrapperId() {
        return identity().wrapperId();
    }

    public static IPluginCloud cloud() {
        return context().cloud();
    }

    public static ICloudServiceController services() {
        return cloud().services();
    }

    public static ICloudGroupController groups() {
        return cloud().groups();
    }

    public static ICloudWrapperController wrappers() {
        return cloud().wrappers();
    }

    public static ICloudTemplateController templates() {
        return cloud().templates();
    }

    public static ICloudVersionController versions() {
        return cloud().versions();
    }

    public static ICloudMaintenanceController maintenance() {
        return cloud().maintenance();
    }

    public static IEventBus events() {
        return context().events();
    }

    public static IPluginMessenger messages() {
        return context().messages();
    }

    public static IPluginScheduler scheduler() {
        return context().scheduler();
    }

    public static IPluginLogger logger() {
        return context().logger();
    }

    public static Path dataDirectory() {
        return context().dataDirectory();
    }

    public static boolean initialized() {
        return SESSION.get() != null;
    }

    public static boolean connected() {
        CloudPluginSession session = SESSION.get();

        if (session == null) {
            return false;
        }

        return session.connected();
    }

    public static CompletableFuture<Void> ready() {
        return session().ready();
    }

    public static CompletableFuture<Void> shutdown() {
        CloudPluginSession session = SESSION.getAndSet(null);

        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }

        return session.disconnect();
    }

    public static void attach(CloudPluginSession session) {
        Objects.requireNonNull(session, "session must not be null");
        SESSION.set(session);
    }

}
