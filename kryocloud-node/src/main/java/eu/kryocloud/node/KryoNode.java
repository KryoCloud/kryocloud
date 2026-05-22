package eu.kryocloud.node;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.database.IDatabaseProvider;
import eu.kryocloud.api.group.IGroupManager;
import eu.kryocloud.api.node.INode;
import eu.kryocloud.api.service.IServiceManager;
import eu.kryocloud.api.template.ITemplateManager;
import eu.kryocloud.common.config.ConfigProvider;
import eu.kryocloud.network.KryoProtocolServer;
import eu.kryocloud.network.auth.AuthManager;
import eu.kryocloud.node.config.LaunchConfig;
import eu.kryocloud.node.config.NodeSecurityConfig;
import eu.kryocloud.node.console.CommandRegistry;
import eu.kryocloud.node.console.KryoConsole;
import eu.kryocloud.node.console.NodeConsoleCommands;
import eu.kryocloud.node.database.DatabaseProvider;
import eu.kryocloud.node.group.GroupManager;
import eu.kryocloud.node.service.ServiceManager;
import eu.kryocloud.node.service.runtime.NodeServicePacketHandlers;
import eu.kryocloud.node.service.runtime.NodeServiceRegistry;
import eu.kryocloud.node.service.schedule.NodeServiceScheduler;
import eu.kryocloud.node.template.TemplateManager;
import eu.kryocloud.node.wrapper.NodeWrapperPacketHandlers;
import eu.kryocloud.node.wrapper.NodeWrapperRegistry;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class KryoNode implements INode {

    private final AtomicBoolean running = new AtomicBoolean(false);

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
    private KryoConsole console;

    public KryoNode() {
        start();
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            configProvider = new ConfigProvider();

            LaunchConfig launchConfig = configProvider.registerConfig(Path.of("launch.cfg"), LaunchConfig.class);
            NodeSecurityConfig securityConfig = configProvider.registerConfig(Path.of("security.cfg"), NodeSecurityConfig.class);
            AuthManager.registerToken(securityConfig.getToken());

            databaseProvider = new DatabaseProvider();
            templateManager = new TemplateManager(configProvider);
            groupManager = new GroupManager(configProvider);
            serviceManager = new ServiceManager();

            wrapperRegistry = new NodeWrapperRegistry();
            wrapperPacketHandlers = new NodeWrapperPacketHandlers(wrapperRegistry);
            wrapperPacketHandlers.register();

            serviceRegistry = new NodeServiceRegistry();
            servicePacketHandlers = new NodeServicePacketHandlers(serviceRegistry);
            servicePacketHandlers.register();

            serviceScheduler = new NodeServiceScheduler(wrapperRegistry, groupManager);

            protocolServer = new KryoProtocolServer(launchConfig.getPort());
            protocolServer.start();

            CommandRegistry commandRegistry = NodeConsoleCommands.createDefaultRegistry();
            console = new KryoConsole(this, commandRegistry);
            console.start();
        } catch (Exception exception) {
            shutdown();
            throw new RuntimeException("Failed to start KryoNode", exception);
        }
    }

    public void shutdown() {
        if (!running.getAndSet(false)) {
            return;
        }

        if (console != null) {
            console.close();
            console = null;
        }

        if (servicePacketHandlers != null) {
            servicePacketHandlers.close();
            servicePacketHandlers = null;
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

        if (configProvider != null) {
            configProvider.unregisterConfig(NodeSecurityConfig.class);
            configProvider.unregisterConfig(LaunchConfig.class);
            configProvider = null;
        }

        System.out.println("KryoCloud node stopped.");
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
    public ITemplateManager templateManager() {
        return templateManager;
    }

    @Override
    public IGroupManager groupManager() {
        return groupManager;
    }

    @Override
    public IServiceManager serviceManager() {
        return serviceManager;
    }
}