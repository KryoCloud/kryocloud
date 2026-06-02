package eu.kryocloud.node.console.wizard;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.node.config.group.GroupConfig;
import eu.kryocloud.node.config.network.NetworkAddressConfig;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.version.VersionInstallResult;

import java.util.ArrayList;
import java.util.List;

public final class GroupSetupWizard {

    public void run(ConsoleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        context.header("Create Minecraft group");

        String groupName = askName(context);
        String templateName = context.ask("Template name:", groupName);
        String serviceType = askServiceType(context);
        boolean staticServices = context.confirm("Use persistent static service directory?", defaultStatic(serviceType));
        String bindAddress = askBindAddress(context, serviceType);
        printAvailableSoftware(context);
        String software = askSoftware(context, serviceType);
        printAvailableVersions(context, software);
        String softwareVersion = askSoftwareVersion(context, software);
        boolean installOnStart = context.confirm("Install and materialize software automatically before start?", true);
        int serviceCount = askPositiveInt(context, "Default service count:", 1);
        int minCount = askNonNegativeInt(context, "Minimum online services:", serviceCount);
        int maxCount = askMaxCount(context, "Maximum online services:", Math.max(serviceCount, minCount), minCount);
        int minMemory = askPositiveInt(context, "Minimum memory in MB:", defaultMinMemory(serviceType));
        int maxMemory = askAtLeast(context, "Maximum memory in MB:", defaultMaxMemory(serviceType), minMemory);
        int maxPlayers = askPositiveInt(context, "Maximum players:", 100);
        int startNewPercent = askPercent(context, "Start new service at percent:", 80);
        int basePort = askBasePort(context, serviceType);
        boolean installNow = context.confirm("Install and materialize software now?", true);

        GroupConfig config = context.node().groupManager().createConfig(groupName);
        config.setName(groupName);
        config.setTemplateName(templateName);
        config.setServiceType(serviceType);
        config.setStaticServices(staticServices);
        config.setBindAddress(bindAddress);
        config.setSoftware(software);
        config.setSoftwareVersion(softwareVersion);
        config.setInstallOnStart(installOnStart);
        config.setServiceCount(serviceCount);
        config.setMinCount(minCount);
        config.setMaxCount(maxCount);
        config.setMinMemory(minMemory);
        config.setMaxMemory(maxMemory);
        config.setMaxPlayers(maxPlayers);
        config.setStartNewPercent(startNewPercent);
        config.setBasePort(basePort);

        IGroup group = context.node().groupManager().createGroup(config);

        if (installNow) {
            installNow(context, config);
        }

        context.success("Created Minecraft group " + group.name());
        context.print("  Template: " + config.getTemplateName());
        context.print("  Software: " + config.getSoftware() + " " + config.getSoftwareVersion());
        context.print("  Bind: " + config.getBindAddress());
        context.print("  Static: " + config.isStaticServices());
        context.print("  Port strategy: " + portStrategy(group.basePort()));
        context.print("  Minimum services: " + group.minCount());

        if (group.minCount() > 0) {
            context.node().serviceScheduler().reconcileGroup(group.name());
            context.info("Auto-start requested for minimum services of " + group.name() + ".");
        }

        context.print("  Manage: " + context.code("group " + group.name() + " info"));
    }

    private void installNow(ConsoleContext context, GroupConfig config) {
        boolean updateLatest = false;

        if (context.node().versionStorage().latestVersion(config.getSoftware()).isPresent()) {
            updateLatest = context.confirm("Update " + config.getSoftware() + "-latest.jar?", false);
        }

        if (!context.node().versionStorage().installed(config.getSoftware(), config.getSoftwareVersion())) {
            VersionInstallResult result = context.node().versionStorage().installFromManifest(config.getSoftware(), config.getSoftwareVersion(), updateLatest);
            context.success("Installed " + result.software() + " " + result.version());
        }

        context.node().versionStorage().materializeTemplate(config.getSoftware(), config.getSoftwareVersion(), config.getTemplateName());
        context.success("Materialized template " + config.getTemplateName());
    }

    private void printAvailableSoftware(ConsoleContext context) {
        List<String> software = context.node().versionStorage().availableSoftware();

        if (software.isEmpty()) {
            return;
        }

        context.print("  Available software: " + String.join(", ", software));
    }

    private void printAvailableVersions(ConsoleContext context, String software) {
        try {
            List<String> versions = context.node().versionStorage().availableVersions(software);

            if (versions.isEmpty()) {
                return;
            }

            context.print("  Latest: " + versions.getFirst());
            context.print("  Versions: " + String.join(", ", versions.stream().limit(8).toList()));
        } catch (Exception exception) {
            context.warn("Could not fetch versions for " + software + ": " + exception.getMessage());
        }
    }

    private String askName(ConsoleContext context) {
        String groupName = context.ask("Group name:", "Lobby");

        if (context.node().groupManager().existsGroup(groupName)) {
            throw new IllegalStateException("Group already exists: " + groupName);
        }

        return groupName;
    }

    private String askServiceType(ConsoleContext context) {
        while (true) {
            String input = context.ask("Service type SERVER/PROXY/LOBBY:", "LOBBY", List.of("SERVER", "PROXY", "LOBBY")).toUpperCase();

            if ("SERVER".equals(input)) {
                return "SERVER";
            }

            if ("PROXY".equals(input)) {
                return "PROXY";
            }

            if ("LOBBY".equals(input)) {
                return "LOBBY";
            }

            context.warn("Service type must be SERVER, PROXY or LOBBY.");
        }
    }

    private String askBindAddress(ConsoleContext context, String serviceType) {
        NetworkAddressConfig networkConfig = context.node().networkAddressConfig();
        String fallback = networkConfig.defaultFor(serviceType);
        List<String> addresses = networkConfig.addressesFor(serviceType);

        if ("PROXY".equalsIgnoreCase(serviceType)) {
            context.info("Proxy groups bind to a public address because players connect to proxies directly.");
            String address = context.ask("Proxy public bind IP:", fallback, addresses);
            networkConfig.addProxyAddress(address);
            networkConfig.save();
            return address;
        }

        context.info("Backend server groups bind to local/private addresses. Proxy addons will register them later.");
        if (!addresses.isEmpty()) {
            context.print("  Available local addresses: " + String.join(", ", addresses));
        }

        if (!context.confirm("Use different local bind IP?", false)) {
            return fallback;
        }

        String address = context.ask("Backend local bind IP:", fallback, addresses);
        networkConfig.addServerAddress(address);
        networkConfig.save();
        return address;
    }

    private String askSoftware(ConsoleContext context, String serviceType) {
        List<String> software = context.node().versionStorage().availableSoftware();
        return context.ask("Minecraft software:", defaultSoftware(context, serviceType), software);
    }

    private String askSoftwareVersion(ConsoleContext context, String software) {
        List<String> candidates = versionCandidates(context, software);
        return context.ask("Minecraft version:", "latest", candidates);
    }

    private List<String> versionCandidates(ConsoleContext context, String software) {
        List<String> versions = new ArrayList<>();
        versions.add("latest");

        try {
            for (String version : context.node().versionStorage().availableVersions(software)) {
                if (versions.stream().anyMatch(entry -> entry.equalsIgnoreCase(version))) {
                    continue;
                }

                versions.add(version);
            }
        } catch (Exception exception) {
            return versions;
        }

        return List.copyOf(versions);
    }

    private boolean defaultStatic(String serviceType) {
        if ("PROXY".equals(serviceType)) {
            return true;
        }

        return false;
    }

    private String defaultSoftware(ConsoleContext context, String serviceType) {
        List<String> software = context.node().versionStorage().availableSoftware();

        if ("PROXY".equals(serviceType) && software.stream().anyMatch(entry -> entry.equalsIgnoreCase("velocity"))) {
            return "velocity";
        }

        if ("PROXY".equals(serviceType) && software.stream().anyMatch(entry -> entry.equalsIgnoreCase("flamecord"))) {
            return "flamecord";
        }

        if (software.stream().anyMatch(entry -> entry.equalsIgnoreCase("paper"))) {
            return "paper";
        }

        if (!software.isEmpty()) {
            return software.getFirst();
        }

        return "paper";
    }

    private int askBasePort(ConsoleContext context, String serviceType) {
        if ("PROXY".equals(serviceType)) {
            return askPort(context, "Proxy public port:", 25565);
        }

        context.info("Backend ports are assigned randomly at service start.");
        return 0;
    }

    private String portStrategy(int basePort) {
        if (basePort < 1) {
            return "random backend port";
        }

        return "fixed proxy port " + basePort;
    }

    private int defaultMinMemory(String serviceType) {
        if ("PROXY".equals(serviceType)) {
            return 256;
        }

        return 512;
    }

    private int defaultMaxMemory(String serviceType) {
        if ("PROXY".equals(serviceType)) {
            return 512;
        }

        return 1024;
    }

    private int askPositiveInt(ConsoleContext context, String question, int fallback) {
        while (true) {
            int value = parseInt(context.ask(question, String.valueOf(fallback)), fallback);

            if (value > 0) {
                return value;
            }

            context.warn("Value must be greater than 0.");
        }
    }

    private int askNonNegativeInt(ConsoleContext context, String question, int fallback) {
        while (true) {
            int value = parseInt(context.ask(question, String.valueOf(fallback)), fallback);

            if (value >= 0) {
                return value;
            }

            context.warn("Value must not be negative.");
        }
    }

    private int askMaxCount(ConsoleContext context, String question, int fallback, int minCount) {
        int safeFallback = Math.max(fallback, minCount);

        while (true) {
            int value = parseInt(context.ask(question, String.valueOf(safeFallback)), safeFallback);

            if (value >= minCount) {
                return value;
            }

            context.warn("maxCount must be greater than or equal to minCount (" + minCount + ").");
        }
    }

    private int askAtLeast(ConsoleContext context, String question, int fallback, int minimum) {
        int safeFallback = Math.max(fallback, minimum);

        while (true) {
            int value = parseInt(context.ask(question, String.valueOf(safeFallback)), safeFallback);

            if (value >= minimum) {
                return value;
            }

            context.warn("Value must be greater than or equal to " + minimum + ".");
        }
    }

    private int askPercent(ConsoleContext context, String question, int fallback) {
        while (true) {
            int value = parseInt(context.ask(question, String.valueOf(fallback)), fallback);

            if (value >= 0 && value <= 100) {
                return value;
            }

            context.warn("Percent must be between 0 and 100.");
        }
    }

    private int askPort(ConsoleContext context, String question, int fallback) {
        while (true) {
            int value = parseInt(context.ask(question, String.valueOf(fallback)), fallback);

            if (value >= 1 && value <= 65_535) {
                return value;
            }

            context.warn("Port must be between 1 and 65535.");
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
