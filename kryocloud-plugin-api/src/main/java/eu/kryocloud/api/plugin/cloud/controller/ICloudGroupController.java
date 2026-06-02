package eu.kryocloud.api.plugin.cloud.controller;

import eu.kryocloud.api.plugin.cloud.model.CloudGroupSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ICloudGroupController {

    CompletableFuture<List<CloudGroupSnapshot>> groups();

    CompletableFuture<Optional<CloudGroupSnapshot>> group(String name);

    CompletableFuture<Boolean> exists(String name);

    CompletableFuture<Void> scale(String group, int minOnline);

    CompletableFuture<Void> reconcile(String group);

    CompletableFuture<Void> reconcileAll();

}
