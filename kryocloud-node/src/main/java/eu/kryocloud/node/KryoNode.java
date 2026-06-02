package eu.kryocloud.node;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.database.IDatabaseProvider;
import eu.kryocloud.api.node.INode;
import eu.kryocloud.api.service.IServiceManager;
import eu.kryocloud.common.config.ConfigProvider;
import eu.kryocloud.common.layout.KryoDirectoryLayout;
import eu.kryocloud.common.logging.ConsoleOutput;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.common.manifest.ManifestRepository;
import eu.kryocloud.network.KryoProtocolServer;
import eu.kryocloud.network.auth.AuthManager;
import eu.kryocloud.network.protocol.CloudServiceState;
import eu.kryocloud.node.config.LaunchConfig;
import eu.kryocloud.node.config.NodeSecurityConfig;
import eu.kryocloud.node.config.network.NetworkAddressConfig;
import eu.kryocloud.node.config.setup.WrapperSetupConfig;
import eu.kryocloud.node.console.CommandRegistry;
import eu.kryocloud.node.console.KryoConsole;
import eu.kryocloud.node.console.NodeConsoleCommands;
import eu.kryocloud.node.console.wizard.CloudHomeSetupWizard;
import eu.kryocloud.node.console.wizard.CloudSetupWizard;
import eu.kryocloud.node.database.DatabaseProvider;
import eu.kryocloud.node.group.GroupManager;
import eu.kryocloud.node.service.ServiceManager;
import eu.kryocloud.node.service.runtime.NodeServicePacketHandlers;
import eu.kryocloud.node.service.runtime.NodeServiceRegistry;
import eu.kryocloud.node.service.runtime.NodeServiceSnapshot;
import eu.kryocloud.node.service.schedule.NodeServiceScheduler;
import eu.kryocloud.node.template.TemplateManager;
import eu.kryocloud.node.version.NodeVersionStorage;
import eu.kryocloud.node.plugin.NodePluginGateway;
import eu.kryocloud.node.wrapper.NodeWrapperPacketHandlers;
import eu.kryocloud.node.wrapper.NodeWrapperRegistry;
import eu.kryocloud.node.wrapper.WrapperSnapshot;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class KryoNode implements INode {

    private static final KryoLogger LOGGER = KryoLogger.logger("Node");

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean consoleReady = new AtomicBoolean(false);
    private final AtomicBoolean pendingMinimumReconcile = new AtomicBoolean(false);
    private final AtomicBoolean reconcilingMinimumServices = new AtomicBoolean(false);

    private IConfigProvider configProvider;
    private IDatabaseProvider databaseProvider;
    private TemplateManager templateManager;
    private GroupManager groupManager;
    private IServiceManager serviceManager;

    private KryoProtocolServer protocolServer;
    private NodeWrapperRegistry wrapperRegistry;
    private NodeWrapperPacketHandlers wrapperPacketHandlers;
    private NodeServiceRegistry serviceRegistry;
    private NodeServicePacketHandlers servicePacketHandlers;
    private NodeServiceScheduler serviceScheduler;
    private NodeVersionStorage versionStorage;
    private NodePluginGateway pluginGateway;
    private NetworkAddressConfig networkAddressConfig;
    private KryoConsole console;

    public KryoNode() {
        start();
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        ConsoleOutput.deferBackgroundOutput(true);

        try {
            KryoDirectoryLayout.bootstrap();
            new CloudHomeSetupWizard().run();
            KryoDirectoryLayout.ensureNodeDirectories();

            configProvider = new ConfigProvider();

            LaunchConfig launchConfig = configProvider.registerConfig(KryoDirectoryLayout.CONFIG.resolve("launch.cfg"), LaunchConfig.class);
            NodeSecurityConfig securityConfig = configProvider.registerConfig(KryoDirectoryLayout.CONFIG.resolve("security.cfg"), NodeSecurityConfig.class);
            WrapperSetupConfig wrapperSetupConfig = configProvider.registerConfig(KryoDirectoryLayout.CONFIG.resolve("wrapper.cfg"), WrapperSetupConfig.class);
            networkAddressConfig = configProvider.registerConfig(KryoDirectoryLayout.CONFIG.resolve("network.cfg"), NetworkAddressConfig.class);

            new CloudSetupWizard().run(launchConfig, securityConfig, wrapperSetupConfig, networkAddressConfig);
            AuthManager.registerToken(securityConfig.getToken());

            databaseProvider = new DatabaseProvider();
            templateManager = new TemplateManager(configProvider);
            groupManager = new GroupManager(configProvider);
            serviceManager = new ServiceManager();

            versionStorage = new NodeVersionStorage(KryoDirectoryLayout.VERSIONS, KryoDirectoryLayout.TEMPLATES, ManifestRepository.defaults(), Duration.ofSeconds(60));

            wrapperRegistry = new NodeWrapperRegistry();
            serviceRegistry = new NodeServiceRegistry();
            serviceScheduler = new NodeServiceScheduler(wrapperRegistry, groupManager, serviceRegistry, versionStorage);
            pluginGateway = new NodePluginGateway(wrapperRegistry, serviceRegistry, serviceScheduler, groupManager, templateManager, versionStorage, networkAddressConfig);
            pluginGateway.register();

            wrapperPacketHandlers = new NodeWrapperPacketHandlers(wrapperRegistry, this::handleWrapperRegistered, this::handleWrapperStateChanged, this::handleWrapperHeartbeat);
            wrapperPacketHandlers.register();

            servicePacketHandlers = new NodeServicePacketHandlers(serviceRegistry, this::handleServiceStateChanged, this::handleServiceMetricsUpdated);
            servicePacketHandlers.register();

            protocolServer = new KryoProtocolServer(launchConfig.getHost(), launchConfig.getPort());
            protocolServer.start();

            CommandRegistry commandRegistry = NodeConsoleCommands.createDefaultRegistry();
            console = new KryoConsole(this, commandRegistry, launchConfig.getCloudName());
            console.start();

            LOGGER.success("KryoCloud " + launchConfig.getCloudName() + " node started on " + launchConfig.getHost() + ":" + launchConfig.getPort());
            LOGGER.info("Reserved web endpoint " + launchConfig.getWebHost() + ":" + launchConfig.getWebPort());

            if (pluginGateway != null) {
                pluginGateway.publishCloudReady();
            }
        } catch (Exception exception) {
            ConsoleOutput.flushDeferred();
            shutdown();
            throw new RuntimeException("Failed to start KryoNode", exception);
        }
    }

    public void shutdown() {
        if (!running.getAndSet(false)) {
            return;
        }

        if (pluginGateway != null) {
            pluginGateway.publishCloudStopping("KryoCloud node shutdown");
        }

        stopMinecraftServices();

        if (console != null) {
            console.close();
            console = null;
        }

        if (servicePacketHandlers != null) {
            servicePacketHandlers.close();
            servicePacketHandlers = null;
        }

        if (pluginGateway != null) {
            pluginGateway.close();
            pluginGateway = null;
        }

        if (wrapperPacketHandlers != null) {
            wrapperPacketHandlers.close();
            wrapperPacketHandlers = null;
        }

        if (protocolServer != null) {
            protocolServer.close();
            protocolServer = null;
        }

        if (serviceRegistry != null) {
            serviceRegistry.clear();
            serviceRegistry = null;
        }

        if (wrapperRegistry != null) {
            wrapperRegistry.clear();
            wrapperRegistry = null;
        }

        serviceScheduler = null;
        versionStorage = null;

        if (configProvider != null) {
            configProvider.unregisterConfig(NetworkAddressConfig.class);
            configProvider.unregisterConfig(WrapperSetupConfig.class);
            configProvider.unregisterConfig(NodeSecurityConfig.class);
            configProvider.unregisterConfig(LaunchConfig.class);
            configProvider = null;
        }

        LOGGER.success("KryoCloud node stopped.");
    }

    public void markConsoleReady() {
        if (!consoleReady.compareAndSet(false, true)) {
            return;
        }

        pendingMinimumReconcile.set(true);
        requestMinimumReconcile("after console intro");
    }

    private void handleServiceStateChanged(NodeServiceSnapshot previous, NodeServiceSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (pluginGateway != null) {
            pluginGateway.publishServiceState(previous, snapshot);
        }

        if (!running.get()) {
            return;
        }

        if (snapshot.state() != CloudServiceState.STOPPED) {
            return;
        }

        requestMinimumReconcile("after " + snapshot.serviceId() + " stopped");
    }

    private void handleServiceMetricsUpdated(NodeServiceSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (pluginGateway == null) {
            return;
        }

        pluginGateway.publishServiceMetrics(snapshot);
    }

    private void handleWrapperRegistered(WrapperSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (pluginGateway != null) {
            pluginGateway.publishWrapperConnected(snapshot);
            pluginGateway.publishWrapperState(snapshot);
        }

        if (serviceScheduler == null) {
            return;
        }

        requestMinimumReconcile("after wrapper availability");
    }


    private void handleWrapperStateChanged(WrapperSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (pluginGateway != null) {
            pluginGateway.publishWrapperState(snapshot);
        }

        if (serviceScheduler == null) {
            return;
        }

        requestMinimumReconcile("after wrapper state change");
    }

    private void handleWrapperHeartbeat(WrapperSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (pluginGateway == null) {
            return;
        }

        pluginGateway.publishWrapperHeartbeat(snapshot);
    }

    private void requestMinimumReconcile(String reason) {
        if (!running.get()) {
            return;
        }

        if (serviceScheduler == null) {
            pendingMinimumReconcile.set(true);
            return;
        }

        if (!consoleReady.get()) {
            pendingMinimumReconcile.set(true);
            return;
        }

        if (!reconcilingMinimumServices.compareAndSet(false, true)) {
            pendingMinimumReconcile.set(true);
            return;
        }

        try {
            pendingMinimumReconcile.set(false);
            List<?> results = serviceScheduler.reconcileMinimumServices();

            if (!results.isEmpty()) {
                LOGGER.success("Auto-start requested " + results.size() + " Minecraft service(s) " + reason + ".");
            }
        } catch (Exception exception) {
            LOGGER.warn("Auto-start reconciliation failed " + reason + ": " + exception.getMessage());
        } finally {
            reconcilingMinimumServices.set(false);
        }

        if (pendingMinimumReconcile.getAndSet(false)) {
            requestMinimumReconcile("after queued startup event");
        }
    }

    private void stopMinecraftServices() {
        if (serviceScheduler == null || serviceRegistry == null) {
            return;
        }

        if (serviceRegistry.services().isEmpty()) {
            return;
        }

        int sent = serviceScheduler.stopAll("KryoCloud node shutdown", false);
        LOGGER.warn("Stopping " + sent + " Minecraft service(s) before node shutdown.");

        if (sent < 1) {
            return;
        }

        if (!serviceScheduler.waitForServiceDrain(Duration.ofSeconds(25))) {
            LOGGER.warn("Not every Minecraft service confirmed shutdown in time. Closing protocol anyway.");
        }
    }

    public NodeWrapperRegistry wrapperRegistry() {
        return wrapperRegistry;
    }

    public NodeServiceRegistry serviceRegistry() {
        return serviceRegistry;
    }

    public NodeServiceScheduler serviceScheduler() {
        return serviceScheduler;
    }

    public NodeVersionStorage versionStorage() {
        return versionStorage;
    }

    public NetworkAddressConfig networkAddressConfig() {
        return networkAddressConfig;
    }

    public static void main(String[] args) {
        KryoNode node = new KryoNode();
        Runtime.getRuntime().addShutdownHook(new Thread(node::shutdown, "kryocloud-node-shutdown"));
    }

    @Override
    public IConfigProvider configProvider() {
        return configProvider;
    }

    @Override
    public IDatabaseProvider databaseProvider() {
        return databaseProvider;
    }

    @Override
    public TemplateManager templateManager() {
        return templateManager;
    }

    @Override
    public GroupManager groupManager() {
        return groupManager;
    }

    @Override
    public IServiceManager serviceManager() {
        return serviceManager;
    }
}
