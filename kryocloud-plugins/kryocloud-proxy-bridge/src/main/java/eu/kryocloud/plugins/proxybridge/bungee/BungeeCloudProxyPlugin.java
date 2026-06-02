package eu.kryocloud.plugins.proxybridge.bungee;

import eu.kryocloud.plugins.proxybridge.ProxyBridgeCore;
import eu.kryocloud.plugins.proxybridge.bungee.command.BungeeCloudCommand;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeeCloudProxyPlugin extends Plugin {

    private ProxyBridgeCore core;

    @Override
    public void onEnable() {
        BungeeProxyPlatform platform = new BungeeProxyPlatform(this);
        core = new ProxyBridgeCore(platform);
        getProxy().getPluginManager().registerCommand(this, new BungeeCloudCommand(core.commandBridge()));
        core.start();
    }

    @Override
    public void onDisable() {
        if (core == null) {
            return;
        }

        core.stop();
        core = null;
    }

}
