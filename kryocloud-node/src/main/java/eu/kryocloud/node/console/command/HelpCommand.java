package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.CommandRegistry;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.tui.ConsoleTheme;

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
        return "Shows KryoCloud command help";
    }

    @Override
    public String usage() {
        return "help";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        context.header("KryoCloud command tree");
        context.print(context.muted("Commands are object-first: ") + context.code("group Lobby start") + context.muted(" or ") + context.code("service Lobby-1 stop"));
        context.print(context.muted("Use TAB for completion, arrow-up for history and CTRL+C for ") + context.code("shutdown") + context.muted("."));
        context.print("");

        printSection(context, "Cloud", List.of(
                line("cloud info", "show cloud home, config, templates, storage and JDK paths"),
                line("cloud home", "show current runtime directory and source"),
                line("cloud home set <path>", "change the .kryocloud-home pointer for next restart"),
                line("cloud home reset", "delete the pointer and run storage setup on next restart"),
                line("shutdown", "gracefully shut down KryoCloud; CTRL+C runs this too")
        ));

        printSection(context, "Groups", List.of(
                line("group setup", "create a group with the setup wizard"),
                line("group list", "show all groups"),
                line("group <name> info", "show group details"),
                line("group <name> start [count]", "start missing or explicit service slots"),
                line("group <name> stop", "stop all services of a group"),
                line("group <name> restart", "stop and start minimum services"),
                line("ip list", "show server/proxy bind addresses"),
                line("ip add <server|proxy> <address>", "register a new bind address")
        ));

        printSection(context, "Services", List.of(
                line("service list", "show known services"),
                line("service <name> info", "show process metrics and state"),
                line("service <name> stop", "gracefully stop and delete temp workspace"),
                line("service <name> kill", "force stop a service"),
                line("service <name> logs [lines]", "request recent service logs"),
                line("service <name> cmd <command>", "write a Minecraft command")
        ));

        printSection(context, "Cluster", List.of(
                line("wrapper list", "show wrappers"),
                line("stats", "show current usage"),
                line("stats live [seconds]", "animated usage sampling"),
                line("version install <software> [version]", "install a Minecraft server version"),
                line("service cleanup [wrapper] [--dry-run]", "remove orphaned tmp workspaces")
        ));

        Map<ConsoleCategory, List<ConsoleCommand>> commandsByCategory = commandRegistry.commands().stream().collect(Collectors.groupingBy(ConsoleCommand::category));
        context.print("");
        context.print(ConsoleTheme.muted("Registered roots: ") + commandsByCategory.values().stream().flatMap(List::stream).map(ConsoleCommand::name).distinct().sorted().map(context::code).collect(Collectors.joining(ConsoleTheme.muted(", "))));
    }

    private void printSection(ConsoleContext context, String title, List<String> lines) {
        context.print("  " + ConsoleTheme.accent(title));

        for (String line : lines) {
            context.print(line);
        }

        context.print("");
    }

    private String line(String command, String description) {
        return "   " + ConsoleTheme.bullet() + " " + ConsoleTheme.code(command) + ConsoleTheme.muted("  —  ") + ConsoleTheme.frost(description);
    }
}
