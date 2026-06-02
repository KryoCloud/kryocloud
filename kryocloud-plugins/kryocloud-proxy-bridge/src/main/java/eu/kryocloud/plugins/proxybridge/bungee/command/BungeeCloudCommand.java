package eu.kryocloud.plugins.proxybridge.bungee.command;

import eu.kryocloud.plugins.proxybridge.command.CloudCommandAudience;
import eu.kryocloud.plugins.proxybridge.command.CloudCommandBridge;
import java.util.Arrays;
import java.util.List;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public final class BungeeCloudCommand extends Command implements TabExecutor {

    private final CloudCommandBridge bridge;

    public BungeeCloudCommand(CloudCommandBridge bridge) {
        super("cloud", null, "kryocloud");
        this.bridge = bridge;
    }

    @Override
    public void execute(CommandSender sender, String[] arguments) {
        bridge.execute(new BungeeAudience(sender), Arrays.asList(arguments));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] arguments) {
        return bridge.suggest(new BungeeAudience(sender), Arrays.asList(arguments));
    }

    private record BungeeAudience(CommandSender sender) implements CloudCommandAudience {

        @Override
        public boolean hasPermission(String permission) {
            return sender.hasPermission(permission);
        }

        @Override
        public void sendMessage(String message) {
            sender.sendMessage(TextComponent.fromLegacyText(message));
        }

    }

}
