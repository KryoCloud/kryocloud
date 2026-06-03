package eu.kryocloud.wrapper.instance.workspace;

import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.packet.type.service.ServiceStartRequestPacket;
import eu.kryocloud.network.protocol.CloudServiceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

public final class InstanceWorkspace {

    private static final KryoLogger LOGGER = KryoLogger.logger("InstanceWorkspace");

    private final Path templatesDirectory;
    private final Path temporaryDirectory;
    private final Path staticDirectory;
    private final Path failedDirectory;
    private final Path addonsDirectory;

    public InstanceWorkspace(Path templatesDirectory, Path temporaryDirectory, Path staticDirectory, Path addonsDirectory) {
        if (templatesDirectory == null) {
            throw new IllegalArgumentException("templatesDirectory must not be null");
        }

        if (temporaryDirectory == null) {
            throw new IllegalArgumentException("temporaryDirectory must not be null");
        }

        if (staticDirectory == null) {
            throw new IllegalArgumentException("staticDirectory must not be null");
        }

        if (addonsDirectory == null) {
            throw new IllegalArgumentException("addonsDirectory must not be null");
        }

        this.templatesDirectory = templatesDirectory;
        this.temporaryDirectory = temporaryDirectory;
        this.staticDirectory = staticDirectory;
        this.failedDirectory = temporaryDirectory.resolve("failed");
        this.addonsDirectory = addonsDirectory;
    }

    public Path prepare(ServiceStartRequestPacket packet) throws IOException {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        Path templateDirectory = templatesDirectory.resolve(packet.templateName());
        Path workingDirectory = workingDirectory(packet);

        if (!Files.exists(templateDirectory)) {
            throw new IllegalStateException("Template does not exist: " + templateDirectory);
        }

        if (packet.staticService()) {
            prepareStaticWorkspace(templateDirectory, workingDirectory);
            writeMinecraftFiles(packet, workingDirectory);
            installDefaultProxyBridge(packet, workingDirectory);
            return workingDirectory;
        }

        prepareTemporaryWorkspace(templateDirectory, workingDirectory);
        writeMinecraftFiles(packet, workingDirectory);
        installDefaultProxyBridge(packet, workingDirectory);

        return workingDirectory;
    }

    private void installDefaultProxyBridge(ServiceStartRequestPacket packet, Path workingDirectory) throws IOException {
        if (packet.serviceType() != CloudServiceType.PROXY) {
            return;
        }

        Path source = defaultProxyBridgeJar();

        if (source == null) {
            LOGGER.warn("Proxy bridge plugin is missing. Expected addons/proxy/kryocloud-proxy-bridge.jar or addons/kryocloud-proxy-bridge.jar");
            return;
        }

        Path pluginsDirectory = workingDirectory.resolve("plugins");
        Files.createDirectories(pluginsDirectory);
        Files.copy(source, pluginsDirectory.resolve("kryocloud-proxy-bridge.jar"), StandardCopyOption.REPLACE_EXISTING);
    }

    private Path defaultProxyBridgeJar() {
        Path proxyScoped = addonsDirectory.resolve("proxy").resolve("kryocloud-proxy-bridge.jar");

        if (Files.exists(proxyScoped)) {
            return proxyScoped;
        }

        Path flat = addonsDirectory.resolve("kryocloud-proxy-bridge.jar");

        if (Files.exists(flat)) {
            return flat;
        }

        return null;
    }

    public boolean cleanupTemporary(ServiceStartRequestPacket packet, Path workingDirectory) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        if (packet.staticService()) {
            return true;
        }

        return cleanup(workingDirectory);
    }

    public boolean cleanupTemporary(boolean staticService, Path workingDirectory) {
        if (staticService) {
            return true;
        }

        return cleanup(workingDirectory);
    }

    public void archive(ServiceStartRequestPacket packet, Path workingDirectory, String logs) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        archive(packet.serviceId(), workingDirectory, logs);
    }

    public void archive(String serviceId, Path workingDirectory, String logs) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        try {
            Path archiveDirectory = failedDirectory.resolve(serviceId);
            Files.createDirectories(archiveDirectory);
            Files.writeString(archiveDirectory.resolve("kryocloud-instance.log"), logs == null ? "" : logs);

            if (workingDirectory != null && Files.exists(workingDirectory.resolve("server.properties"))) {
                Files.copy(workingDirectory.resolve("server.properties"), archiveDirectory.resolve("server.properties"), StandardCopyOption.REPLACE_EXISTING);
            }

            if (workingDirectory != null && Files.exists(workingDirectory.resolve("eula.txt"))) {
                Files.copy(workingDirectory.resolve("eula.txt"), archiveDirectory.resolve("eula.txt"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to archive Minecraft instance " + serviceId + ": " + exception.getMessage());
        }
    }

    public String archivedLogs(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        Path archiveLog = failedDirectory.resolve(serviceId).resolve("kryocloud-instance.log");

        if (!Files.exists(archiveLog)) {
            return "";
        }

        try {
            return Files.readString(archiveLog);
        } catch (Exception exception) {
            LOGGER.warn("Failed to read archived Minecraft logs for " + serviceId + ": " + exception.getMessage());
            return "";
        }
    }

    public InstanceCleanupResult cleanupOrphans(Set<String> activeServiceIds, boolean dryRun) {
        if (activeServiceIds == null) {
            throw new IllegalArgumentException("activeServiceIds must not be null");
        }

        try {
            Files.createDirectories(temporaryDirectory);
        } catch (Exception exception) {
            LOGGER.warn("Failed to create temporary directory before cleanup: " + exception.getMessage());
            return new InstanceCleanupResult(0, 0, 0, 1, exception.getMessage());
        }

        int scanned = 0;
        int deleted = 0;
        int skipped = 0;
        int failed = 0;
        StringJoiner details = new StringJoiner(System.lineSeparator());

        try (var paths = Files.list(temporaryDirectory)) {
            for (Path path : paths.filter(Files::isDirectory).sorted(Comparator.comparing(Path::getFileName)).toList()) {
                String directoryName = path.getFileName().toString();

                if ("failed".equalsIgnoreCase(directoryName)) {
                    skipped++;
                    details.add("skipped archive directory " + directoryName);
                    continue;
                }

                scanned++;

                if (activeServiceIds.contains(directoryName)) {
                    skipped++;
                    details.add("skipped running instance " + directoryName);
                    continue;
                }

                if (dryRun) {
                    skipped++;
                    details.add("would delete tmp/" + directoryName);
                    continue;
                }

                if (cleanup(path)) {
                    deleted++;
                    details.add("deleted tmp/" + directoryName);
                    continue;
                }

                failed++;
                details.add("failed tmp/" + directoryName);
            }
        } catch (Exception exception) {
            failed++;
            details.add("cleanup scan failed: " + exception.getMessage());
            LOGGER.warn("Failed to scan orphan Minecraft workspaces: " + exception.getMessage());
        }

        return new InstanceCleanupResult(scanned, deleted, skipped, failed, details.toString());
    }

    public List<String> javaFlags(Path workingDirectory) {
        Path flagsFile = workingDirectory.resolve("java-flags.txt");

        if (!Files.exists(flagsFile)) {
            return List.of();
        }

        try {
            return Files.readAllLines(flagsFile).stream().filter(line -> !line.isBlank()).toList();
        } catch (Exception exception) {
            LOGGER.warn("Failed to read java-flags.txt from " + workingDirectory + ": " + exception.getMessage());
            return List.of();
        }
    }

    public int javaVersion(Path workingDirectory) {
        Path javaVersionFile = workingDirectory.resolve("java-version.txt");

        if (Files.exists(javaVersionFile)) {
            try {
                return Integer.parseInt(Files.readString(javaVersionFile).trim());
            } catch (Exception exception) {
                return javaVersionFromProperties(workingDirectory);
            }
        }

        return javaVersionFromProperties(workingDirectory);
    }

    private int javaVersionFromProperties(Path workingDirectory) {
        Path versionProperties = workingDirectory.resolve("version.properties");

        if (!Files.exists(versionProperties)) {
            return 21;
        }

        try (var input = Files.newInputStream(versionProperties)) {
            Properties properties = new Properties();
            properties.load(input);
            String javaVersion = properties.getProperty("javaVersion");

            if (javaVersion == null || javaVersion.isBlank()) {
                return 21;
            }

            return Integer.parseInt(javaVersion);
        } catch (Exception exception) {
            return 21;
        }
    }

    private Path workingDirectory(ServiceStartRequestPacket packet) {
        if (packet.staticService()) {
            return staticDirectory.resolve(packet.serviceId());
        }

        return temporaryDirectory.resolve(packet.serviceId());
    }

    private void prepareStaticWorkspace(Path templateDirectory, Path workingDirectory) throws IOException {
        Files.createDirectories(staticDirectory);
        Files.createDirectories(workingDirectory);

        if (Files.exists(workingDirectory.resolve("server.jar"))) {
            return;
        }

        copyRecursively(templateDirectory, workingDirectory);
    }

    private void prepareTemporaryWorkspace(Path templateDirectory, Path workingDirectory) throws IOException {
        Files.createDirectories(temporaryDirectory);

        if (Files.exists(workingDirectory) && !cleanup(workingDirectory)) {
            throw new IOException("Temporary Minecraft workspace could not be cleaned before start: " + workingDirectory);
        }

        copyRecursively(templateDirectory, workingDirectory);
    }

    private void writeMinecraftFiles(ServiceStartRequestPacket packet, Path workingDirectory) throws IOException {
        Files.writeString(workingDirectory.resolve("eula.txt"), "eula=true\n");
        writeServerProperties(packet, workingDirectory);

        if (packet.serviceType() == CloudServiceType.PROXY) {
            writeProxyForwardingFiles(packet, workingDirectory);
            return;
        }

        writeBackendForwardingFiles(packet, workingDirectory);
    }

    private void writeServerProperties(ServiceStartRequestPacket packet, Path workingDirectory) throws IOException {
        Path propertiesFile = workingDirectory.resolve("server.properties");
        Properties properties = new Properties();

        if (Files.exists(propertiesFile)) {
            try (var input = Files.newInputStream(propertiesFile)) {
                properties.load(input);
            }
        }

        properties.setProperty("server-ip", packet.bindAddress());
        properties.setProperty("server-port", String.valueOf(packet.port()));
        properties.setProperty("query.port", String.valueOf(packet.port()));
        properties.setProperty("motd", "KryoCloud " + packet.serviceId());

        if (packet.serviceType() != CloudServiceType.PROXY) {
            properties.setProperty("online-mode", String.valueOf(packet.onlineMode()));
        }

        try (var output = Files.newOutputStream(propertiesFile)) {
            properties.store(output, "KryoCloud Minecraft instance properties");
        }
    }

    private void writeProxyForwardingFiles(ServiceStartRequestPacket packet, Path workingDirectory) throws IOException {
        if ("VELOCITY".equalsIgnoreCase(packet.forwardingMode())) {
            writeVelocityConfig(packet, workingDirectory);
            return;
        }

        if ("BUNGEECORD".equalsIgnoreCase(packet.forwardingMode())) {
            writeBungeeConfig(packet, workingDirectory);
        }
    }

    private void writeBackendForwardingFiles(ServiceStartRequestPacket packet, Path workingDirectory) throws IOException {
        if ("VELOCITY".equalsIgnoreCase(packet.forwardingMode())) {
            writeVelocityBackendConfig(packet, workingDirectory);
            return;
        }

        if ("BUNGEECORD".equalsIgnoreCase(packet.forwardingMode())) {
            writeBungeeBackendConfig(workingDirectory, true);
            writeVelocityBackendDisabled(workingDirectory);
            return;
        }

        writeBungeeBackendConfig(workingDirectory, false);
        writeVelocityBackendDisabled(workingDirectory);
    }

    private void writeVelocityConfig(ServiceStartRequestPacket packet, Path workingDirectory) throws IOException {
        Path config = workingDirectory.resolve("velocity.toml");
        String content = "config-version = \"2.7\"\n"
                + "bind = \"" + packet.bindAddress() + ":" + packet.port() + "\"\n"
                + "motd = \"KryoCloud Proxy\"\n"
                + "show-max-players = 100\n"
                + "online-mode = true\n"
                + "force-key-authentication = true\n"
                + "prevent-client-proxy-connections = false\n"
                + "player-info-forwarding-mode = \"modern\"\n"
                + "forwarding-secret-file = \"forwarding.secret\"\n"
                + "announce-forge = false\n"
                + "kick-existing-players = false\n"
                + "ping-passthrough = \"DISABLED\"\n"
                + "enable-player-address-logging = true\n\n"
                + "[servers]\n"
                + "lobby = \"127.0.0.1:1\"\n"
                + "try = [\"lobby\"]\n\n"
                + "[forced-hosts]\n";

        Files.writeString(config, content);
        Files.writeString(workingDirectory.resolve("forwarding.secret"), packet.forwardingSecret() + System.lineSeparator());
    }

    private void writeBungeeConfig(ServiceStartRequestPacket packet, Path workingDirectory) throws IOException {
        Path config = workingDirectory.resolve("config.yml");
        String content = "online_mode: true\n"
                + "ip_forward: true\n"
                + "listeners:\n"
                + "- query_port: " + packet.port() + "\n"
                + "  motd: KryoCloud Proxy\n"
                + "  tab_list: GLOBAL_PING\n"
                + "  query_enabled: false\n"
                + "  proxy_protocol: false\n"
                + "  forced_hosts: {}\n"
                + "  ping_passthrough: false\n"
                + "  priorities:\n"
                + "  - lobby\n"
                + "  bind_local_address: true\n"
                + "  host: " + packet.bindAddress() + ":" + packet.port() + "\n"
                + "  max_players: 100\n"
                + "  tab_size: 60\n"
                + "  force_default_server: true\n"
                + "servers:\n"
                + "  lobby:\n"
                + "    motd: KryoCloud bootstrap\n"
                + "    address: 127.0.0.1:1\n"
                + "    restricted: false\n";

        Files.writeString(config, content);
    }

    private void writeVelocityBackendConfig(ServiceStartRequestPacket packet, Path workingDirectory) throws IOException {
        writeBungeeBackendConfig(workingDirectory, false);

        Path directory = workingDirectory.resolve("config");
        Files.createDirectories(directory);

        Path global = directory.resolve("paper-global.yml");
        String content = read(global);

        if (content.isBlank()) {
            content = "proxies:\n"
                    + "  velocity:\n"
                    + "    enabled: true\n"
                    + "    online-mode: true\n"
                    + "    secret: \"" + yaml(packet.forwardingSecret()) + "\"\n"
                    + "  bungee-cord:\n"
                    + "    online-mode: false\n";
        }

        content = setPaperVelocityBlock(content, true, packet.forwardingSecret());
        Files.writeString(global, content);

        Path legacyPaper = workingDirectory.resolve("paper.yml");
        String legacy = read(legacyPaper);

        if (legacy.isBlank()) {
            legacy = "settings:\n"
                    + "  velocity-support:\n"
                    + "    enabled: true\n"
                    + "    online-mode: true\n"
                    + "    secret: \"" + yaml(packet.forwardingSecret()) + "\"\n";
        }

        legacy = setLegacyPaperVelocityBlock(legacy, true, packet.forwardingSecret());
        Files.writeString(legacyPaper, legacy);
    }

    private void writeVelocityBackendDisabled(Path workingDirectory) throws IOException {
        Path global = workingDirectory.resolve("config").resolve("paper-global.yml");

        if (Files.exists(global)) {
            Files.writeString(global, setPaperVelocityBlock(read(global), false, ""));
        }

        Path legacyPaper = workingDirectory.resolve("paper.yml");

        if (Files.exists(legacyPaper)) {
            Files.writeString(legacyPaper, setLegacyPaperVelocityBlock(read(legacyPaper), false, ""));
        }
    }

    private void writeBungeeBackendConfig(Path workingDirectory, boolean enabled) throws IOException {
        Path spigot = workingDirectory.resolve("spigot.yml");
        String content = read(spigot);

        if (content.isBlank()) {
            content = "settings:\n  bungeecord: " + enabled + "\n";
        }

        content = setSpigotBungee(content, enabled);
        Files.writeString(spigot, content);
    }

    private String setSpigotBungee(String content, boolean enabled) {
        if (java.util.regex.Pattern.compile("(?m)^\\s*bungeecord:\\s*(true|false).*$").matcher(content).find()) {
            return content.replaceAll("(?m)^(\\s*bungeecord:\\s*)(true|false).*$", "$1" + enabled);
        }

        if (java.util.regex.Pattern.compile("(?m)^settings:\\s*$").matcher(content).find()) {
            return content.replaceFirst("(?m)^settings:\\s*$", "settings:\n  bungeecord: " + enabled);
        }

        return content.stripTrailing() + "\nsettings:\n  bungeecord: " + enabled + "\n";
    }

    private String setPaperVelocityBlock(String content, boolean enabled, String secret) {
        String block = "proxies:\n"
                + "  velocity:\n"
                + "    enabled: " + enabled + "\n"
                + "    online-mode: true\n"
                + "    secret: \"" + yaml(secret) + "\"\n"
                + "  bungee-cord:\n"
                + "    online-mode: false\n";

        if (content.contains("proxies:")) {
            return content.replaceFirst("(?s)proxies:\\n(?:  .+\\n?)*", block);
        }

        return content.stripTrailing() + "\n" + block;
    }

    private String setLegacyPaperVelocityBlock(String content, boolean enabled, String secret) {
        String block = "settings:\n"
                + "  velocity-support:\n"
                + "    enabled: " + enabled + "\n"
                + "    online-mode: true\n"
                + "    secret: \"" + yaml(secret) + "\"\n";

        if (content.contains("velocity-support:")) {
            String updated = replaceOrAppend(content, "(?m)^(\\s*enabled:\\s*)(true|false).*$", "$1" + enabled, "    enabled: " + enabled);
            updated = replaceOrAppend(updated, "(?m)^(\\s*online-mode:\\s*)(true|false).*$", "$1true", "    online-mode: true");
            return replaceOrAppend(updated, "(?m)^(\\s*secret:\\s*).*$", "$1\"" + yaml(secret) + "\"", "    secret: \"" + yaml(secret) + "\"");
        }

        if (java.util.regex.Pattern.compile("(?m)^settings:\\s*$").matcher(content).find()) {
            return content.replaceFirst("(?m)^settings:\\s*$", "settings:\n  velocity-support:\n    enabled: " + enabled + "\n    online-mode: true\n    secret: \"" + yaml(secret) + "\"");
        }

        return content.stripTrailing() + "\n" + block;
    }

    private String upsertToml(String content, String key, String value) {
        return replaceOrAppend(content, "(?m)^(" + java.util.regex.Pattern.quote(key) + "\\s*=\\s*).*$", "$1" + value, key + " = " + value);
    }

    private String upsertYamlRoot(String content, String key, String value) {
        return replaceOrAppend(content, "(?m)^(" + java.util.regex.Pattern.quote(key) + ":\\s*).*$", "$1" + value, key + ": " + value);
    }

    private String replaceOrAppend(String content, String regex, String replacement, String appendLine) {
        if (java.util.regex.Pattern.compile(regex).matcher(content).find()) {
            return content.replaceAll(regex, replacement);
        }

        return content.stripTrailing() + "\n" + appendLine + "\n";
    }

    private String read(Path path) throws IOException {
        if (!Files.exists(path)) {
            return "";
        }

        return Files.readString(path);
    }

    private String yaml(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean cleanup(Path workingDirectory) {
        if (workingDirectory == null) {
            return true;
        }

        for (int attempt = 1; attempt <= 8; attempt++) {
            if (!Files.exists(workingDirectory)) {
                return true;
            }

            try {
                deleteRecursively(workingDirectory);

                if (!Files.exists(workingDirectory)) {
                    return true;
                }
            } catch (Exception exception) {
                LOGGER.warn("Cleanup attempt " + attempt + " failed for " + workingDirectory + ": " + exception.getMessage());
            }

            sleep(attempt * 250L);
        }

        return !Files.exists(workingDirectory);
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);

                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                    continue;
                }

                Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
