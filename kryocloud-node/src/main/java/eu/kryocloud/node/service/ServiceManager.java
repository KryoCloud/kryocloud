package eu.kryocloud.node.service;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.service.IService;
import eu.kryocloud.api.service.IServiceManager;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ServiceManager implements IServiceManager {

    @Override
    public IService createService(IGroup group) {
        return null;
    }

    @Override
    public Collection<IService> createServices(IGroup group, int count) {
        return List.of();
    }

    @Override
    public boolean existsService(String name) {
        return false;
    }

    @Override
    public IService serviceById(UUID uniqueId) {
        return null;
    }

    @Override
    public IService serviceByName(String name) {
        return null;
    }

    @Override
    public Collection<IService> services(String groupName) {
        return List.of();
    }

    @Override
    public Collection<IService> services() {
        return List.of();
    }
}
