package eu.kryocloud.api.plugin.bootstrap;

import eu.kryocloud.api.plugin.CloudAPI;
import eu.kryocloud.api.plugin.ICloudPlugin;
import eu.kryocloud.api.plugin.description.PluginDescription;
import eu.kryocloud.api.plugin.identity.CloudServiceIdentity;
import eu.kryocloud.api.plugin.internal.platform.PlatformAccess;
import eu.kryocloud.api.plugin.internal.session.DefaultCloudPluginSession;
import eu.kryocloud.api.plugin.logging.IPluginLogger;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class CloudPluginBootstrap {

    private final Object platformPlugin;

    private ICloudPlugin plugin;
    private PluginDescription description;
    private CloudPluginEndpoint endpoint = CloudPluginEndpoint.configured();
    private Duration timeout = Duration.ofSeconds(8);
    private Path dataDirectory;
    private boolean autoEnable = true;
    private boolean global = true;

    private CloudPluginBootstrap(Object platformPlugin) {
        this.platformPlugin = Objects.requireNonNull(platformPlugin, "platformPlugin must not be null");
    }

    public static CloudPluginBootstrap create(Object platformPlugin) {
        return new CloudPluginBootstrap(platformPlugin);
    }

    public CloudPluginBootstrap plugin(ICloudPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin must not be null");
        return this;
    }

    public CloudPluginBootstrap description(PluginDescription description) {
        this.description = Objects.requireNonNull(description, "description must not be null");
        return this;
    }

    public CloudPluginBootstrap endpoint(CloudPluginEndpoint endpoint) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint must not be null");
        return this;
    }

    public CloudPluginBootstrap address(String host, int port) {
        this.endpoint = CloudPluginEndpoint.of(host, port);
        return this;
    }

    public CloudPluginBootstrap host(String host) {
        this.endpoint = CloudPluginEndpoint.of(host, endpoint.port());
        return this;
    }

    public CloudPluginBootstrap port(int port) {
        this.endpoint = CloudPluginEndpoint.of(endpoint.host(), port);
        return this;
    }

    public CloudPluginBootstrap timeout(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        this.timeout = timeout;
        return this;
    }

    public CloudPluginBootstrap dataDirectory(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory must not be null");
        return this;
    }

    public CloudPluginBootstrap autoEnable(boolean autoEnable) {
        this.autoEnable = autoEnable;
        return this;
    }

    public CloudPluginBootstrap global(boolean global) {
        this.global = global;
        return this;
    }

    public CloudPluginSession connect() {
        return connectAsync().join();
    }

    public CompletableFuture<CloudPluginSession> connectAsync() {
        ICloudPlugin activePlugin = plugin == null ? resolvePlugin() : plugin;
        PluginDescription activeDescription = description == null ? activePlugin.description() : description;
        CloudServiceIdentity identity = CloudServiceIdentity.resolve();
        CloudPluginEndpoint activeEndpoint = endpoint == null ? identity.endpoint() : endpoint;
        Path activeDataDirectory = dataDirectory == null ? PlatformAccess.dataDirectory(platformPlugin, activeDescription) : dataDirectory;
        IPluginLogger logger = PlatformAccess.logger(platformPlugin, activeDescription);
        DefaultCloudPluginSession session = new DefaultCloudPluginSession(activePlugin, activeDescription, identity, activeEndpoint, timeout, activeDataDirectory, logger, autoEnable);

        return session.connect().thenApply(value -> {
            if (global) {
                CloudAPI.attach(session);
            }

            return session;
        });
    }

    private ICloudPlugin resolvePlugin() {
        if (platformPlugin instanceof ICloudPlugin cloudPlugin) {
            return cloudPlugin;
        }

        return new PlatformCloudPlugin(platformPlugin.getClass());
    }

    private record PlatformCloudPlugin(Class<?> type) implements ICloudPlugin {

        @Override
        public PluginDescription description() {
            return PluginDescription.from(type);
        }

    }

}
