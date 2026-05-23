package eu.kryocloud.node.console.command;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.node.config.group.GroupConfig;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.wizard.GroupSetupWizard;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class GroupsCommand implements ConsoleCommand {

    private final GroupSetupWizard setupWizard = new GroupSetupWizard();

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
        return "Lists, inspects or creates Minecraft groups";
    }

    @Override
    public String usage() {
        return "groups [list|info <groupName>|create]";
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

        if ("info".equalsIgnoreCase(action)) {
            info(context, arguments);
            return;
        }

        if ("create".equalsIgnoreCase(action)) {
            setupWizard.run(context);
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

        context.header("Minecraft groups");

        for (IGroup group : groups) {
            context.print(" • " + context.accent(group.name()) + " | " + group.serviceType() + " | template=" + group.templateName() + " | software=" + group.software() + " " + group.softwareVersion() + " | services=" + group.serviceCount());
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

        Optional<GroupConfig> optionalConfig = context.node().groupManager().config(group.name());

        context.header("Group " + group.name());
        context.print("  UniqueId: " + group.uniqueId());
        context.print("  Java: " + group.javaVersion());
        context.print("  Template: " + group.templateName());
        context.print("  Software: " + group.software() + " " + group.softwareVersion());
        context.print("  Type: " + group.serviceType());
        context.print("  Services: " + group.serviceCount());
        context.print("  Count: " + group.minCount() + "-" + group.maxCount());
        context.print("  Memory: " + group.minMemory() + "-" + group.maxMemory() + "MB");
        context.print("  Max players: " + group.maxPlayers());
        context.print("  Start new percent: " + group.startNewPercent());
        context.print("  Base port: " + group.basePort());
        context.print("  Static services: " + group.staticServices());
        context.print("  Install on start: " + group.installOnStart());

        if (optionalConfig.isEmpty()) {
            return;
        }

        context.print("  Config: " + optionalConfig.get().file());
    }
}
