package eu.kryocloud.api.node;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.database.IDatabaseProvider;
import eu.kryocloud.api.group.IGroupManager;
import eu.kryocloud.api.service.IServiceManager;
import eu.kryocloud.api.template.ITemplateManager;

public interface INode {
    IConfigProvider configProvider();
    IDatabaseProvider databaseProvider();
    ITemplateManager templateManager();
    IGroupManager groupManager();
    IServiceManager serviceManager();

    static void initialize(INode node) {
        NodeAccess.initialize(node);
    }

    static INode get() {
        return NodeAccess.get();
    }
}
