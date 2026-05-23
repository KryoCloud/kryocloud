package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.CommandRegistry;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HelpCommand implements ConsoleCommand {

    private final CommandRegistry commandRegistry;

    public HelpCommand(CommandRegistry commandRegistry) {
        if (commandRegistry == null) {
            throw new IllegalArgumentException("commandRegistry must not be null");
        }

        this.commandRegistry = commandRegistry;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CORE;
    }

    @Override
    public String description() {
        return "Displays all available commands and descriptions";
    }

    @Override
    public String usage() {
        return "help";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        Map<ConsoleCategory, List<ConsoleCommand>> commandsByCategory = commandRegistry.commands().stream().collect(Collectors.groupingBy(ConsoleCommand::category));

        for (ConsoleCategory category : ConsoleCategory.values()) {
            List<ConsoleCommand> commands = commandsByCategory.get(category);

            if (commands == null || commands.isEmpty()) {
                continue;
            }

            context.header(category.displayName());

            for (ConsoleCommand command : commands.stream().sorted(Comparator.comparing(ConsoleCommand::name)).toList()) {
                context.bullet(context.code(command.usage()) + context.muted("  —  ") + command.description());
            }
        }
    }
}
