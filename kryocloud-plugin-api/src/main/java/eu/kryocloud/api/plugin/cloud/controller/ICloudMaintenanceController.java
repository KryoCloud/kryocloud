package eu.kryocloud.api.plugin.cloud.controller;

import eu.kryocloud.api.plugin.cloud.model.CloudStatsSnapshot;
import java.util.concurrent.CompletableFuture;

public interface ICloudMaintenanceController {

    CompletableFuture<Boolean> enabled();

    CompletableFuture<Void> enable(String reason);

    CompletableFuture<Void> disable();

    CompletableFuture<CloudStatsSnapshot> stats();

    CompletableFuture<Void> shutdown(String reason);

}
