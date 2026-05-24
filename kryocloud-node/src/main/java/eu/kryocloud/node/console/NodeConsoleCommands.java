package eu.kryocloud.node.console;

import eu.kryocloud.node.console.command.GroupsCommand;
import eu.kryocloud.node.console.command.HelpCommand;
import eu.kryocloud.node.console.command.ServicesCommand;
import eu.kryocloud.node.console.command.StatsCommand;
import eu.kryocloud.node.console.command.StopCommand;
import eu.kryocloud.node.console.command.VersionCommand;
import eu.kryocloud.node.console.command.WrappersCommand;

public final class NodeConsoleCommands {

    private NodeConsoleCommands() {
    }

    public static CommandRegistry createDefaultRegistry() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new HelpCommand(registry));
        registry.register(new StopCommand());
        registry.register(new GroupsCommand());
        registry.register(new ServicesCommand());
        registry.register(new WrappersCommand());
        registry.register(new VersionCommand());
        registry.register(new StatsCommand());
        return registry;
    }
}
