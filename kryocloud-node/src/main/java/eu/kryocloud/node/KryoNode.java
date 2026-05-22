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
import eu.kryocloud.node.database.DatabaseProvider;
import eu.kryocloud.node.group.GroupManager;
import eu.kryocloud.node.service.ServiceManager;
import eu.kryocloud.node.service.schedule.NodeServiceScheduler;
import eu.kryocloud.node.template.TemplateManager;
import eu.kryocloud.node.wrapper.NodeWrapperPacketHandlers;
import eu.kryocloud.node.wrapper.NodeWrapperRegistry;

import java.nio.file.Path;

public class KryoNode implements INode {

    private IConfigProvider configProvider;
    private IDatabaseProvider databaseProvider;
    private ITemplateManager templateManager;
    private IGroupManager groupManager;
    private IServiceManager serviceManager;

    private KryoProtocolServer protocolServer;
    private NodeWrapperRegistry wrapperRegistry;
    private NodeWrapperPacketHandlers wrapperPacketHandlers;
    private NodeServiceScheduler serviceScheduler;

    public KryoNode() {
        start();
    }

    public void start() {
        try {
            configProvider = new ConfigProvider();

            LaunchConfig launchConfig = configProvider.registerConfig(Path.of("launch.cfg"), LaunchConfig.class);
            NodeSecurityConfig securityConfig = configProvider.registerConfig(Path.of("security.cfg"), NodeSecurityConfig.class);
            AuthManager.registerToken(securityConfig.getToken());

            databaseProvider = new DatabaseProvider();
            templateManager = new TemplateManager(configProvider);
            groupManager = new GroupManager();
            serviceManager = new ServiceManager();

            wrapperRegistry = new NodeWrapperRegistry();
            wrapperPacketHandlers = new NodeWrapperPacketHandlers(wrapperRegistry);
            serviceScheduler = new NodeServiceScheduler(wrapperRegistry);

            wrapperPacketHandlers.register();

            protocolServer = new KryoProtocolServer(launchConfig.getPort());
            protocolServer.start();
        } catch (Exception exception) {
            shutdown();
            throw new RuntimeException("Failed to start KryoNode", exception);
        }
    }

    public void shutdown() {
        if (wrapperPacketHandlers != null) {
            wrapperPacketHandlers.close();
            wrapperPacketHandlers = null;
        }

        if (protocolServer != null) {
            protocolServer.close();
            protocolServer = null;
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
    }

    public NodeWrapperRegistry wrapperRegistry() {
        return wrapperRegistry;
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