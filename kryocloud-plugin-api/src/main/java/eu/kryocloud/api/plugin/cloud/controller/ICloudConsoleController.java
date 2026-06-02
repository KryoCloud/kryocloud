package eu.kryocloud.api.plugin.cloud.controller;

import eu.kryocloud.api.plugin.cloud.model.CloudCommandResult;
import eu.kryocloud.api.plugin.cloud.model.CloudCommandSpec;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ICloudConsoleController {

    CompletableFuture<CloudCommandResult> execute(List<String> arguments);

    CompletableFuture<List<String>> suggest(List<String> arguments);

    CompletableFuture<List<CloudCommandSpec>> commands();

}
