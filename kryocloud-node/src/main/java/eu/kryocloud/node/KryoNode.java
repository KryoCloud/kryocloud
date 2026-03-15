package eu.kryocloud.node;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.database.IDatabaseProvider;
import eu.kryocloud.api.group.IGroupManager;
import eu.kryocloud.api.node.INode;
import eu.kryocloud.api.service.IServiceManager;
import eu.kryocloud.api.template.ITemplateManager;
import eu.kryocloud.common.config.ConfigProvider;
import eu.kryocloud.node.database.DatabaseProvider;
import eu.kryocloud.node.group.GroupManager;
import eu.kryocloud.node.service.ServiceManager;
import eu.kryocloud.node.template.TemplateManager;

public class KryoNode implements INode {

    private final IConfigProvider configProvider;
    private final IDatabaseProvider databaseProvider;
    private final ITemplateManager templateManager;
    private final IGroupManager groupManager;
    private final IServiceManager serviceManager;

    public KryoNode() {
        this.configProvider = new ConfigProvider();
        this.databaseProvider = new DatabaseProvider();
        this.templateManager = new TemplateManager();
        this.groupManager = new GroupManager();
        this.serviceManager = new ServiceManager();
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
