package eu.kryocloud.node.config.group;

import eu.kryocloud.api.service.ServiceType;
import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;
import eu.kryocloud.node.group.CloudGroup;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class GroupConfig extends Config {

    @Comment("Unique group id")
    private String uniqueId = UUID.randomUUID().toString();

    @Comment("Group name")
    private String name = "Lobby";

    @Comment("Java executable or version identifier")
    private String javaVersion = "java";

    @Comment("Template used for services of this group")
    private String templateName = "Lobby";

    @Comment("Minecraft service type: SERVER, PROXY or LOBBY")
    private String serviceType = "LOBBY";

    @Comment("Minecraft software name from manifest/version storage")
    private String software = "paper";

    @Comment("Minecraft software version from manifest/version storage")
    private String softwareVersion = "latest";

    @Comment("IP address this group should bind Minecraft services to")
    private String bindAddress = "127.0.0.1";

    @Comment("Install and materialize software automatically before group start")
    private boolean installOnStart = true;

    @Comment("Amount of services started by default with start <group>")
    private int serviceCount = 1;

    @Comment("Minimum online services")
    private int minCount = 1;

    @Comment("Maximum online services")
    private int maxCount = 1;

    @Comment("Minimum memory in MB")
    private int minMemory = 512;

    @Comment("Maximum memory in MB")
    private int maxMemory = 1024;

    @Comment("Maximum players")
    private int maxPlayers = 100;

    @Comment("Start new service at usage percent")
    private int startNewPercent = 80;

    @Comment("Proxy port, or 0 to assign random backend ports")
    private int basePort = 0;

    @Comment("Use persistent service directory instead of temporary template copy")
    private boolean staticServices = false;

    public GroupConfig(Path path) {
        super(path);
    }

    public CloudGroup toGroup() {
        return new CloudGroup(UUID.fromString(requireNonBlank(uniqueId, "uniqueId")), requireNonBlank(name, "name"), requireNonBlank(javaVersion, "javaVersion"), requireNonBlank(templateName, "templateName"), requireNonBlank(software, "software"), requireNonBlank(softwareVersion, "softwareVersion"), requireNonBlank(bindAddress, "bindAddress"), parseServiceType(serviceType), List.of(), serviceCount, minCount, maxCount, minMemory, maxMemory, maxPlayers, startNewPercent, effectiveBasePort(), staticServices, installOnStart);
    }

    public String getSoftware() {
        return software;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public boolean isInstallOnStart() {
        return installOnStart;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public int getBasePort() {
        return basePort;
    }

    public boolean isStaticServices() {
        return staticServices;
    }

    public String getName() {
        return name;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public void setInstallOnStart(boolean installOnStart) {
        this.installOnStart = installOnStart;
    }

    public void setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
    }

    public void setMinCount(int minCount) {
        this.minCount = minCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public void setMinMemory(int minMemory) {
        this.minMemory = minMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setStartNewPercent(int startNewPercent) {
        this.startNewPercent = startNewPercent;
    }

    public void setBasePort(int basePort) {
        this.basePort = basePort;
    }

    public void setStaticServices(boolean staticServices) {
        this.staticServices = staticServices;
    }

    private int effectiveBasePort() {
        if ("PROXY".equalsIgnoreCase(serviceType)) {
            return basePort < 1 ? 25565 : basePort;
        }

        return 0;
    }

    private ServiceType parseServiceType(String value) {
        String normalized = requireNonBlank(value, "serviceType").toUpperCase();

        if ("SERVER".equals(normalized)) {
            return ServiceType.SERVER;
        }

        if ("PROXY".equals(normalized)) {
            return ServiceType.PROXY;
        }

        if ("LOBBY".equals(normalized)) {
            return ServiceType.LOBBY;
        }

        throw new IllegalArgumentException("Unsupported serviceType: " + value);
    }

    private String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
