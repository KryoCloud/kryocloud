package eu.kryocloud.api.plugin.internal.remote;

import eu.kryocloud.api.plugin.cloud.controller.ICloudVersionController;
import eu.kryocloud.api.plugin.cloud.model.CloudVersionSnapshot;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.mapper.SnapshotMapper;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RemoteVersionController implements ICloudVersionController {

    private final PluginRequestClient client;

    public RemoteVersionController(PluginRequestClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<List<CloudVersionSnapshot>> versions() {
        return client.request("version.list", Map.of()).thenApply(message -> SnapshotMapper.list(message.payload(), "versions").stream().map(SnapshotMapper::version).toList());
    }

    @Override
    public CompletableFuture<Void> install(String name, String url) {
        return client.request("version.install", Payload.create().put("version", name).put("url", url).map()).thenApply(message -> null);
    }

    @Override
    public CompletableFuture<Void> refresh() {
        return client.request("version.refresh", Map.of()).thenApply(message -> null);
    }

}
