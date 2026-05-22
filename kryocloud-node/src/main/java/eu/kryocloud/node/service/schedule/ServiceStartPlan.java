package eu.kryocloud.node.service.schedule;

import eu.kryocloud.network.protocol.CloudServiceType;

public record ServiceStartPlan(String serviceId, String groupName, String templateName, CloudServiceType serviceType, int port, int maxMemoryMb, boolean staticService) {

    public ServiceStartPlan {
        if (serviceId == null) {
            throw new IllegalArgumentException("serviceId must not be null");
        }

        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        if (groupName == null) {
            throw new IllegalArgumentException("groupName must not be null");
        }

        if (groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }

        if (templateName == null) {
            throw new IllegalArgumentException("templateName must not be null");
        }

        if (templateName.isBlank()) {
            throw new IllegalArgumentException("templateName must not be blank");
        }

        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType must not be null");
        }

        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        if (maxMemoryMb < 1) {
            throw new IllegalArgumentException("maxMemoryMb must be greater than 0");
        }
    }
}