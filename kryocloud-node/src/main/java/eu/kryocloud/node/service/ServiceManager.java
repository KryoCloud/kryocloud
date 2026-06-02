package eu.kryocloud.node.service;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.group.IGroupManager;
import eu.kryocloud.api.service.IService;
import eu.kryocloud.api.service.IServiceManager;
import eu.kryocloud.node.service.runtime.NodeServiceRegistry;
import eu.kryocloud.node.service.runtime.NodeServiceSnapshot;
import eu.kryocloud.node.service.schedule.NodeServiceScheduler;
import eu.kryocloud.node.service.schedule.ServiceStartResult;
import eu.kryocloud.node.template.TemplateManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ServiceManager implements IServiceManager {

    private final IGroupManager groupManager;
    private final TemplateManager templateManager;
    private final NodeServiceRegistry serviceRegistry;
    private final NodeServiceScheduler scheduler;

    public ServiceManager(IGroupManager groupManager, TemplateManager templateManager, NodeServiceRegistry serviceRegistry, NodeServiceScheduler scheduler) {
        if (groupManager == null) {
            throw new IllegalArgumentException("groupManager must not be null");
        }

        if (templateManager == null) {
            throw new IllegalArgumentException("templateManager must not be null");
        }

        if (serviceRegistry == null) {
            throw new IllegalArgumentException("serviceRegistry must not be null");
        }

        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler must not be null");
        }

        this.groupManager = groupManager;
        this.templateManager = templateManager;
        this.serviceRegistry = serviceRegistry;
        this.scheduler = scheduler;
    }

    @Override
    public IService createService(IGroup group) {
        if (group == null) {
            throw new IllegalArgumentException("group must not be null");
        }

        List<ServiceStartResult> results = scheduler.startGroup(group.name(), 1);

        if (results.isEmpty()) {
            throw new IllegalStateException("No service was scheduled for group " + group.name());
        }

        return new CloudService(results.getFirst().serviceId(), group, scheduler, templateManager, Optional.empty());
    }

    @Override
    public Collection<IService> createServices(IGroup group, int count) {
        if (group == null) {
            throw new IllegalArgumentException("group must not be null");
        }

        if (count < 1) {
            throw new IllegalArgumentException("count must be greater than 0");
        }

        return scheduler.startGroup(group.name(), count).stream()
                .map(result -> new CloudService(result.serviceId(), group, scheduler, templateManager, Optional.<NodeServiceSnapshot>empty()))
                .map(IService.class::cast)
                .toList();
    }

    @Override
    public boolean existsService(String name) {
        validateName(name);
        return serviceRegistry.service(name).isPresent();
    }

    @Override
    public IService serviceById(UUID uniqueId) {
        if (uniqueId == null) {
            throw new IllegalArgumentException("uniqueId must not be null");
        }

        return services().stream()
                .filter(service -> service.uniqueId().equals(uniqueId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public IService serviceByName(String name) {
        validateName(name);
        Optional<NodeServiceSnapshot> snapshot = serviceRegistry.service(name);

        if (snapshot.isEmpty()) {
            return null;
        }

        return service(snapshot.get());
    }

    @Override
    public Collection<IService> services(String groupName) {
        validateName(groupName);
        return serviceRegistry.services(groupName).stream().map(this::service).toList();
    }

    @Override
    public Collection<IService> services() {
        return serviceRegistry.services().stream().map(this::service).toList();
    }

    private IService service(NodeServiceSnapshot snapshot) {
        IGroup group = groupManager.groupByName(snapshot.groupName());

        if (group == null) {
            throw new IllegalStateException("Service " + snapshot.serviceId() + " references unknown group " + snapshot.groupName());
        }

        return new CloudService(snapshot.serviceId(), group, scheduler, templateManager, Optional.of(snapshot));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

}
