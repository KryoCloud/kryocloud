package eu.kryocloud.node.console.wizard;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.node.config.group.GroupConfig;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.version.VersionInstallResult;

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
        String software = context.ask("Minecraft software:", "paper");
        String softwareVersion = context.ask("Minecraft version:", "latest");
        boolean installOnStart = context.confirm("Install and materialize software automatically before start?", true);
        int serviceCount = askPositiveInt(context, "Default service count:", 1);
        int minCount = askNonNegativeInt(context, "Minimum online services:", serviceCount);
        int maxCount = askMaxCount(context, "Maximum online services:", Math.max(serviceCount, minCount), minCount);
        int minMemory = askPositiveInt(context, "Minimum memory in MB:", 512);
        int maxMemory = askAtLeast(context, "Maximum memory in MB:", 1024, minMemory);
        int maxPlayers = askPositiveInt(context, "Maximum players:", 100);
        int startNewPercent = askPercent(context, "Start new service at percent:", 80);
        int basePort = askPort(context, "Base port:", defaultPort(serviceType));
        boolean installNow = context.confirm("Install and materialize software now?", true);

        GroupConfig config = context.node().groupManager().createConfig(groupName);
        config.setName(groupName);
        config.setTemplateName(templateName);
        config.setServiceType(serviceType);
        config.setStaticServices(staticServices);
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
        context.print("  Static: " + config.isStaticServices());
        context.print("  Services: " + config.getServiceCount());
        context.print("  Start: " + context.code("start " + group.name()));
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

    private String askName(ConsoleContext context) {
        String groupName = context.ask("Group name:", "Lobby");

        if (context.node().groupManager().existsGroup(groupName)) {
            throw new IllegalStateException("Group already exists: " + groupName);
        }

        return groupName;
    }

    private String askServiceType(ConsoleContext context) {
        while (true) {
            String input = context.ask("Service type SERVER/PROXY/LOBBY:", "LOBBY").toUpperCase();

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

    private boolean defaultStatic(String serviceType) {
        if ("PROXY".equals(serviceType)) {
            return true;
        }

        return false;
    }

    private int defaultPort(String serviceType) {
        if ("PROXY".equals(serviceType)) {
            return 25577;
        }

        return 25565;
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
