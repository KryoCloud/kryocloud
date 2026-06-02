package eu.kryocloud.api.plugin.internal.remote;

import eu.kryocloud.api.plugin.cloud.controller.ICloudServiceController;
import eu.kryocloud.api.plugin.cloud.model.CloudServiceSnapshot;
import eu.kryocloud.api.plugin.cloud.model.ServiceStartResult;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.mapper.SnapshotMapper;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RemoteServiceController implements ICloudServiceController {

    private final PluginRequestClient client;

    public RemoteServiceController(PluginRequestClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<ServiceStartResult> start(String group) {
        return client.request("service.start", Payload.create().put("group", group).put("count", 1).map()).thenApply(message -> SnapshotMapper.startResult(message.payload()));
    }

    @Override
    public CompletableFuture<List<ServiceStartResult>> start(String group, int count) {
        return client.request("service.start", Payload.create().put("group", group).put("count", count).map())
                .thenApply(message -> SnapshotMapper.list(message.payload(), "results").stream().map(SnapshotMapper::startResult).toList());
    }

    @Override
    public CompletableFuture<Void> stop(String service) {
        return simple("service.stop", Map.of("service", service));
    }

    @Override
    public CompletableFuture<Void> kill(String service) {
        return simple("service.kill", Map.of("service", service));
    }

    @Override
    public CompletableFuture<Void> restart(String service) {
        return simple("service.restart", Map.of("service", service));
    }

    @Override
    public CompletableFuture<Void> command(String service, String command) {
        return simple("service.command", Payload.create().put("service", service).put("command", command).map());
    }

    @Override
    public CompletableFuture<String> logs(String service, int tailLines) {
        return client.request("service.logs", Payload.create().put("service", service).put("tail", tailLines).map()).thenApply(message -> message.payload().getOrDefault("logs", ""));
    }

    @Override
    public CompletableFuture<List<CloudServiceSnapshot>> services() {
        return client.request("service.list", Map.of()).thenApply(message -> SnapshotMapper.list(message.payload(), "services").stream().map(SnapshotMapper::service).toList());
    }

    @Override
    public CompletableFuture<List<CloudServiceSnapshot>> services(String group) {
        return client.request("service.list", Map.of("group", group)).thenApply(message -> SnapshotMapper.list(message.payload(), "services").stream().map(SnapshotMapper::service).toList());
    }

    @Override
    public CompletableFuture<Optional<CloudServiceSnapshot>> service(String service) {
        return client.request("service.info", Map.of("service", service)).thenApply(message -> Payload.bool(message.payload(), "present") ? Optional.of(SnapshotMapper.service(message.payload())) : Optional.empty());
    }

    private CompletableFuture<Void> simple(String route, Map<String, String> payload) {
        return client.request(route, payload).thenApply(message -> null);
    }

}
