package eu.kryocloud.node.console.command;

import eu.kryocloud.common.manifest.ManifestCodename;
import eu.kryocloud.common.manifest.SoftwareManifest;
import eu.kryocloud.common.manifest.ManifestVersionComparator;
import eu.kryocloud.common.manifest.SoftwareVersion;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.tui.ConsoleTheme;
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
        return List.of("versions", "software");
    }

    @Override
    public String description() {
        return "Manages Minecraft software manifests and installed versions";
    }

    @Override
    public String usage() {
        return "version list | version refresh | version <software> list | version <software> info [version] | version <software> install [version|latest] | version create <software> <name> <jar> | version scan";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            listSoftware(context);
            return;
        }

        String first = arguments.getFirst();

        if ("list".equalsIgnoreCase(first) || "available".equalsIgnoreCase(first)) {
            listSoftware(context);
            return;
        }

        if ("refresh".equalsIgnoreCase(first) || "reload".equalsIgnoreCase(first)) {
            refresh(context);
            return;
        }

        if ("install".equalsIgnoreCase(first)) {
            legacyInstall(context, arguments);
            return;
        }

        if ("create".equalsIgnoreCase(first)) {
            create(context, arguments);
            return;
        }

        if ("scan".equalsIgnoreCase(first)) {
            scan(context);
            return;
        }

        softwareAction(context, arguments);
    }

    private void softwareAction(ConsoleContext context, List<String> arguments) {
        String software = arguments.getFirst();
        String action = arguments.size() >= 2 ? arguments.get(1) : "info";

        if ("list".equalsIgnoreCase(action) || "versions".equalsIgnoreCase(action)) {
            listVersions(context, software);
            return;
        }

        if ("info".equalsIgnoreCase(action)) {
            String version = arguments.size() >= 3 ? arguments.get(2) : "latest";
            info(context, software, version);
            return;
        }

        if ("install".equalsIgnoreCase(action)) {
            String version = arguments.size() >= 3 ? arguments.get(2) : "latest";
            install(context, software, version);
            return;
        }

        throw new IllegalArgumentException("Usage: version " + software + " <list|info [version]|install [version]>");
    }

    private void listSoftware(ConsoleContext context) {
        List<String> software = context.node().versionStorage().availableSoftware();

        if (software.isEmpty()) {
            context.warn("No Minecraft software manifests are registered. Try " + context.code("version refresh") + ".");
            return;
        }

        context.header("Minecraft software manifests");
        context.row("Index", context.node().versionStorage().manifestIndexSource().toString());
        context.row("Cloud codename", context.node().versionStorage().latestCodename());

        if (!context.node().versionStorage().channels().isEmpty()) {
            context.row("Channels", String.join(", ", context.node().versionStorage().channels()));
        }

        if (!context.node().versionStorage().codenames().isEmpty()) {
            context.print("");
            context.print("  " + ConsoleTheme.accent("Cloud codenames"));

            for (ManifestCodename codename : context.node().versionStorage().codenames()) {
                context.print("   " + ConsoleTheme.bullet() + " " + context.accent(codename.name()) + context.muted("  •  ") + String.join(", ", codename.versions()));
            }
        }

        context.print("");
        context.print("  " + ConsoleTheme.accent("Minecraft software"));

        for (String entry : software) {
            context.print("   " + ConsoleTheme.bullet() + " " + context.accent(entry) + context.muted("  •  ") + context.node().versionStorage().manifestSource(entry));
        }
    }

    private void refresh(ConsoleContext context) {
        int loaded = context.node().versionStorage().refreshManifests();

        if (loaded < 1) {
            context.warn("Dynamic manifest refresh failed or returned no new software. Fallback manifests are still available.");
            return;
        }

        context.success("Loaded " + loaded + " Minecraft software manifest(s).");
    }

    private void listVersions(ConsoleContext context, String software) {
        SoftwareManifest manifest = context.node().versionStorage().manifest(software);

        context.header("Versions for " + software);
        context.row("Type", manifest.type().name());
        context.row("Latest", manifest.latestVersion());
        context.row("Manifest", context.node().versionStorage().manifestSource(software).toString());
        context.print("");

        for (String version : ManifestVersionComparator.sortNewestFirst(manifest.versions().keySet())) {
            SoftwareVersion softwareVersion = manifest.versions().get(version);
            String marker = version.equalsIgnoreCase(manifest.latestVersion()) ? context.good(" latest") : "";
            context.print(" " + ConsoleTheme.bullet() + " " + context.accent(version) + marker + context.muted("  •  Java ") + softwareVersion.javaVersion() + context.muted("  •  ") + softwareVersion.javaFlags().size() + " JVM flag(s)");
        }
    }

    private void info(ConsoleContext context, String software, String requestedVersion) {
        SoftwareManifest manifest = context.node().versionStorage().manifest(software);
        SoftwareVersion version = manifest.resolve(requestedVersion);

        context.header(software + " " + version.version());
        context.row("Type", manifest.type().name());
        context.row("Latest", manifest.latestVersion());
        context.row("Java", String.valueOf(version.javaVersion()));
        context.row("Manifest", context.node().versionStorage().manifestSource(software).toString());
        context.row("Download", version.link().toString());

        if (version.javaFlags().isEmpty()) {
            context.row("Flags", "none");
            return;
        }

        context.print("");
        context.print("  " + ConsoleTheme.accent("JVM flags"));

        for (String flag : version.javaFlags()) {
            context.print("   " + ConsoleTheme.bullet() + " " + context.value(flag));
        }
    }

    private void legacyInstall(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: version install <software> [version|latest]");
        }

        String software = arguments.get(1);
        String version = arguments.size() >= 3 ? arguments.get(2) : "latest";
        install(context, software, version);
    }

    private void install(ConsoleContext context, String software, String version) {
        boolean updateLatest = false;

        if (context.node().versionStorage().latestVersion(software).isPresent()) {
            updateLatest = context.confirm("Update " + software + "-latest.jar after install?", false);
        }

        VersionInstallResult result = context.node().versionStorage().installFromManifest(software, version, updateLatest);
        context.success("Installed " + result.software() + " " + result.version());
        context.print("  Java: " + result.javaVersion());
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
            context.print(" " + ConsoleTheme.bullet() + " " + candidate.source() + " -> " + candidate.suggestedSoftware() + " " + candidate.suggestedVersion());

            if (!context.confirm("Install this version?", true)) {
                continue;
            }

            boolean updateLatest = context.confirm("Update " + candidate.suggestedSoftware() + "-latest.jar?", false);
            VersionInstallResult result = context.node().versionStorage().installLocalJar(candidate.suggestedSoftware(), candidate.suggestedVersion(), candidate.source(), updateLatest);
            context.success("Imported " + result.software() + " " + result.version());
        }
    }
}
