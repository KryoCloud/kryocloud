package eu.kryocloud.api.module;

import eu.kryocloud.api.node.INode;
import eu.kryocloud.api.worker.IWorker;

import java.util.Collection;

public interface IModule {

    void load();
    void enabled();
    void disabled();

    INode node();
    Collection<IWorker> workers();

}
