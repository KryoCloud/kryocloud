package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.stats.ClusterStatsCollector;
import eu.kryocloud.node.console.stats.ClusterStatsSnapshot;
import eu.kryocloud.node.console.stats.GroupStatsSnapshot;
import eu.kryocloud.node.console.tui.ConsoleAnimation;
import eu.kryocloud.node.console.tui.ConsoleTheme;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

public final class StatsCommand implements ConsoleCommand {

    private final ConsoleAnimation animation = new ConsoleAnimation();

    @Override
    public String name() {
        return "stats";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CLUSTER;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("usage", "metrics");
    }

    @Override
    public String description() {
        return "Shows animated Minecraft cloud usage stats";
    }

    @Override
    public String usage() {
        return "stats [groups|group <name>|live [seconds] [intervalMs]]";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            snapshot(context);
            return;
        }

        String action = arguments.getFirst();

        if ("groups".equalsIgnoreCase(action)) {
            groups(context);
            return;
        }

        if ("group".equalsIgnoreCase(action)) {
            group(context, arguments);
            return;
        }

        if ("live".equalsIgnoreCase(action)) {
            live(context, arguments);
            return;
        }

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void snapshot(ConsoleContext context) {
        ClusterStatsSnapshot snapshot = collector(context).snapshot();

        context.header("Minecraft cloud stats");
        context.row("Wrappers", snapshot.wrappersOnline() + " online, " + snapshot.wrappersTimedOut() + " timed out");
        context.row("Services", snapshot.runningServices() + " running, " + snapshot.startingServices() + " starting, " + snapshot.failedServices() + " failed, " + snapshot.knownServices() + " known");
        context.row("Memory", snapshot.wrapperMemoryUsedMb() + "MB / " + snapshot.wrapperMemoryMaxMb() + "MB " + ConsoleTheme.progressBar(snapshot.wrapperMemoryRatio(), 18) + " " + ConsoleTheme.percent(snapshot.wrapperMemoryRatio()));
        context.row("Groups", String.valueOf(snapshot.groupCount()));
    }

    private void groups(ConsoleContext context) {
        ClusterStatsSnapshot snapshot = collector(context).snapshot();

        if (snapshot.groups().isEmpty()) {
            context.warn("No Minecraft groups configured.");
            return;
        }

        context.header("Minecraft group usage");

        for (GroupStatsSnapshot group : snapshot.groups()) {
            printGroupLine(context, group);
        }
    }

    private void group(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: stats group <name>");
        }

        GroupStatsSnapshot group = collector(context).groupSnapshot(arguments.get(1));

        context.header("Group " + group.groupName());
        context.row("Type", group.serviceType().name());
        context.row("Software", group.software() + " " + group.softwareVersion());
        context.row("Services", group.runningServices() + "/" + group.maxCount() + " running " + ConsoleTheme.progressBar(group.serviceUsageRatio(), 18) + " " + ConsoleTheme.percent(group.serviceUsageRatio()));
        context.row("Configured memory", group.configuredMemoryMb() + "MB / " + group.maxMemoryCapacityMb() + "MB " + ConsoleTheme.progressBar(group.memoryUsageRatio(), 18) + " " + ConsoleTheme.percent(group.memoryUsageRatio()));
        context.row("Static", String.valueOf(group.staticServices()));
        context.row("Known", group.knownServices() + " known, " + group.failedServices() + " failed");
    }

    private void live(ConsoleContext context, List<String> arguments) {
        int seconds = arguments.size() >= 2 ? positiveInt(arguments.get(1), "seconds") : 12;
        int intervalMillis = arguments.size() >= 3 ? positiveInt(arguments.get(2), "intervalMs") : 500;
        ClusterStatsCollector collector = collector(context);

        animation.live(context, () -> liveLine(collector.snapshot()), Duration.ofSeconds(seconds), Duration.ofMillis(intervalMillis));
        animation.success(context, "Stats capture finished.");
        snapshot(context);
    }

    private String liveLine(ClusterStatsSnapshot snapshot) {
        return ConsoleTheme.value("wrappers ") + ConsoleTheme.info(String.valueOf(snapshot.wrappersOnline())) + ConsoleTheme.muted("  •  ") + ConsoleTheme.value("services ") + ConsoleTheme.success(String.valueOf(snapshot.runningServices())) + ConsoleTheme.muted("/") + ConsoleTheme.value(String.valueOf(snapshot.knownServices())) + ConsoleTheme.muted("  •  ") + ConsoleTheme.value("memory ") + ConsoleTheme.progressBar(snapshot.wrapperMemoryRatio(), 14) + " " + ConsoleTheme.percent(snapshot.wrapperMemoryRatio());
    }

    private void printGroupLine(ConsoleContext context, GroupStatsSnapshot group) {
        context.print(" " + ConsoleTheme.bullet() + " " + context.accent(group.groupName()) + context.muted("  •  ") + ConsoleTheme.value(group.serviceType().name()) + context.muted("  •  ") + ConsoleTheme.value(group.software() + " " + group.softwareVersion()) + context.muted("  •  ") + group.runningServices() + "/" + group.maxCount() + " " + ConsoleTheme.progressBar(group.serviceUsageRatio(), 12) + " " + ConsoleTheme.percent(group.serviceUsageRatio()));
    }

    private ClusterStatsCollector collector(ConsoleContext context) {
        return new ClusterStatsCollector(context.node());
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