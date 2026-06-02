package eu.kryocloud.api.plugin.internal.remote;

import eu.kryocloud.api.plugin.cloud.controller.ICloudGroupController;
import eu.kryocloud.api.plugin.cloud.model.CloudGroupSnapshot;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.mapper.SnapshotMapper;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RemoteGroupController implements ICloudGroupController {

    private final PluginRequestClient client;

    public RemoteGroupController(PluginRequestClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<List<CloudGroupSnapshot>> groups() {
        return client.request("group.list", Map.of()).thenApply(message -> SnapshotMapper.list(message.payload(), "groups").stream().map(SnapshotMapper::group).toList());
    }

    @Override
    public CompletableFuture<Optional<CloudGroupSnapshot>> group(String name) {
        return client.request("group.info", Map.of("group", name)).thenApply(message -> Payload.bool(message.payload(), "present") ? Optional.of(SnapshotMapper.group(message.payload())) : Optional.empty());
    }

    @Override
    public CompletableFuture<Boolean> exists(String name) {
        return client.request("group.exists", Map.of("group", name)).thenApply(message -> Payload.bool(message.payload(), "exists"));
    }

    @Override
    public CompletableFuture<Void> scale(String group, int minOnline) {
        return client.request("group.scale", Payload.create().put("group", group).put("minOnline", minOnline).map()).thenApply(message -> null);
    }

    @Override
    public CompletableFuture<Void> reconcile(String group) {
        return client.request("group.reconcile", Map.of("group", group)).thenApply(message -> null);
    }

    @Override
    public CompletableFuture<Void> reconcileAll() {
        return client.request("group.reconcile.all", Map.of()).thenApply(message -> null);
    }

}
