package eu.kryocloud.api.plugin.event.maintenance;

import eu.kryocloud.api.plugin.event.ICloudEvent;

public record MaintenanceEnabledEvent(String reason) implements ICloudEvent {
}
