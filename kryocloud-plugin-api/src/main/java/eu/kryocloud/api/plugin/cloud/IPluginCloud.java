package eu.kryocloud.api.plugin.cloud;

import eu.kryocloud.api.plugin.cloud.controller.ICloudGroupController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudMaintenanceController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudServiceController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudTemplateController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudVersionController;
import eu.kryocloud.api.plugin.cloud.controller.ICloudWrapperController;

public interface IPluginCloud {

    ICloudServiceController services();

    ICloudGroupController groups();

    ICloudWrapperController wrappers();

    ICloudTemplateController templates();

    ICloudVersionController versions();

    ICloudMaintenanceController maintenance();

}
