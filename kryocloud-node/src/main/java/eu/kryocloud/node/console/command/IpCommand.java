package eu.kryocloud.node.console.command;

import eu.kryocloud.node.config.network.NetworkAddressConfig;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.tui.ConsoleTheme;

import java.util.Collection;
import java.util.List;

public final class IpCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "ip";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CLUSTER;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("ips", "address", "addresses", "bind");
    }

    @Override
    public String description() {
        return "Manages Minecraft bind addresses";
    }

    @Override
    public String usage() {
        return "ip list | ip add <server|proxy> <address> | ip remove <server|proxy> <address> | ip default <server|proxy> <address>";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            list(context);
            return;
        }

        String action = arguments.getFirst();

        if ("list".equalsIgnoreCase(action)) {
            list(context);
            return;
        }

        if ("add".equalsIgnoreCase(action)) {
            add(context, arguments);
            return;
        }

        if ("remove".equalsIgnoreCase(action) || "delete".equalsIgnoreCase(action)) {
            remove(context, arguments);
            return;
        }

        if ("default".equalsIgnoreCase(action)) {
            setDefault(context, arguments);
            return;
        }

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void list(ConsoleContext context) {
        NetworkAddressConfig config = context.node().networkAddressConfig();

        context.header("Minecraft bind addresses");
        context.row("Server default", config.getDefaultServerAddress());
        context.row("Proxy default", config.getDefaultProxyAddress());
        context.print("");
        context.print("  " + ConsoleTheme.accent("Server addresses"));

        for (String address : config.getServerAddresses()) {
            context.print("   " + ConsoleTheme.bullet() + " " + context.value(address));
        }

        context.print("");
        context.print("  " + ConsoleTheme.accent("Proxy addresses"));

        for (String address : config.getProxyAddresses()) {
            context.print("   " + ConsoleTheme.bullet() + " " + context.value(address));
        }
    }

    private void add(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 3) {
            throw new IllegalArgumentException("Usage: ip add <server|proxy> <address>");
        }

        NetworkAddressConfig config = context.node().networkAddressConfig();
        String type = arguments.get(1);
        String address = arguments.get(2);

        if (serverType(type)) {
            config.addServerAddress(address);
            config.save();
            context.success("Added server bind address " + address);
            return;
        }

        if (proxyType(type)) {
            config.addProxyAddress(address);
            config.save();
            context.success("Added proxy bind address " + address);
            return;
        }

        throw new IllegalArgumentException("type must be server or proxy");
    }

    private void remove(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 3) {
            throw new IllegalArgumentException("Usage: ip remove <server|proxy> <address>");
        }

        NetworkAddressConfig config = context.node().networkAddressConfig();
        String type = arguments.get(1);
        String address = arguments.get(2);
        boolean removed;

        if (serverType(type)) {
            removed = config.removeServerAddress(address);
            config.save();

            if (removed) {
                context.success("Removed server bind address " + address);
                return;
            }

            context.warn("Server bind address was not registered: " + address);
            return;
        }

        if (proxyType(type)) {
            removed = config.removeProxyAddress(address);
            config.save();

            if (removed) {
                context.success("Removed proxy bind address " + address);
                return;
            }

            context.warn("Proxy bind address was not registered: " + address);
            return;
        }

        throw new IllegalArgumentException("type must be server or proxy");
    }

    private void setDefault(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 3) {
            throw new IllegalArgumentException("Usage: ip default <server|proxy> <address>");
        }

        NetworkAddressConfig config = context.node().networkAddressConfig();
        String type = arguments.get(1);
        String address = arguments.get(2);

        if (serverType(type)) {
            config.setDefaultServerAddress(address);
            config.save();
            context.success("Default server bind address is now " + address);
            return;
        }

        if (proxyType(type)) {
            config.setDefaultProxyAddress(address);
            config.save();
            context.success("Default proxy bind address is now " + address);
            return;
        }

        throw new IllegalArgumentException("type must be server or proxy");
    }

    private boolean serverType(String type) {
        return "server".equalsIgnoreCase(type) || "local".equalsIgnoreCase(type) || "backend".equalsIgnoreCase(type);
    }

    private boolean proxyType(String type) {
        return "proxy".equalsIgnoreCase(type) || "public".equalsIgnoreCase(type);
    }
}
