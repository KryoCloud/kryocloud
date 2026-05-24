package eu.kryocloud.node.console.wizard;

import eu.kryocloud.common.layout.KryoDirectoryLayout;
import eu.kryocloud.node.config.LaunchConfig;
import eu.kryocloud.node.config.NodeSecurityConfig;
import eu.kryocloud.node.config.network.NetworkAddressConfig;
import eu.kryocloud.node.config.setup.WrapperSetupConfig;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Scanner;

public final class CloudSetupWizard {

    private final Scanner scanner = new Scanner(System.in);

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
            launchConfig.save();
            normalizeJavaRuntimeDirectory(wrapperConfig);
            wrapperConfig.save();
            networkConfig.save();
            return;
        }

        printHeader();

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
        String webHost = ask("Web service bind IP", launchConfig.getWebHost());
        int webPort = askPort("Web service port", launchConfig.getWebPort());

        launchConfig.setCloudName(cloudName);
        launchConfig.setDataDirectory(KryoDirectoryLayout.ROOT.toString());
        launchConfig.setHost(nodeHost);
        launchConfig.setPort(nodePort);
        launchConfig.setWebHost(webHost);
        launchConfig.setWebPort(webPort);
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

        launchConfig.save();
        securityConfig.save();
        wrapperConfig.save();
        networkConfig.save();

        printDone(cloudName, nodeHost, nodePort, wrapperNodeHost, webHost, webPort, serverAddress, proxyAddress);
    }

    private void printHeader() {
        System.out.println();
        System.out.println("❄ KryoCloud first setup");
        System.out.println("This configures the cloud identity, node protocol, wrapper connection, Minecraft bind addresses and future web service.");
        System.out.println("Groups can be created afterwards with: group setup");
        System.out.println();
    }

    private void printDone(String cloudName, String nodeHost, int nodePort, String wrapperNodeHost, String webHost, int webPort, String serverAddress, String proxyAddress) {
        System.out.println();
        System.out.println("✓ Setup saved.");
        System.out.println("  Cloud:       " + cloudName);
        System.out.println("  Home:        " + KryoDirectoryLayout.ROOT.toAbsolutePath().normalize());
        System.out.println("  Node:        " + nodeHost + ":" + nodePort);
        System.out.println("  Wrapper:     connects to " + wrapperNodeHost + ":" + nodePort);
        System.out.println("  Servers:     bind to " + serverAddress);
        System.out.println("  Proxies:     bind to " + proxyAddress);
        System.out.println("  Web:         " + webHost + ":" + webPort + " (reserved for dashboard)");
        System.out.println("  Config:      " + KryoDirectoryLayout.CONFIG.toAbsolutePath().normalize());
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

    private String ask(String question, String fallback) {
        String safeFallback = fallback == null || fallback.isBlank() ? "" : fallback;

        System.out.print(question + " [" + safeFallback + "]: ");
        String input = scanner.nextLine();

        if (input == null || input.isBlank()) {
            return safeFallback;
        }

        return input.trim();
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
