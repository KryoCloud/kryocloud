package eu.kryocloud.api.plugin.event.group;

import eu.kryocloud.api.plugin.event.ICloudEvent;

public record GroupReconcileAllEvent(int startedServices) implements ICloudEvent {
}
