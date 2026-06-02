package eu.kryocloud.plugins.proxybridge;

import eu.kryocloud.api.plugin.ICloudPlugin;
import eu.kryocloud.api.plugin.context.PluginContext;
import eu.kryocloud.api.plugin.description.PluginDescription;

public final class CloudProxyBridgeExtension implements ICloudPlugin {

    private final ProxyBridgeCore core;

    public CloudProxyBridgeExtension(ProxyBridgeCore core) {
        if (core == null) {
            throw new IllegalArgumentException("core must not be null");
        }

        this.core = core;
    }

    @Override
    public PluginDescription description() {
        return PluginDescription.builder()
                .id("kryocloud-proxy-bridge")
                .name("KryoCloudProxyBridge")
                .version("1.0.0-alpha.1")
                .description("Connects Minecraft proxies with KryoCloud services")
                .author("KryoCloud")
                .build();
    }

    @Override
    public void enable(PluginContext context) {
        core.enable(context);
    }

    @Override
    public void disable(PluginContext context) {
        core.disable();
    }

}
