package eu.kryocloud.wrapper.config;

import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;

import java.nio.file.Path;

public final class WrapperLaunchConfig extends Config {

    @Comment("Internal KryoCloud name used for wrapper screen sessions")
    private String cloudName = "kryocloud";

    @Comment("Unique wrapper id used by the node to track this wrapper")
    private String wrapperId = "wrapper-1";

    @Comment("Node host address")
    private String nodeHost = "127.0.0.1";

    @Comment("Node protocol port")
    private int nodePort = 1130;

    @Comment("Shared protocol token used to authenticate against the node")
    private String token = "change-this-kryocloud-development-token-0001";

    @Comment("Address advertised to the node for Minecraft services running on this wrapper")
    private String advertisedAddress = "127.0.0.1";

    @Comment("Maximum memory in MB this wrapper should advertise")
    private int maxMemoryMb = 4096;

    @Comment("Directory containing managed Java runtimes. Relative paths are resolved from the KryoCloud data directory.")
    private String javaRuntimesDirectory = ".jdk";

    @Comment("Local host used by Minecraft plugins to connect to the KryoCloud plugin API")
    private String pluginApiHost = "127.0.0.1";

    @Comment("Local port used by Minecraft plugins to connect to the KryoCloud plugin API. Use 0 to bind a free local port automatically.")
    private int pluginApiPort = 0;

    @Comment("Seconds the wrapper waits for Minecraft readiness log output before marking a service as RUNNING")
    private int startupProbeSeconds = 90;

    @Comment("Seconds the wrapper waits after sending stop before forcing screen shutdown and deleting temporary workspace")
    private int shutdownTimeoutSeconds = 30;

    public WrapperLaunchConfig(Path path) {
        super(path);
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getWrapperId() {
        return wrapperId;
    }

    public void setWrapperId(String wrapperId) {
        this.wrapperId = wrapperId;
    }

    public String getNodeHost() {
        return nodeHost;
    }

    public void setNodeHost(String nodeHost) {
        this.nodeHost = nodeHost;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAdvertisedAddress() {
        return advertisedAddress;
    }

    public void setAdvertisedAddress(String advertisedAddress) {
        this.advertisedAddress = advertisedAddress;
    }

    public int getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }

    public String getJavaRuntimesDirectory() {
        return javaRuntimesDirectory;
    }

    public String getPluginApiHost() {
        return pluginApiHost;
    }

    public void setPluginApiHost(String pluginApiHost) {
        this.pluginApiHost = pluginApiHost;
    }

    public int getPluginApiPort() {
        return pluginApiPort;
    }

    public void setPluginApiPort(int pluginApiPort) {
        this.pluginApiPort = pluginApiPort;
    }

    public void setJavaRuntimesDirectory(String javaRuntimesDirectory) {
        this.javaRuntimesDirectory = javaRuntimesDirectory;
    }

    public int getStartupProbeSeconds() {
        return startupProbeSeconds;
    }

    public void setStartupProbeSeconds(int startupProbeSeconds) {
        this.startupProbeSeconds = startupProbeSeconds;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public void setShutdownTimeoutSeconds(int shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }
}
