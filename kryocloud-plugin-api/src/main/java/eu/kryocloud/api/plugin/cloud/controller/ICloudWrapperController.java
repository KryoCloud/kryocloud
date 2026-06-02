package eu.kryocloud.api.plugin.cloud.controller;

import eu.kryocloud.api.plugin.cloud.model.CloudWrapperSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ICloudWrapperController {

    CompletableFuture<List<CloudWrapperSnapshot>> wrappers();

    CompletableFuture<List<CloudWrapperSnapshot>> availableWrappers();

    CompletableFuture<Optional<CloudWrapperSnapshot>> wrapper(String name);

    CompletableFuture<Void> cleanup(String wrapper, boolean dryRun);

    CompletableFuture<Void> cleanupAll(boolean dryRun);

}
