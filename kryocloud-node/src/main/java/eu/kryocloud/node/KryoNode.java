package eu.kryocloud.node;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.database.IDatabaseProvider;
import eu.kryocloud.api.group.IGroupManager;
import eu.kryocloud.api.node.INode;
import eu.kryocloud.api.service.IServiceManager;
import eu.kryocloud.api.template.ITemplateManager;
import eu.kryocloud.common.config.ConfigProvider;
import eu.kryocloud.network.NetServer;
import eu.kryocloud.network.packet.PacketManager;
import eu.kryocloud.network.packet.impl.AuthPacket;
import eu.kryocloud.node.config.LaunchConfig;
import eu.kryocloud.node.database.DatabaseProvider;
import eu.kryocloud.node.group.GroupManager;
import eu.kryocloud.node.service.ServiceManager;
import eu.kryocloud.node.template.TemplateManager;

import java.nio.file.Path;

public class KryoNode implements INode {

    private IConfigProvider configProvider;
    private IDatabaseProvider databaseProvider;
    private ITemplateManager templateManager;
    private IGroupManager groupManager;
    private IServiceManager serviceManager;

    private NetServer netServer;

    public KryoNode() {
        this.start();
    }

    public void start() {
        try {
            this.configProvider = new ConfigProvider();
            LaunchConfig launchConfig = this.configProvider.registerConfig(Path.of("launch.cfg"), LaunchConfig.class);

            this.databaseProvider = new DatabaseProvider();
            this.templateManager = new TemplateManager();
            this.groupManager = new GroupManager();
            this.serviceManager = new ServiceManager();

            PacketManager.register(0x01, AuthPacket.class);
            this.netServer = new NetServer(launchConfig.getPort());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        this.configProvider.unregisterConfig(LaunchConfig.class);

        this.netServer.close();
    }

    static void main() {
        KryoNode node = new KryoNode();
        Runtime.getRuntime().addShutdownHook(new Thread(node::shutdown));
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
