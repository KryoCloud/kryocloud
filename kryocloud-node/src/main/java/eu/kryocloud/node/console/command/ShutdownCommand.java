package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;

import java.util.Collection;
import java.util.List;

public final class ShutdownCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "shutdown";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CORE;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("stop", "exit", "quit");
    }

    @Override
    public String description() {
        return "Gracefully shuts down KryoCloud";
    }

    @Override
    public String usage() {
        return "shutdown";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        context.warn("Shutting down KryoCloud...");
        context.stopConsole();
        context.node().shutdown();
    }
}
