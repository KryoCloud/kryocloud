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

    @Comment("KryoSphere isolation mode: NONE, BASIC or STRICT")
    private String kryoSphereMode = "BASIC";

    @Comment("Use bubblewrap when KryoSphere mode is STRICT and bwrap is available")
    private boolean kryoSphereBubblewrap = true;

    @Comment("Give isolated Linux services a private /tmp when supported")
    private boolean kryoSpherePrivateTmp = true;

    @Comment("Hide the host home directory from strict Linux service namespaces when supported")
    private boolean kryoSphereProtectHome = true;

    @Comment("Use a restricted /proc view for strict Linux service namespaces when supported")
    private boolean kryoSphereRestrictProc = true;

    @Comment("Disallow privilege escalation for future KryoSphere launch backends when supported")
    private boolean kryoSphereNoNewPrivileges = true;

    @Comment("Optional hard virtual memory limit in MB for services. Use 0 to disable")
    private int kryoSphereMemoryLimitMb = 0;

    @Comment("Optional CPU limit percent for future KryoSphere launch backends. Use 0 to disable")
    private int kryoSphereCpuLimitPercent = 0;

    @Comment("Optional open file limit applied before service start. Use 0 to disable")
    private int kryoSphereOpenFileLimit = 65536;

    @Comment("Optional process limit applied before service start. Use 0 to disable")
    private int kryoSphereProcessLimit = 0;

    @Comment("Optional service user for future KryoSphere launch backends. Empty keeps current wrapper user")
    private String kryoSphereServiceUser = "";

    @Comment("Clear inherited environment variables for service processes where possible")
    private boolean kryoSphereClearEnvironment = true;

    @Comment("Allow inherited host networking. Disable only for services that do not need to bind/connect sockets")
    private boolean kryoSphereAllowNetwork = true;

    @Comment("Optional tmpfs size in MB for strict private /tmp. Use 0 for bubblewrap default")
    private int kryoSphereTmpSizeMb = 256;

    @Comment("Comma-separated extra read-only paths exposed to strict KryoSphere services")
    private String kryoSphereReadOnlyPaths = "";

    @Comment("Comma-separated extra writable paths exposed to strict KryoSphere services")
    private String kryoSphereWritablePaths = "";

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
    public String getKryoSphereMode() {
        return kryoSphereMode;
    }

    public void setKryoSphereMode(String kryoSphereMode) {
        this.kryoSphereMode = kryoSphereMode;
    }

    public boolean isKryoSphereBubblewrap() {
        return kryoSphereBubblewrap;
    }

    public void setKryoSphereBubblewrap(boolean kryoSphereBubblewrap) {
        this.kryoSphereBubblewrap = kryoSphereBubblewrap;
    }

    public boolean isKryoSpherePrivateTmp() {
        return kryoSpherePrivateTmp;
    }

    public void setKryoSpherePrivateTmp(boolean kryoSpherePrivateTmp) {
        this.kryoSpherePrivateTmp = kryoSpherePrivateTmp;
    }

    public boolean isKryoSphereProtectHome() {
        return kryoSphereProtectHome;
    }

    public void setKryoSphereProtectHome(boolean kryoSphereProtectHome) {
        this.kryoSphereProtectHome = kryoSphereProtectHome;
    }

    public boolean isKryoSphereRestrictProc() {
        return kryoSphereRestrictProc;
    }

    public void setKryoSphereRestrictProc(boolean kryoSphereRestrictProc) {
        this.kryoSphereRestrictProc = kryoSphereRestrictProc;
    }

    public boolean isKryoSphereNoNewPrivileges() {
        return kryoSphereNoNewPrivileges;
    }

    public void setKryoSphereNoNewPrivileges(boolean kryoSphereNoNewPrivileges) {
        this.kryoSphereNoNewPrivileges = kryoSphereNoNewPrivileges;
    }

    public int getKryoSphereMemoryLimitMb() {
        return kryoSphereMemoryLimitMb;
    }

    public void setKryoSphereMemoryLimitMb(int kryoSphereMemoryLimitMb) {
        this.kryoSphereMemoryLimitMb = kryoSphereMemoryLimitMb;
    }

    public int getKryoSphereCpuLimitPercent() {
        return kryoSphereCpuLimitPercent;
    }

    public void setKryoSphereCpuLimitPercent(int kryoSphereCpuLimitPercent) {
        this.kryoSphereCpuLimitPercent = kryoSphereCpuLimitPercent;
    }

    public int getKryoSphereOpenFileLimit() {
        return kryoSphereOpenFileLimit;
    }

    public void setKryoSphereOpenFileLimit(int kryoSphereOpenFileLimit) {
        this.kryoSphereOpenFileLimit = kryoSphereOpenFileLimit;
    }

    public int getKryoSphereProcessLimit() {
        return kryoSphereProcessLimit;
    }

    public void setKryoSphereProcessLimit(int kryoSphereProcessLimit) {
        this.kryoSphereProcessLimit = kryoSphereProcessLimit;
    }

    public String getKryoSphereServiceUser() {
        return kryoSphereServiceUser;
    }

    public void setKryoSphereServiceUser(String kryoSphereServiceUser) {
        this.kryoSphereServiceUser = kryoSphereServiceUser;
    }

    public boolean isKryoSphereClearEnvironment() {
        return kryoSphereClearEnvironment;
    }

    public void setKryoSphereClearEnvironment(boolean kryoSphereClearEnvironment) {
        this.kryoSphereClearEnvironment = kryoSphereClearEnvironment;
    }

    public boolean isKryoSphereAllowNetwork() {
        return kryoSphereAllowNetwork;
    }

    public void setKryoSphereAllowNetwork(boolean kryoSphereAllowNetwork) {
        this.kryoSphereAllowNetwork = kryoSphereAllowNetwork;
    }

    public int getKryoSphereTmpSizeMb() {
        return kryoSphereTmpSizeMb;
    }

    public void setKryoSphereTmpSizeMb(int kryoSphereTmpSizeMb) {
        this.kryoSphereTmpSizeMb = kryoSphereTmpSizeMb;
    }

    public String getKryoSphereReadOnlyPaths() {
        return kryoSphereReadOnlyPaths;
    }

    public void setKryoSphereReadOnlyPaths(String kryoSphereReadOnlyPaths) {
        this.kryoSphereReadOnlyPaths = kryoSphereReadOnlyPaths;
    }

    public String getKryoSphereWritablePaths() {
        return kryoSphereWritablePaths;
    }

    public void setKryoSphereWritablePaths(String kryoSphereWritablePaths) {
        this.kryoSphereWritablePaths = kryoSphereWritablePaths;
    }
}

