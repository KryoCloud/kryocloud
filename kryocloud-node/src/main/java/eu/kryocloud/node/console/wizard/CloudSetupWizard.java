package eu.kryocloud.node.console.wizard;

import eu.kryocloud.common.config.Config;
import eu.kryocloud.common.config.ConfigPathResolver;
import eu.kryocloud.common.config.type.ConfigType;
import eu.kryocloud.common.layout.KryoDirectoryLayout;
import eu.kryocloud.node.config.LaunchConfig;
import eu.kryocloud.node.config.NodeSecurityConfig;
import eu.kryocloud.node.config.network.NetworkAddressConfig;
import eu.kryocloud.node.config.setup.WrapperSetupConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class CloudSetupWizard {

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public void run(LaunchConfig launchConfig, NodeSecurityConfig securityConfig, WrapperSetupConfig wrapperConfig, NetworkAddressConfig networkConfig) {
        if (launchConfig == null) {
            throw new IllegalArgumentException("launchConfig must not be null");
        }

        if (securityConfig == null) {
            throw new IllegalArgumentException("securityConfig must not be null");
        }

        if (wrapperConfig == null) {
            throw new IllegalArgumentException("wrapperConfig must not be null");
        }

        if (networkConfig == null) {
            throw new IllegalArgumentException("networkConfig must not be null");
        }

        networkConfig.normalize();
        launchConfig.setDataDirectory(KryoDirectoryLayout.ROOT.toString());
        normalizeCloudName(launchConfig, wrapperConfig);

        if (launchConfig.isSetupComplete()) {
            normalizeJavaRuntimeDirectory(wrapperConfig);
            ensureConfigFormatConfigured(launchConfig, securityConfig, wrapperConfig, networkConfig);
            launchConfig.save();
            wrapperConfig.save();
            networkConfig.save();
            return;
        }

        printHeader();

        ConfigType configType = askConfigType("Config format", launchConfig.getFileExtension());
        String cloudName = askCloudName("Internal cloud name", launchConfig.getCloudName());
        String token = defaultToken(securityConfig.getToken());
        String nodeHost = ask("Node bind IP", launchConfig.getHost());
        int nodePort = askPort("Node protocol port", launchConfig.getPort());
        String wrapperId = ask("Default wrapper id", wrapperConfig.getWrapperId());
        String wrapperNodeHost = ask("Wrapper target node IP", suggestedWrapperHost(nodeHost));
        String wrapperAddress = ask("Wrapper advertised IP", wrapperConfig.getAdvertisedAddress());
        int wrapperMemory = askPositive("Wrapper max memory MB", wrapperConfig.getMaxMemoryMb());
        String serverAddress = ask("Default backend server bind IP", networkConfig.getDefaultServerAddress());
        String proxyAddress = ask("Default proxy public bind IP", networkConfig.getDefaultProxyAddress());
        launchConfig.setCloudName(cloudName);
        launchConfig.setDataDirectory(KryoDirectoryLayout.ROOT.toString());
        launchConfig.setHost(nodeHost);
        launchConfig.setPort(nodePort);
        launchConfig.setFileExtension(configType.getEnding());
        launchConfig.setConfigFormatConfigured(true);
        launchConfig.setConfigFormatSetupVersion(1);
        launchConfig.setSetupComplete(true);

        securityConfig.setToken(token);

        wrapperConfig.setCloudName(cloudName);
        wrapperConfig.setWrapperId(wrapperId);
        wrapperConfig.setNodeHost(wrapperNodeHost);
        wrapperConfig.setNodePort(nodePort);
        wrapperConfig.setToken(token);
        wrapperConfig.setAdvertisedAddress(wrapperAddress);
        wrapperConfig.setMaxMemoryMb(wrapperMemory);
        wrapperConfig.setJavaRuntimesDirectory(".jdk");

        networkConfig.setDefaultServerAddress(serverAddress);
        networkConfig.setDefaultProxyAddress(proxyAddress);

        removeLegacyWebConfigKeys(launchConfig);
        migrateConfigFiles(configType, launchConfig, securityConfig, wrapperConfig, networkConfig);

        printDone(cloudName, nodeHost, nodePort, wrapperNodeHost, serverAddress, proxyAddress, configType);
    }

    private void migrateConfigFiles(ConfigType configType, Config... configs) {
        for (Config config : configs) {
            String fileName = config.file().getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            String name = dot == -1 ? fileName : fileName.substring(0, dot);
            config.moveTo(ConfigPathResolver.target(KryoDirectoryLayout.CONFIG, name, configType), true);
        }
    }

    private void ensureConfigFormatConfigured(LaunchConfig launchConfig, NodeSecurityConfig securityConfig, WrapperSetupConfig wrapperConfig, NetworkAddressConfig networkConfig) {
        if (launchConfig.isConfigFormatConfigured() && launchConfig.getConfigFormatSetupVersion() >= 1) {
            removeLegacyWebConfigKeys(launchConfig);
            return;
        }

        System.out.println();
        System.out.println("❄ KryoCloud config format setup");
        System.out.println("Choose the format KryoCloud should use for newly written configs.");
        System.out.println("Existing launch/security/wrapper/network configs will be migrated to this format.");
        System.out.println();

        ConfigType configType = askConfigType("Config format", launchConfig.getFileExtension());

        launchConfig.setFileExtension(configType.getEnding());
        launchConfig.setConfigFormatConfigured(true);
        launchConfig.setConfigFormatSetupVersion(1);
        removeLegacyWebConfigKeys(launchConfig);

        migrateConfigFiles(configType, launchConfig, securityConfig, wrapperConfig, networkConfig);

        System.out.println();
        System.out.println("✓ Config format saved: " + configType.getEnding());
        System.out.println();
    }


    private void removeLegacyWebConfigKeys(LaunchConfig launchConfig) {
        launchConfig.getProvider().remove("webHost");
        launchConfig.getProvider().remove("webPort");
    }

    private void printHeader() {
        System.out.println();
        System.out.println("❄ KryoCloud first setup");
        System.out.println("This configures the config format, cloud identity, node protocol, wrapper connection and Minecraft bind addresses.");
        System.out.println("Groups can be created afterwards with: group setup");
        System.out.println();
    }

    private void printDone(String cloudName, String nodeHost, int nodePort, String wrapperNodeHost, String serverAddress, String proxyAddress, ConfigType configType) {
        System.out.println();
        System.out.println("✓ Setup saved.");
        System.out.println("  Cloud:       " + cloudName);
        System.out.println("  Home:        " + KryoDirectoryLayout.ROOT.toAbsolutePath().normalize());
        System.out.println("  Node:        " + nodeHost + ":" + nodePort);
        System.out.println("  Wrapper:     connects to " + wrapperNodeHost + ":" + nodePort);
        System.out.println("  Servers:     bind to " + serverAddress);
        System.out.println("  Proxies:     bind to " + proxyAddress);
        System.out.println("  Config:      " + KryoDirectoryLayout.CONFIG.toAbsolutePath().normalize());
        System.out.println("  Groups:      " + KryoDirectoryLayout.GROUPS.toAbsolutePath().normalize());
        System.out.println("  Format:      " + configType.getEnding());
        System.out.println();
    }

    private void normalizeCloudName(LaunchConfig launchConfig, WrapperSetupConfig wrapperConfig) {
        String cloudName = sanitizeCloudName(launchConfig.getCloudName());

        if (cloudName.isBlank()) {
            cloudName = "kryocloud";
        }

        launchConfig.setCloudName(cloudName);
        wrapperConfig.setCloudName(cloudName);
    }

    private ConfigType askConfigType(String question, String fallback) {
        ConfigType fallbackType = fallbackConfigType(fallback);

        while (true) {
            String value = ask(question + " (" + ConfigType.supportedNames() + ")", fallbackType.getEnding().replace(".", ""));

            try {
                return ConfigType.fromName(value);
            } catch (IllegalArgumentException exception) {
                System.out.println("Supported config formats: " + ConfigType.supportedNames());
            }
        }
    }

    private ConfigType fallbackConfigType(String value) {
        try {
            return ConfigType.fromName(value);
        } catch (Exception exception) {
            return ConfigType.YAML;
        }
    }

    private String ask(String question, String fallback) {
        String safeFallback = fallback == null || fallback.isBlank() ? "" : fallback;

        System.out.print(question + " [" + safeFallback + "]: ");
        String input = readLine();

        if (input == null || input.isBlank()) {
            return safeFallback;
        }

        return input.trim();
    }

    private String readLine() {
        try {
            java.io.Console console = System.console();

            if (console != null) {
                return console.readLine();
            }

            return reader.readLine();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read setup input", exception);
        }
    }

    private String askCloudName(String question, String fallback) {
        while (true) {
            String value = sanitizeCloudName(ask(question, sanitizeCloudName(fallback)));

            if (!value.isBlank()) {
                return value;
            }

            System.out.println("Cloud name may only contain letters, numbers, dots, underscores and hyphens.");
        }
    }

    private int askPort(String question, int fallback) {
        while (true) {
            int value = parseInt(ask(question, String.valueOf(fallback)), fallback);

            if (value >= 1 && value <= 65_535) {
                return value;
            }

            System.out.println("Port must be between 1 and 65535.");
        }
    }

    private int askPositive(String question, int fallback) {
        while (true) {
            int value = parseInt(ask(question, String.valueOf(fallback)), fallback);

            if (value > 0) {
                return value;
            }

            System.out.println("Value must be greater than 0.");
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String suggestedWrapperHost(String nodeHost) {
        if ("0.0.0.0".equals(nodeHost)) {
            return "127.0.0.1";
        }

        return nodeHost;
    }

    private String sanitizeCloudName(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase().replaceAll("[^a-z0-9_.-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
    }

    private void normalizeJavaRuntimeDirectory(WrapperSetupConfig wrapperConfig) {
        String directory = wrapperConfig.getJavaRuntimesDirectory();

        if (directory == null || directory.isBlank() || "runtimes".equalsIgnoreCase(directory.trim())) {
            wrapperConfig.setJavaRuntimesDirectory(".jdk");
        }
    }

    private String defaultToken(String configuredToken) {
        if (configuredToken != null && !configuredToken.isBlank() && !configuredToken.startsWith("change-this")) {
            return configuredToken;
        }

        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
