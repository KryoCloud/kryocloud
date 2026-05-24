package eu.kryocloud.node.console.command;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.node.config.group.GroupConfig;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.tui.ConsoleAnimation;
import eu.kryocloud.node.console.tui.ConsoleTheme;
import eu.kryocloud.node.console.wizard.GroupSetupWizard;
import eu.kryocloud.node.service.schedule.ServiceStartResult;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class GroupsCommand implements ConsoleCommand {

    private final GroupSetupWizard setupWizard = new GroupSetupWizard();
    private final ConsoleAnimation animation = new ConsoleAnimation();

    @Override
    public String name() {
        return "group";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.GROUP;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("groups", "task", "tasks");
    }

    @Override
    public String description() {
        return "Creates and controls Minecraft groups";
    }

    @Override
    public String usage() {
        return "group setup | group list | group <name> <info|start|stop|restart>";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            list(context);
            return;
        }

        String first = arguments.getFirst();

        if ("setup".equalsIgnoreCase(first) || "create".equalsIgnoreCase(first)) {
            setupWizard.run(context);
            return;
        }

        if ("list".equalsIgnoreCase(first)) {
            list(context);
            return;
        }

        groupAction(context, arguments);
    }

    private void groupAction(ConsoleContext context, List<String> arguments) {
        String groupName = arguments.getFirst();
        String action = arguments.size() >= 2 ? arguments.get(1) : "info";

        if ("info".equalsIgnoreCase(action)) {
            info(context, groupName);
            return;
        }

        if ("start".equalsIgnoreCase(action)) {
            int count = arguments.size() >= 3 ? positiveInt(arguments.get(2), "count") : missingMinimum(context, groupName);
            start(context, groupName, Math.max(1, count));
            return;
        }

        if ("stop".equalsIgnoreCase(action)) {
            stop(context, groupName, false);
            return;
        }

        if ("kill".equalsIgnoreCase(action)) {
            stop(context, groupName, true);
            return;
        }

        if ("restart".equalsIgnoreCase(action)) {
            stop(context, groupName, false);
            start(context, groupName, missingMinimum(context, groupName));
            return;
        }

        throw new IllegalArgumentException("Usage: group " + groupName + " <info|start|stop|restart>");
    }

    private void list(ConsoleContext context) {
        Collection<IGroup> groups = context.node().groupManager().groups();

        if (groups.isEmpty()) {
            context.warn("No groups configured. Create one with: " + context.code("group setup"));
            return;
        }

        context.header("Minecraft groups");

        for (IGroup group : groups) {
            int active = context.node().serviceRegistry().activeServiceCount(group.name());
            context.print(" " + ConsoleTheme.bullet() + " " + context.accent(group.name()) + context.muted("  •  ") + group.serviceType() + context.muted("  •  ") + group.software() + " " + group.softwareVersion() + context.muted("  •  ") + active + "/" + group.minCount() + " online" + context.muted("  •  ") + group.bindAddress());
        }
    }

    private void info(ConsoleContext context, String groupName) {
        IGroup group = group(context, groupName);
        Optional<GroupConfig> optionalConfig = context.node().groupManager().config(group.name());

        context.header("Group " + group.name());
        context.row("Type", group.serviceType().name());
        context.row("Template", group.templateName());
        context.row("Software", group.software() + " " + group.softwareVersion());
        context.row("Bind IP", group.bindAddress());
        context.row("Services", context.node().serviceRegistry().activeServiceCount(group.name()) + " active / " + group.minCount() + " minimum / " + group.maxCount() + " maximum");
        context.row("Memory", group.minMemory() + "-" + group.maxMemory() + "MB");
        context.row("Base port", String.valueOf(group.basePort()));
        context.row("Static", String.valueOf(group.staticServices()));
        context.row("Install on start", String.valueOf(group.installOnStart()));

        if (optionalConfig.isPresent()) {
            context.row("Config", optionalConfig.get().file().toString());
        }
    }

    private void start(ConsoleContext context, String groupName, int count) {
        animation.spin(context, "starting " + count + " service slot(s) for " + groupName, Duration.ofMillis(650));
        List<ServiceStartResult> results = context.node().serviceScheduler().startGroup(groupName, count);
        animation.success(context, "Start requested for " + results.size() + " service(s).");

        for (ServiceStartResult result : results) {
            context.print("  " + context.accent(result.serviceId()) + context.muted(" -> ") + result.wrapperId());
        }
    }

    private void stop(ConsoleContext context, String groupName, boolean force) {
        animation.spin(context, (force ? "killing " : "stopping ") + groupName, Duration.ofMillis(550));
        int sent = context.node().serviceScheduler().stopGroup(groupName, "Group " + groupName + " stop requested from console", force);
        animation.success(context, (force ? "Kill" : "Stop") + " requested for " + sent + " service(s).");
    }

    private IGroup group(ConsoleContext context, String groupName) {
        IGroup group = context.node().groupManager().groupByName(groupName);

        if (group == null) {
            throw new IllegalArgumentException("Unknown group: " + groupName);
        }

        return group;
    }

    private int missingMinimum(ConsoleContext context, String groupName) {
        IGroup group = group(context, groupName);
        int active = context.node().serviceRegistry().activeServiceCount(group.name());
        return Math.max(1, group.minCount() - active);
    }

    private int positiveInt(String input, String name) {
        try {
            int value = Integer.parseInt(input);

            if (value < 1) {
                throw new IllegalArgumentException(name + " must be greater than 0");
            }

            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a valid number", exception);
        }
    }
}
