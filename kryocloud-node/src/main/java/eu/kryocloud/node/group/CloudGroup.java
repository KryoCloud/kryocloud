package eu.kryocloud.node.group;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.service.IService;
import eu.kryocloud.api.service.ServiceType;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record CloudGroup(UUID uniqueId, String name, String javaVersion, String templateName, String software, String softwareVersion, String bindAddress, ServiceType serviceType, Collection<IService> services, int serviceCount, int minCount, int maxCount, int minMemory, int maxMemory, int maxPlayers, int startNewPercent, int basePort, boolean staticServices, boolean installOnStart) implements IGroup {

    public CloudGroup {
        if (uniqueId == null) {
            throw new IllegalArgumentException("uniqueId must not be null");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (javaVersion == null || javaVersion.isBlank()) {
            throw new IllegalArgumentException("javaVersion must not be blank");
        }

        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("templateName must not be blank");
        }

        if (software == null || software.isBlank()) {
            throw new IllegalArgumentException("software must not be blank");
        }

        if (softwareVersion == null || softwareVersion.isBlank()) {
            throw new IllegalArgumentException("softwareVersion must not be blank");
        }

        if (bindAddress == null || bindAddress.isBlank()) {
            throw new IllegalArgumentException("bindAddress must not be blank");
        }

        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType must not be null");
        }

        if (services == null) {
            throw new IllegalArgumentException("services must not be null");
        }

        if (serviceCount < 0) {
            throw new IllegalArgumentException("serviceCount must not be negative");
        }

        if (minCount < 0) {
            throw new IllegalArgumentException("minCount must not be negative");
        }

        if (maxCount < minCount) {
            throw new IllegalArgumentException("maxCount must be greater than or equal to minCount");
        }

        if (minMemory < 1) {
            throw new IllegalArgumentException("minMemory must be greater than 0");
        }

        if (maxMemory < minMemory) {
            throw new IllegalArgumentException("maxMemory must be greater than or equal to minMemory");
        }

        if (maxPlayers < 1) {
            throw new IllegalArgumentException("maxPlayers must be greater than 0");
        }

        if (startNewPercent < 0 || startNewPercent > 100) {
            throw new IllegalArgumentException("startNewPercent must be between 0 and 100");
        }

        if (basePort < 0 || basePort > 65_535) {
            throw new IllegalArgumentException("basePort must be 0 or between 1 and 65535");
        }

        services = List.copyOf(services);
    }

    @Override
    public void stopServices() {
        for (IService service : services) {
            service.stop();
        }
    }
}
