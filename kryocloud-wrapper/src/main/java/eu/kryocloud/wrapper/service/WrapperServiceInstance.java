package eu.kryocloud.wrapper.service;

import eu.kryocloud.network.protocol.CloudServiceState;
import eu.kryocloud.network.protocol.CloudServiceType;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public record WrapperServiceInstance(UUID requestId, String serviceId, String groupName, String templateName, CloudServiceType serviceType, int port, int maxMemoryMb, boolean staticService, Path workingDirectory, Process process, CloudServiceState state, Instant startedAt) {

    public WrapperServiceInstance {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId must not be null");
        }

        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }

        if (templateName == null || templateName.isBlank()) {
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

        if (workingDirectory == null) {
            throw new IllegalArgumentException("workingDirectory must not be null");
        }

        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }

        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null");
        }
    }

    public boolean alive() {
        return process != null && process.isAlive();
    }

    public WrapperServiceInstance withState(CloudServiceState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("newState must not be null");
        }

        return new WrapperServiceInstance(requestId, serviceId, groupName, templateName, serviceType, port, maxMemoryMb, staticService, workingDirectory, process, newState, startedAt);
    }
}