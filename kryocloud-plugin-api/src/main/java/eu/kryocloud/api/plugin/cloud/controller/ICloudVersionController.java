package eu.kryocloud.api.plugin.cloud.controller;

import eu.kryocloud.api.plugin.cloud.model.CloudVersionSnapshot;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ICloudVersionController {

    CompletableFuture<List<CloudVersionSnapshot>> versions();

    CompletableFuture<Void> install(String name, String url);

    CompletableFuture<Void> refresh();

}
