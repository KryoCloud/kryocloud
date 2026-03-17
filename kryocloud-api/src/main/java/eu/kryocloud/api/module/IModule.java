package eu.kryocloud.api.module;

import eu.kryocloud.api.node.INode;
import eu.kryocloud.api.wrapper.IWrapper;

import java.util.Collection;

public interface IModule {

    void load();
    void enabled();
    void disabled();

    INode node();
    Collection<IWrapper> workers();

}
