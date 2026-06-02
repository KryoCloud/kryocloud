package eu.kryocloud.api.plugin.internal.remote;

import eu.kryocloud.api.plugin.cloud.controller.ICloudWrapperController;
import eu.kryocloud.api.plugin.cloud.model.CloudWrapperSnapshot;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.mapper.SnapshotMapper;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RemoteWrapperController implements ICloudWrapperController {

    private final PluginRequestClient client;

    public RemoteWrapperController(PluginRequestClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<List<CloudWrapperSnapshot>> wrappers() {
        return client.request("wrapper.list", Map.of()).thenApply(message -> SnapshotMapper.list(message.payload(), "wrappers").stream().map(SnapshotMapper::wrapper).toList());
    }

    @Override
    public CompletableFuture<List<CloudWrapperSnapshot>> availableWrappers() {
        return client.request("wrapper.available", Map.of()).thenApply(message -> SnapshotMapper.list(message.payload(), "wrappers").stream().map(SnapshotMapper::wrapper).toList());
    }

    @Override
    public CompletableFuture<Optional<CloudWrapperSnapshot>> wrapper(String name) {
        return client.request("wrapper.info", Map.of("wrapper", name)).thenApply(message -> Payload.bool(message.payload(), "present") ? Optional.of(SnapshotMapper.wrapper(message.payload())) : Optional.empty());
    }

    @Override
    public CompletableFuture<Void> cleanup(String wrapper, boolean dryRun) {
        return client.request("wrapper.cleanup", Payload.create().put("wrapper", wrapper).put("dryRun", dryRun).map()).thenApply(message -> null);
    }

    @Override
    public CompletableFuture<Void> cleanupAll(boolean dryRun) {
        return client.request("wrapper.cleanup.all", Payload.create().put("dryRun", dryRun).map()).thenApply(message -> null);
    }

}
