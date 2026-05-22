package eu.kryocloud.node.service.runtime;

import eu.kryocloud.network.protocol.CloudServiceState;
import eu.kryocloud.network.protocol.CloudServiceType;

public record NodeServiceSnapshot(String serviceId, String groupName, CloudServiceType serviceType, CloudServiceState state, String wrapperId, String host, int port, long timestamp, String message) {

    public NodeServiceSnapshot {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }

        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType must not be null");
        }

        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }

        if (wrapperId == null || wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }

        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        if (timestamp < 1) {
            throw new IllegalArgumentException("timestamp must be greater than 0");
        }

        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
    }

    public boolean running() {
        return state == CloudServiceState.RUNNING;
    }
}