package eu.kryocloud.wrapper;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.screen.IScreenManager;
import eu.kryocloud.api.wrapper.IWrapper;
import eu.kryocloud.common.config.ConfigPathResolver;
import eu.kryocloud.common.config.ConfigProvider;
import eu.kryocloud.common.layout.KryoDirectoryLayout;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.KryoProtocolClient;
import eu.kryocloud.sphere.KryoSphereMode;
import eu.kryocloud.sphere.KryoSpherePlatform;
import eu.kryocloud.sphere.KryoSphereSettings;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.wrapper.WrapperRegisterPacket;
import eu.kryocloud.network.protocol.PeerType;
import eu.kryocloud.wrapper.config.WrapperLaunchConfig;
import eu.kryocloud.wrapper.heartbeat.WrapperHeartbeatTask;
import eu.kryocloud.wrapper.instance.InstanceManager;
import eu.kryocloud.wrapper.instance.InstancePacketHandlers;
import eu.kryocloud.wrapper.instance.runtime.JavaRuntimeResolver;
import eu.kryocloud.wrapper.instance.workspace.InstanceWorkspace;
import eu.kryocloud.wrapper.plugin.WrapperPluginGatewayServer;
import eu.kryocloud.wrapper.screen.ScreenManager;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public final class KryoWrapper implements IWrapper {

    private static final KryoLogger LOGGER = KryoLogger.logger("Wrapper");

    private final AtomicBoolean running = new AtomicBoolean(false);

    private IConfigProvider configProvider;
    private WrapperLaunchConfig launchConfig;
    private IScreenManager screenManager;
    private KryoProtocolClient protocolClient;
    private InstanceManager instanceManager;
    private InstancePacketHandlers instancePacketHandlers;
    private WrapperHeartbeatTask heartbeatTask;
    private WrapperPluginGatewayServer pluginGatewayServer;
    private ScheduledExecutorService heartbeatExecutor;

    public KryoWrapper() {
        start();
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            KryoDirectoryLayout.bootstrap();
            KryoDirectoryLayout.ensureWrapperDirectories();

            configProvider = new ConfigProvider();
            launchConfig = configProvider.registerConfig(ConfigPathResolver.resolve(KryoDirectoryLayout.CONFIG, "wrapper"), WrapperLaunchConfig.class);
            migrateLegacyPluginApiPort(launchConfig);

            String wrapperId = requireNonBlank(launchConfig.getWrapperId(), "wrapperId");
            String nodeHost = requireNonBlank(launchConfig.getNodeHost(), "nodeHost");
            String token = requireNonBlank(launchConfig.getToken(), "token");
            String advertisedAddress = requireNonBlank(launchConfig.getAdvertisedAddress(), "advertisedAddress");
            String pluginApiHost = requireNonBlank(launchConfig.getPluginApiHost(), "pluginApiHost");
            Path javaRuntimesDirectory = runtimeDirectory(requireNonBlank(launchConfig.getJavaRuntimesDirectory(), "javaRuntimesDirectory"));

            validatePort(launchConfig.getNodePort(), "nodePort");
            validatePortOrAuto(launchConfig.getPluginApiPort(), "pluginApiPort");
            validatePositive(launchConfig.getMaxMemoryMb(), "maxMemoryMb");
            validatePositive(launchConfig.getStartupProbeSeconds(), "startupProbeSeconds");
            validatePositive(launchConfig.getShutdownTimeoutSeconds(), "shutdownTimeoutSeconds");

            screenManager = new ScreenManager();
            InstanceWorkspace workspace = new InstanceWorkspace(KryoDirectoryLayout.TEMPLATES, KryoDirectoryLayout.TMP, KryoDirectoryLayout.STATIC, KryoDirectoryLayout.ADDONS);
            JavaRuntimeResolver javaRuntimeResolver = new JavaRuntimeResolver(javaRuntimesDirectory, Duration.ofSeconds(3));

            protocolClient = new KryoProtocolClient(nodeHost, launchConfig.getNodePort());
            KryoConnection nodeConnection = protocolClient.connect(PeerType.WRAPPER, wrapperId, token, Duration.ofSeconds(10));

            pluginGatewayServer = new WrapperPluginGatewayServer(pluginApiHost, launchConfig.getPluginApiPort(), wrapperId, protocolClient);
            pluginGatewayServer.start();
            int effectivePluginApiPort = pluginGatewayServer.boundPort();

            instanceManager = new InstanceManager(launchConfig.getCloudName(), wrapperId, advertisedAddress, pluginApiHost, effectivePluginApiPort, screenManager, workspace, javaRuntimeResolver, sphereSettings(launchConfig), launchConfig.getStartupProbeSeconds(), launchConfig.getShutdownTimeoutSeconds());
            instancePacketHandlers = new InstancePacketHandlers(instanceManager);
            instancePacketHandlers.register();

            protocolClient.send(new WrapperRegisterPacket(wrapperId, hostname(), advertisedAddress, System.getProperty("os.name"), Runtime.getRuntime().availableProcessors(), launchConfig.getMaxMemoryMb()));

            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "kryocloud-wrapper-heartbeat");
                thread.setDaemon(true);
                return thread;
            });

            heartbeatTask = new WrapperHeartbeatTask(wrapperId, protocolClient, instanceManager, heartbeatExecutor, launchConfig.getMaxMemoryMb());
            heartbeatTask.start();

            LOGGER.success("KryoWrapper connected as " + wrapperId + " via " + nodeConnection.id());
        } catch (Exception exception) {
            shutdown();
            throw new RuntimeException("Failed to start KryoWrapper", exception);
        }
    }

    public void shutdown() {
        if (!running.getAndSet(false)) {
            return;
        }

        if (heartbeatTask != null) {
            heartbeatTask.close();
            heartbeatTask = null;
        }

        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }

        if (pluginGatewayServer != null) {
            pluginGatewayServer.close();
            pluginGatewayServer = null;
        }

        if (instancePacketHandlers != null) {
            instancePacketHandlers.close();
            instancePacketHandlers = null;
        }

        if (instanceManager != null) {
            instanceManager.shutdown();
            instanceManager = null;
        }

        if (protocolClient != null) {
            protocolClient.close();
            protocolClient = null;
        }

        screenManager = null;

        if (configProvider != null) {
            configProvider.unregisterConfig(WrapperLaunchConfig.class);
            configProvider = null;
        }

        launchConfig = null;
    }

    public boolean running() {
        return running.get();
    }

    public IScreenManager screenManager() {
        return screenManager;
    }

    public KryoProtocolClient protocolClient() {
        return protocolClient;
    }

    public InstanceManager instanceManager() {
        return instanceManager;
    }

    public static void main(String[] args) {
        KryoWrapper wrapper = new KryoWrapper();
        Runtime.getRuntime().addShutdownHook(new Thread(wrapper::shutdown, "kryocloud-wrapper-shutdown"));
    }

    private void migrateLegacyPluginApiPort(WrapperLaunchConfig launchConfig) {
        if (launchConfig.getPluginApiPort() != 7070) {
            return;
        }

        launchConfig.setPluginApiPort(0);
        launchConfig.save();
        LOGGER.info("Migrated legacy pluginApiPort 7070 to auto-bind port 0.");
    }

    private KryoSphereSettings sphereSettings(WrapperLaunchConfig config) {
        KryoSphereMode mode = KryoSphereMode.parse(config.getKryoSphereMode());
        KryoSpherePlatform platform = KryoSpherePlatform.current();

        if (platform != KryoSpherePlatform.LINUX && mode != KryoSphereMode.NONE) {
            LOGGER.warn("KryoSphere isolation is only supported on Linux. Running on " + platform + ", so KryoSphere is disabled for this wrapper runtime.");
            return KryoSphereSettings.disabled();
        }

        return new KryoSphereSettings(
                mode,
                config.isKryoSphereBubblewrap(),
                config.isKryoSpherePrivateTmp(),
                config.isKryoSphereProtectHome(),
                config.isKryoSphereRestrictProc(),
                config.isKryoSphereNoNewPrivileges(),
                config.isKryoSphereClearEnvironment(),
                config.isKryoSphereAllowNetwork(),
                config.getKryoSphereMemoryLimitMb(),
                config.getKryoSphereCpuLimitPercent(),
                config.getKryoSphereOpenFileLimit(),
                config.getKryoSphereProcessLimit(),
                config.getKryoSphereTmpSizeMb(),
                config.getKryoSphereServiceUser(),
                splitPaths(config.getKryoSphereReadOnlyPaths()),
                splitPaths(config.getKryoSphereWritablePaths())
        );
    }

    private List<String> splitPaths(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .toList();
    }

    private Path runtimeDirectory(String configuredDirectory) {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            return KryoDirectoryLayout.JDK;
        }

        if ("runtimes".equalsIgnoreCase(configuredDirectory.trim())) {
            return KryoDirectoryLayout.JDK;
        }

        Path path = Path.of(configuredDirectory);

        if (path.isAbsolute()) {
            return path.normalize();
        }

        return KryoDirectoryLayout.ROOT.resolve(path).toAbsolutePath().normalize();
    }

    private String hostname() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();

            if (hostname != null && !hostname.isBlank()) {
                return hostname;
            }
        } catch (Exception exception) {
            return "unknown-host";
        }

        return "unknown-host";
    }

    private String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }

    private void validatePort(int value, String name) {
        if (value < 1 || value > 65_535) {
            throw new IllegalArgumentException(name + " must be between 1 and 65535");
        }
    }

    private void validatePortOrAuto(int value, String name) {
        if (value < 0 || value > 65_535) {
            throw new IllegalArgumentException(name + " must be 0 or between 1 and 65535");
        }
    }

    private void validatePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }
    }
}
