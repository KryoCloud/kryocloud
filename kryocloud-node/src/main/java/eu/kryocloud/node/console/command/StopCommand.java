package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;

import java.util.Collection;
import java.util.List;

public final class StopCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "stop";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CORE;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("shutdown", "exit");
    }

    @Override
    public String description() {
        return "Gracefully stops the KryoCloud node";
    }

    @Override
    public String usage() {
        return "stop";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        context.warn("Stopping KryoCloud node...");
        context.stopConsole();
        context.node().shutdown();
    }
}