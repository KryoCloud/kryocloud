package eu.kryocloud.node.console.command;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;

import java.util.Collection;
import java.util.List;

public final class GroupsCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "groups";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CLUSTER;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("group", "tasks");
    }

    @Override
    public String description() {
        return "Lists or inspects configured cloud groups";
    }

    @Override
    public String usage() {
        return "groups [list|info <groupName>]";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            list(context);
            return;
        }

        if ("list".equalsIgnoreCase(arguments.getFirst())) {
            list(context);
            return;
        }

        if ("info".equalsIgnoreCase(arguments.getFirst())) {
            info(context, arguments);
            return;
        }

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void list(ConsoleContext context) {
        Collection<IGroup> groups = context.node().groupManager().groups();

        if (groups.isEmpty()) {
            context.warn("No groups configured.");
            return;
        }

        context.header("Cloud groups");

        for (IGroup group : groups) {
            context.print(" • " + group.name() + " | " + group.serviceType() + " | template=" + group.templateName() + " | memory=" + group.minMemory() + "-" + group.maxMemory() + "MB | count=" + group.minCount() + "-" + group.maxCount());
        }
    }

    private void info(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: groups info <groupName>");
        }

        String groupName = arguments.get(1);
        IGroup group = context.node().groupManager().groupByName(groupName);

        if (group == null) {
            throw new IllegalArgumentException("Unknown group: " + groupName);
        }

        context.header("Group " + group.name());
        context.print("  UniqueId: " + group.uniqueId());
        context.print("  Java: " + group.javaVersion());
        context.print("  Template: " + group.templateName());
        context.print("  Type: " + group.serviceType());
        context.print("  Count: " + group.minCount() + "-" + group.maxCount());
        context.print("  Memory: " + group.minMemory() + "-" + group.maxMemory() + "MB");
        context.print("  Max players: " + group.maxPlayers());
        context.print("  Start new percent: " + group.startNewPercent());
    }
}