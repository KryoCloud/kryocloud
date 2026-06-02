package eu.kryocloud.api.plugin.internal.remote;

import eu.kryocloud.api.plugin.cloud.IPluginCloud;
import eu.kryocloud.api.plugin.cloud.controller.ICloudConsoleController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudGroupController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudMaintenanceController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudServiceController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudTemplateController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudVersionController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudWrapperController;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;

public final class RemotePluginCloud implements IPluginCloud {

    private final ICloudServiceController services;
    private final ICloudGroupController groups;
    private final ICloudWrapperController wrappers;
    private final ICloudTemplateController templates;
    private final ICloudVersionController versions;
    private final ICloudMaintenanceController maintenance;
    private final ICloudConsoleController console;

    public RemotePluginCloud(PluginRequestClient client) {
        this.services = new RemoteServiceController(client);
        this.groups = new RemoteGroupController(client);
        this.wrappers = new RemoteWrapperController(client);
        this.templates = new RemoteTemplateController(client);
        this.versions = new RemoteVersionController(client);
        this.maintenance = new RemoteMaintenanceController(client);
        this.console = new RemoteConsoleController(client);
    }

    @Override
    public ICloudServiceController services() {
        return services;
    }

    @Override
    public ICloudGroupController groups() {
        return groups;
    }

    @Override
    public ICloudWrapperController wrappers() {
        return wrappers;
    }

    @Override
    public ICloudTemplateController templates() {
        return templates;
    }

    @Override
    public ICloudVersionController versions() {
        return versions;
    }

    @Override
    public ICloudMaintenanceController maintenance() {
        return maintenance;
    }

    @Override
    public ICloudConsoleController console() {
        return console;
    }

}
