package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.version.VersionImportCandidate;
import eu.kryocloud.node.version.VersionInstallResult;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public final class VersionCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "version";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.SERVICE;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("versions");
    }

    @Override
    public String description() {
        return "Manages installed server/proxy software versions";
    }

    @Override
    public String usage() {
        return "version install <software> [version|latest] | version create <software> <name> <jar> | version scan";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Usage: " + usage());
        }

        String action = arguments.getFirst();

        if ("install".equalsIgnoreCase(action)) {
            install(context, arguments);
            return;
        }

        if ("create".equalsIgnoreCase(action)) {
            create(context, arguments);
            return;
        }

        if ("scan".equalsIgnoreCase(action)) {
            scan(context);
            return;
        }

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void install(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: version install <software> [version|latest]");
        }

        String software = arguments.get(1);
        String version = "latest";

        if (arguments.size() >= 3) {
            version = arguments.get(2);
        }

        boolean updateLatest = false;

        if (context.node().versionStorage().latestVersion(software).isPresent()) {
            updateLatest = context.confirm("Update " + software + "-latest.jar after install?", false);
        }

        VersionInstallResult result = context.node().versionStorage().installFromManifest(software, version, updateLatest);
        context.success("Installed " + result.software() + " " + result.version());
        context.print("  Canonical: " + result.serverJar());
        context.print("  Alias: " + result.flatJar());
        context.print("  Latest updated: " + result.latestUpdated());
    }

    private void create(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 4) {
            throw new IllegalArgumentException("Usage: version create <software> <name> <jar>");
        }

        String software = arguments.get(1);
        String version = arguments.get(2);
        Path jar = Path.of(arguments.get(3));
        boolean updateLatest = false;

        if (context.node().versionStorage().latestVersion(software).isPresent()) {
            updateLatest = context.confirm("Update " + software + "-latest.jar to " + version + "?", false);
        }

        VersionInstallResult result = context.node().versionStorage().installLocalJar(software, version, jar, updateLatest);
        context.success("Created version " + result.software() + " " + result.version());
        context.print("  Canonical: " + result.serverJar());
        context.print("  Alias: " + result.flatJar());
        context.print("  Latest updated: " + result.latestUpdated());
    }

    private void scan(ConsoleContext context) {
        List<VersionImportCandidate> candidates = context.node().versionStorage().pendingImports();

        if (candidates.isEmpty()) {
            context.warn("No pending jar imports found in storage/versions.");
            return;
        }

        context.header("Pending version imports");

        for (VersionImportCandidate candidate : candidates) {
            context.print(" • " + candidate.source() + " -> " + candidate.suggestedSoftware() + " " + candidate.suggestedVersion());

            if (!context.confirm("Install this version?", true)) {
                continue;
            }

            boolean updateLatest = context.confirm("Update " + candidate.suggestedSoftware() + "-latest.jar?", false);
            VersionInstallResult result = context.node().versionStorage().installLocalJar(candidate.suggestedSoftware(), candidate.suggestedVersion(), candidate.source(), updateLatest);
            context.success("Imported " + result.software() + " " + result.version());
        }
    }
}