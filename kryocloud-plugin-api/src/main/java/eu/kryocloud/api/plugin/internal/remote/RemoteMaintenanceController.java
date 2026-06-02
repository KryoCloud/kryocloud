package eu.kryocloud.api.plugin.internal.remote;

import eu.kryocloud.api.plugin.cloud.controller.ICloudMaintenanceController;
import eu.kryocloud.api.plugin.cloud.model.CloudStatsSnapshot;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.mapper.SnapshotMapper;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RemoteMaintenanceController implements ICloudMaintenanceController {

    private final PluginRequestClient client;

    public RemoteMaintenanceController(PluginRequestClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<Boolean> enabled() {
        return client.request("maintenance.enabled", Map.of()).thenApply(message -> Payload.bool(message.payload(), "enabled"));
    }

    @Override
    public CompletableFuture<Void> enable(String reason) {
        return client.request("maintenance.enable", Map.of("reason", reason)).thenApply(message -> null);
    }

    @Override
    public CompletableFuture<Void> disable() {
        return client.request("maintenance.disable", Map.of()).thenApply(message -> null);
    }

    @Override
    public CompletableFuture<CloudStatsSnapshot> stats() {
        return client.request("cloud.stats", Map.of()).thenApply(message -> SnapshotMapper.stats(message.payload()));
    }

    @Override
    public CompletableFuture<Void> shutdown(String reason) {
        return client.request("cloud.shutdown", Map.of("reason", reason)).thenApply(message -> null);
    }

}
