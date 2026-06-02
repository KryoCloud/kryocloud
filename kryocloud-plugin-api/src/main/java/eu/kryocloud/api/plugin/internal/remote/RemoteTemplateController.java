package eu.kryocloud.api.plugin.internal.remote;

import eu.kryocloud.api.plugin.cloud.controller.ICloudTemplateController;
import eu.kryocloud.api.plugin.cloud.model.CloudTemplateSnapshot;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.mapper.SnapshotMapper;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RemoteTemplateController implements ICloudTemplateController {

    private final PluginRequestClient client;

    public RemoteTemplateController(PluginRequestClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<List<CloudTemplateSnapshot>> templates() {
        return client.request("template.list", Map.of()).thenApply(message -> SnapshotMapper.list(message.payload(), "templates").stream().map(SnapshotMapper::template).toList());
    }

    @Override
    public CompletableFuture<Void> create(String name) {
        return client.request("template.create", Map.of("template", name)).thenApply(message -> null);
    }

    @Override
    public CompletableFuture<Void> delete(String name) {
        return client.request("template.delete", Map.of("template", name)).thenApply(message -> null);
    }

    @Override
    public CompletableFuture<Void> copy(String source, String target) {
        return client.request("template.copy", Payload.create().put("source", source).put("target", target).map()).thenApply(message -> null);
    }

    @Override
    public CompletableFuture<Void> sync(String name) {
        return client.request("template.sync", Map.of("template", name)).thenApply(message -> null);
    }

}
