package eu.kryocloud.node.console;

import eu.kryocloud.node.console.command.*;

public final class NodeConsoleCommands {

    private NodeConsoleCommands() {
    }

    public static CommandRegistry createDefaultRegistry() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new HelpCommand(registry));
        registry.register(new StopCommand());
        registry.register(new GroupsCommand());
        registry.register(new WrappersCommand());
        registry.register(new ServicesCommand());
        registry.register(new VersionCommand());
        registry.register(new StatsCommand());
        registry.register(new StartServiceCommand());
        return registry;
    }
}