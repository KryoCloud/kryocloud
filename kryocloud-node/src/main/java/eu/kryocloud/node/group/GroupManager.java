package eu.kryocloud.node.group;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.group.IGroupManager;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class GroupManager implements IGroupManager {

    @Override
    public void createGroup(IGroup group) {

    }

    @Override
    public void deleteGroup(UUID uniqueId) {

    }

    @Override
    public boolean existsGroup(String name) {
        return false;
    }

    @Override
    public IGroup groupById(UUID uniqueId) {
        return null;
    }

    @Override
    public IGroup groupByName(String name) {
        return null;
    }

    @Override
    public Collection<IGroup> groups() {
        return List.of();
    }
}
