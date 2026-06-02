package eu.kryocloud.api.plugin.event.group;

import eu.kryocloud.api.plugin.cloud.model.CloudGroupSnapshot;
import eu.kryocloud.api.plugin.event.ICloudEvent;

public record GroupReconciledEvent(CloudGroupSnapshot group, int startedServices) implements ICloudEvent {
}
