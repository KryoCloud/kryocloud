package eu.kryocloud.node.console.command;

import eu.kryocloud.common.layout.KryoDirectoryLayout;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public final class CloudCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "cloud";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CORE;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("home", "kryo", "kryocloud");
    }

    @Override
    public String description() {
        return "Shows and manages KryoCloud runtime home";
    }

    @Override
    public String usage() {
        return "cloud info | cloud home | cloud home set <path> | cloud home reset";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            info(context);
            return;
        }

        String first = arguments.getFirst();

        if ("info".equalsIgnoreCase(first)) {
            info(context);
            return;
        }

        if ("home".equalsIgnoreCase(first)) {
            home(context, arguments);
            return;
        }

        if ("set".equalsIgnoreCase(first)) {
            setHome(context, arguments, 1);
            return;
        }

        if ("reset".equalsIgnoreCase(first)) {
            resetHome(context);
            return;
        }

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void home(ConsoleContext context, List<String> arguments) {
        if (arguments.size() == 1) {
            info(context);
            return;
        }

        String action = arguments.get(1);

        if ("set".equalsIgnoreCase(action)) {
            setHome(context, arguments, 2);
            return;
        }

        if ("reset".equalsIgnoreCase(action)) {
            resetHome(context);
            return;
        }

        throw new IllegalArgumentException("Usage: cloud home [set <path>|reset]");
    }

    private void info(ConsoleContext context) {
        context.header("KryoCloud home");
        context.row("Home", KryoDirectoryLayout.ROOT.toString());
        context.row("Source", KryoDirectoryLayout.homeSource());
        context.row("Pointer", KryoDirectoryLayout.homePointer().toString());
        context.row("Config", KryoDirectoryLayout.CONFIG.toString());
        context.row("Templates", KryoDirectoryLayout.TEMPLATES.toString());
        context.row("Storage", KryoDirectoryLayout.STORAGE.toString());
        context.row("Static", KryoDirectoryLayout.STATIC.toString());
        context.row("Temporary", KryoDirectoryLayout.TMP.toString());
        context.row("JDK", KryoDirectoryLayout.JDK.toString());
        context.print("");
        context.info("Use " + context.code("cloud home set <path>") + " to update the pointer for the next restart.");
    }

    private void setHome(ConsoleContext context, List<String> arguments, int pathIndex) {
        if (arguments.size() <= pathIndex) {
            throw new IllegalArgumentException("Usage: cloud home set <path>");
        }

        Path path = Path.of(arguments.get(pathIndex)).toAbsolutePath().normalize();

        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Home path points to an existing regular file: " + path);
        }

        KryoDirectoryLayout.persistHomePointer(path);
        context.success("KryoCloud home pointer updated to " + path);
        context.warn("Restart KryoCloud before creating configs, templates or services in the new home.");
    }

    private void resetHome(ConsoleContext context) {
        if (KryoDirectoryLayout.hasExternalHome()) {
            context.warn("External home is controlled by -Dkryocloud.home or KRYOCLOUD_HOME. Remove that first.");
            return;
        }

        KryoDirectoryLayout.resetHomePointer();
        context.success("KryoCloud home pointer reset.");
        context.warn("Restart KryoCloud to run storage setup again.");
    }
}
