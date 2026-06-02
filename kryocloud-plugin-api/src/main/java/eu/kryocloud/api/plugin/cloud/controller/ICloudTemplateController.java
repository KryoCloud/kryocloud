package eu.kryocloud.api.plugin.cloud.controller;

import eu.kryocloud.api.plugin.cloud.model.CloudTemplateSnapshot;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ICloudTemplateController {

    CompletableFuture<List<CloudTemplateSnapshot>> templates();

    CompletableFuture<Void> create(String name);

    CompletableFuture<Void> delete(String name);

    CompletableFuture<Void> copy(String source, String target);

    CompletableFuture<Void> sync(String name);

}
