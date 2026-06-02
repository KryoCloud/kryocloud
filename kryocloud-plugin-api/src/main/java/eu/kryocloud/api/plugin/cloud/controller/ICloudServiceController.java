package eu.kryocloud.api.plugin.cloud.controller;

import eu.kryocloud.api.plugin.cloud.model.CloudServiceSnapshot;
import eu.kryocloud.api.plugin.cloud.model.ServiceStartResult;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ICloudServiceController {

    CompletableFuture<ServiceStartResult> start(String group);

    CompletableFuture<List<ServiceStartResult>> start(String group, int count);

    CompletableFuture<Void> stop(String service);

    CompletableFuture<Void> kill(String service);

    CompletableFuture<Void> restart(String service);

    CompletableFuture<Void> command(String service, String command);

    CompletableFuture<String> logs(String service, int tailLines);

    CompletableFuture<List<CloudServiceSnapshot>> services();

    CompletableFuture<List<CloudServiceSnapshot>> services(String group);

    CompletableFuture<Optional<CloudServiceSnapshot>> service(String service);

}
