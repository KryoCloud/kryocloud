package eu.kryocloud.api.plugin.internal.event;

import eu.kryocloud.api.plugin.cloud.model.CloudServiceSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudServiceState;
import eu.kryocloud.api.plugin.cloud.model.CloudWrapperSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudWrapperState;
import eu.kryocloud.api.plugin.event.ICloudEvent;
import eu.kryocloud.api.plugin.event.group.GroupCreatedEvent;
import eu.kryocloud.api.plugin.event.group.GroupDeletedEvent;
import eu.kryocloud.api.plugin.event.group.GroupReconcileAllEvent;
import eu.kryocloud.api.plugin.event.group.GroupReconciledEvent;
import eu.kryocloud.api.plugin.event.group.GroupScaledEvent;
import eu.kryocloud.api.plugin.event.group.GroupUpdatedEvent;
import eu.kryocloud.api.plugin.event.lifecycle.CloudReadyEvent;
import eu.kryocloud.api.plugin.event.lifecycle.CloudStoppingEvent;
import eu.kryocloud.api.plugin.event.maintenance.MaintenanceDisabledEvent;
import eu.kryocloud.api.plugin.event.maintenance.MaintenanceEnabledEvent;
import eu.kryocloud.api.plugin.event.remote.RemoteCloudEvent;
import eu.kryocloud.api.plugin.event.service.ServiceCommandSentEvent;
import eu.kryocloud.api.plugin.event.service.ServiceFailedEvent;
import eu.kryocloud.api.plugin.event.service.ServiceMetricsUpdatedEvent;
import eu.kryocloud.api.plugin.event.service.ServicePreparingEvent;
import eu.kryocloud.api.plugin.event.service.ServiceStartedEvent;
import eu.kryocloud.api.plugin.event.service.ServiceStartingEvent;
import eu.kryocloud.api.plugin.event.service.ServiceStateChangedEvent;
import eu.kryocloud.api.plugin.event.service.ServiceStoppedEvent;
import eu.kryocloud.api.plugin.event.service.ServiceStoppingEvent;
import eu.kryocloud.api.plugin.event.template.TemplateCopiedEvent;
import eu.kryocloud.api.plugin.event.template.TemplateCreatedEvent;
import eu.kryocloud.api.plugin.event.template.TemplateDeletedEvent;
import eu.kryocloud.api.plugin.event.template.TemplateSyncedEvent;
import eu.kryocloud.api.plugin.event.version.VersionInstalledEvent;
import eu.kryocloud.api.plugin.event.version.VersionRefreshedEvent;
import eu.kryocloud.api.plugin.event.wrapper.WrapperConnectedEvent;
import eu.kryocloud.api.plugin.event.wrapper.WrapperDisconnectedEvent;
import eu.kryocloud.api.plugin.event.wrapper.WrapperHeartbeatEvent;
import eu.kryocloud.api.plugin.event.wrapper.WrapperStateChangedEvent;
import eu.kryocloud.api.plugin.internal.mapper.SnapshotMapper;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import java.util.Map;

public final class RemoteEventMapper {

    private RemoteEventMapper() {
    }

    public static ICloudEvent map(String route, Map<String, String> payload) {
        String event = route == null ? "" : route;

        if (event.equals(CloudReadyEvent.class.getName())) {
            return new CloudReadyEvent();
        }

        if (event.equals(CloudStoppingEvent.class.getName())) {
            return new CloudStoppingEvent(payload.getOrDefault("reason", ""));
        }

        if (event.equals(ServiceStateChangedEvent.class.getName())) {
            return new ServiceStateChangedEvent(SnapshotMapper.service(payload), SnapshotMapper.enumValue(CloudServiceState.class, payload.get("oldState"), CloudServiceState.UNKNOWN), SnapshotMapper.enumValue(CloudServiceState.class, payload.get("newState"), CloudServiceState.UNKNOWN));
        }

        if (event.equals(ServiceMetricsUpdatedEvent.class.getName())) {
            return new ServiceMetricsUpdatedEvent(SnapshotMapper.service(payload), Payload.integer(payload, "cpuLoadPermille") / 1000.0D, Payload.longValue(payload, "memoryMb") * 1024L * 1024L, Payload.longValue(payload, "maxMemoryMb") * 1024L * 1024L);
        }

        if (event.equals(ServicePreparingEvent.class.getName())) {
            return new ServicePreparingEvent(SnapshotMapper.service(payload));
        }

        if (event.equals(ServiceStartingEvent.class.getName())) {
            return new ServiceStartingEvent(SnapshotMapper.service(payload));
        }

        if (event.equals(ServiceStartedEvent.class.getName())) {
            return new ServiceStartedEvent(SnapshotMapper.service(payload));
        }

        if (event.equals(ServiceStoppingEvent.class.getName())) {
            return new ServiceStoppingEvent(SnapshotMapper.service(payload));
        }

        if (event.equals(ServiceStoppedEvent.class.getName())) {
            return new ServiceStoppedEvent(SnapshotMapper.service(payload));
        }

        if (event.equals(ServiceFailedEvent.class.getName())) {
            return new ServiceFailedEvent(SnapshotMapper.service(payload), payload.getOrDefault("reason", payload.getOrDefault("message", "")));
        }

        if (event.equals(ServiceCommandSentEvent.class.getName())) {
            return new ServiceCommandSentEvent(payload.getOrDefault("service", ""), payload.getOrDefault("command", ""));
        }

        if (event.equals(WrapperConnectedEvent.class.getName())) {
            return new WrapperConnectedEvent(SnapshotMapper.wrapper(payload));
        }

        if (event.equals(WrapperDisconnectedEvent.class.getName())) {
            return new WrapperDisconnectedEvent(SnapshotMapper.wrapper(payload), payload.getOrDefault("reason", ""));
        }

        if (event.equals(WrapperHeartbeatEvent.class.getName())) {
            return new WrapperHeartbeatEvent(SnapshotMapper.wrapper(payload));
        }

        if (event.equals(WrapperStateChangedEvent.class.getName())) {
            CloudWrapperSnapshot wrapper = SnapshotMapper.wrapper(payload);
            return new WrapperStateChangedEvent(wrapper, SnapshotMapper.enumValue(CloudWrapperState.class, payload.get("oldState"), CloudWrapperState.UNKNOWN), SnapshotMapper.enumValue(CloudWrapperState.class, payload.get("newState"), wrapper.state()));
        }

        if (event.equals(GroupCreatedEvent.class.getName())) {
            return new GroupCreatedEvent(SnapshotMapper.group(payload));
        }

        if (event.equals(GroupDeletedEvent.class.getName())) {
            return new GroupDeletedEvent(SnapshotMapper.group(payload));
        }

        if (event.equals(GroupUpdatedEvent.class.getName())) {
            return new GroupUpdatedEvent(SnapshotMapper.group(payload));
        }

        if (event.equals(GroupScaledEvent.class.getName())) {
            return new GroupScaledEvent(SnapshotMapper.group(payload), Payload.integer(payload, "oldMinOnline"), Payload.integer(payload, "newMinOnline"));
        }

        if (event.equals(GroupReconciledEvent.class.getName())) {
            return new GroupReconciledEvent(SnapshotMapper.group(payload), Payload.integer(payload, "startedServices"));
        }

        if (event.equals(GroupReconcileAllEvent.class.getName())) {
            return new GroupReconcileAllEvent(Payload.integer(payload, "startedServices"));
        }

        if (event.equals(TemplateCreatedEvent.class.getName())) {
            return new TemplateCreatedEvent(SnapshotMapper.template(payload));
        }

        if (event.equals(TemplateDeletedEvent.class.getName())) {
            return new TemplateDeletedEvent(SnapshotMapper.template(payload));
        }

        if (event.equals(TemplateSyncedEvent.class.getName())) {
            return new TemplateSyncedEvent(SnapshotMapper.template(payload));
        }

        if (event.equals(TemplateCopiedEvent.class.getName())) {
            return new TemplateCopiedEvent(SnapshotMapper.template(prefixed(payload, "source.")), SnapshotMapper.template(prefixed(payload, "target.")));
        }

        if (event.equals(VersionInstalledEvent.class.getName())) {
            return new VersionInstalledEvent(SnapshotMapper.version(payload));
        }

        if (event.equals(VersionRefreshedEvent.class.getName())) {
            return new VersionRefreshedEvent();
        }

        if (event.equals(MaintenanceEnabledEvent.class.getName())) {
            return new MaintenanceEnabledEvent(payload.getOrDefault("reason", ""));
        }

        if (event.equals(MaintenanceDisabledEvent.class.getName())) {
            return new MaintenanceDisabledEvent();
        }

        return new RemoteCloudEvent(route, payload);
    }

    private static Map<String, String> prefixed(Map<String, String> payload, String prefix) {
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();

        for (Map.Entry<String, String> entry : payload.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }

            values.put(entry.getKey().substring(prefix.length()), entry.getValue());
        }

        return Map.copyOf(values);
    }

}
