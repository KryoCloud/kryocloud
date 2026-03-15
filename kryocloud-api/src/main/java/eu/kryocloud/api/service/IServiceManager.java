package eu.kryocloud.api.service;

import eu.kryocloud.api.group.IGroup;

import java.util.Collection;
import java.util.UUID;

public interface IServiceManager {

    IService createService(IGroup group);
    Collection<IService> createServices(IGroup group, int count);

    boolean existsService(String name);

    IService serviceById(UUID uniqueId);
    IService serviceByName(String name);
    Collection<IService> services(String groupName);
    Collection<IService> services();

}
