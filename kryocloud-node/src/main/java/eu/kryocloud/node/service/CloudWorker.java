package eu.kryocloud.node.service;

import eu.kryocloud.api.wrapper.IWrapper;

public record CloudWorker(String wrapperId) implements IWrapper {

    public CloudWorker {
        if (wrapperId == null || wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }
    }

}
