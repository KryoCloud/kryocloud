package eu.kryocloud.node.service.schedule;

import java.util.UUID;

public record ServiceStartResult(UUID requestId, String serviceId, String wrapperId) {

    public ServiceStartResult {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId must not be null");
        }

        if (serviceId == null) {
            throw new IllegalArgumentException("serviceId must not be null");
        }

        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        if (wrapperId == null) {
            throw new IllegalArgumentException("wrapperId must not be null");
        }

        if (wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }
    }
}