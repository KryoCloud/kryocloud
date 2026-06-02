package eu.kryocloud.api.plugin.internal.remote;

import eu.kryocloud.api.plugin.cloud.controller.ICloudConsoleController;
import eu.kryocloud.api.plugin.cloud.model.CloudCommandResult;
import eu.kryocloud.api.plugin.cloud.model.CloudCommandSpec;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.mapper.SnapshotMapper;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RemoteConsoleController implements ICloudConsoleController {

    private final PluginRequestClient client;

    public RemoteConsoleController(PluginRequestClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<CloudCommandResult> execute(List<String> arguments) {
        return client.request("console.execute", payload(arguments)).thenApply(message -> {
            Map<String, String> payload = message.payload();
            boolean success = Payload.bool(payload, "success");
            String resultMessage = payload.getOrDefault("message", "");
            List<String> lines = SnapshotMapper.values(payload, "lines");
            return new CloudCommandResult(success, resultMessage, lines);
        });
    }

    @Override
    public CompletableFuture<List<String>> suggest(List<String> arguments) {
        return client.request("console.suggest", payload(arguments)).thenApply(message -> SnapshotMapper.values(message.payload(), "suggestions"));
    }

    @Override
    public CompletableFuture<List<CloudCommandSpec>> commands() {
        return client.request("console.commands", Map.of()).thenApply(message -> SnapshotMapper.list(message.payload(), "commands").stream().map(this::command).toList());
    }

    private CloudCommandSpec command(Map<String, String> payload) {
        return new CloudCommandSpec(
                payload.getOrDefault("name", ""),
                payload.getOrDefault("description", ""),
                payload.getOrDefault("usage", ""),
                Payload.bool(payload, "executable"),
                Payload.bool(payload, "cliOnly"),
                SnapshotMapper.values(payload, "aliases")
        );
    }

    private Map<String, String> payload(List<String> arguments) {
        Payload payload = Payload.create();
        List<String> values = arguments == null ? List.of() : arguments;
        payload.put("input", String.join(" ", values));
        payload.put("arguments.size", values.size());

        for (int index = 0; index < values.size(); index++) {
            payload.put("arguments." + index, values.get(index));
        }

        return payload.map();
    }

}
