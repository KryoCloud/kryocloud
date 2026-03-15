package eu.kryocloud.api.group;

import java.util.Collection;
import java.util.UUID;

public interface IGroupManager {

    void createGroup(IGroup group);
    void deleteGroup(UUID uniqueId);

    boolean existsGroup(String name);

    IGroup groupById(UUID uniqueId);
    IGroup groupByName(String name);
    Collection<IGroup> groups();

}
