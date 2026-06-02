package eu.kryocloud.plugins.proxybridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.kryocloud.plugins.proxybridge.ProxyBridgeCore;
import eu.kryocloud.plugins.proxybridge.velocity.command.VelocityCloudCommand;
import org.slf4j.Logger;

@Plugin(
        id = "kryocloud-proxy-bridge",
        name = "KryoCloudProxyBridge",
        version = "1.0.0-alpha.1",
        description = "Connects Velocity proxies with KryoCloud services",
        authors = "KryoCloud"
)
public final class VelocityCloudProxyPlugin {

    private final ProxyServer proxy;
    private final ProxyBridgeCore core;

    @Inject
    public VelocityCloudProxyPlugin(ProxyServer proxy, Logger logger) {
        VelocityProxyPlatform platform = new VelocityProxyPlatform(this, proxy, logger);
        this.proxy = proxy;
        this.core = new ProxyBridgeCore(platform);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        registerCommand();
        core.start();
    }

    private void registerCommand() {
        CommandMeta meta = proxy.getCommandManager()
                .metaBuilder("cloud")
                .aliases("kryocloud")
                .plugin(this)
                .build();

        proxy.getCommandManager().register(meta, new VelocityCloudCommand(core.commandBridge()));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        core.stop();
    }

}
