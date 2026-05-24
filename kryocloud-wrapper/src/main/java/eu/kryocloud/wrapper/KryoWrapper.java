package eu.kryocloud.wrapper;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.screen.IScreenManager;
import eu.kryocloud.api.wrapper.IWrapper;
import eu.kryocloud.common.config.ConfigProvider;
import eu.kryocloud.common.layout.KryoDirectoryLayout;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.KryoProtocolClient;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.wrapper.WrapperRegisterPacket;
import eu.kryocloud.network.protocol.PeerType;
import eu.kryocloud.wrapper.config.WrapperLaunchConfig;
import eu.kryocloud.wrapper.heartbeat.WrapperHeartbeatTask;
import eu.kryocloud.wrapper.instance.InstanceManager;
import eu.kryocloud.wrapper.instance.InstancePacketHandlers;
import eu.kryocloud.wrapper.instance.runtime.JavaRuntimeResolver;
import eu.kryocloud.wrapper.instance.workspace.InstanceWorkspace;
import eu.kryocloud.wrapper.screen.ScreenManager;

import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class KryoWrapper implements IWrapper {

    private static final KryoLogger LOGGER = KryoLogger.logger("Wrapper");

    private IConfigProvider configProvider;
    private WrapperLaunchConfig launchConfig;
    private IScreenManager screenManager;
    private KryoProtocolClient protocolClient;
    private InstanceManager instanceManager;
    private InstancePacketHandlers instancePacketHandlers;
    private WrapperHeartbeatTask heartbeatTask;
    private ScheduledExecutorService heartbeatExecutor;

    public KryoWrapper() {
        start();
    }

    public void start() {
        try {
            KryoDirectoryLayout.ensureWrapperDirectories();

            configProvider = new ConfigProvider();
            launchConfig = configProvider.registerConfig(KryoDirectoryLayout.CONFIG.resolve("wrapper.cfg"), WrapperLaunchConfig.class);

            String wrapperId = requireNonBlank(launchConfig.getWrapperId(), "wrapperId");
            String nodeHost = requireNonBlank(launchConfig.getNodeHost(), "nodeHost");
            String token = requireNonBlank(launchConfig.getToken(), "token");
            String advertisedAddress = requireNonBlank(launchConfig.getAdvertisedAddress(), "advertisedAddress");
            Path javaRuntimesDirectory = Path.of(requireNonBlank(launchConfig.getJavaRuntimesDirectory(), "javaRuntimesDirectory"));

            validatePort(launchConfig.getNodePort(), "nodePort");
            validatePositive(launchConfig.getMaxMemoryMb(), "maxMemoryMb");
            validatePositive(launchConfig.getStartupProbeSeconds(), "startupProbeSeconds");
            validatePositive(launchConfig.getShutdownTimeoutSeconds(), "shutdownTimeoutSeconds");

            screenManager = new ScreenManager();
            InstanceWorkspace workspace = new InstanceWorkspace(KryoDirectoryLayout.TEMPLATES, KryoDirectoryLayout.TMP, KryoDirectoryLayout.STATIC);
            JavaRuntimeResolver javaRuntimeResolver = new JavaRuntimeResolver(javaRuntimesDirectory, Duration.ofSeconds(3));
            instanceManager = new InstanceManager(wrapperId, advertisedAddress, screenManager, workspace, javaRuntimeResolver, launchConfig.getStartupProbeSeconds(), launchConfig.getShutdownTimeoutSeconds());
            instancePacketHandlers = new InstancePacketHandlers(instanceManager);
            instancePacketHandlers.register();

            protocolClient = new KryoProtocolClient(nodeHost, launchConfig.getNodePort());
            KryoConnection nodeConnection = protocolClient.connect(PeerType.WRAPPER, wrapperId, token, Duration.ofSeconds(10));

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
        if (heartbeatTask != null) {
            heartbeatTask.close();
            heartbeatTask = null;
        }

        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
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

    private void validatePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }
    }
}
