package eu.kryocloud.node.group;

import eu.kryocloud.api.service.ServiceType;
import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;

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
    private String templateName = "default";

    @Comment("Service type: SERVER, PROXY or LOBBY")
    private String serviceType = "SERVER";

    @Comment("Amount of services currently expected by default")
    private int serviceCount = 1;

    @Comment("Minimum amount of online services")
    private int minCount = 1;

    @Comment("Maximum amount of online services")
    private int maxCount = 1;

    @Comment("Minimum memory in MB")
    private int minMemory = 512;

    @Comment("Maximum memory in MB")
    private int maxMemory = 1024;

    @Comment("Maximum player count")
    private int maxPlayers = 100;

    @Comment("Start a new service when this percent is reached")
    private int startNewPercent = 80;

    @Comment("First port used when starting services manually from this group")
    private int basePort = 25565;

    @Comment("Whether services from this group use static directories")
    private boolean staticServices = false;

    public GroupConfig(Path path) {
        super(path);
    }

    public CloudGroup toGroup() {
        return new CloudGroup(UUID.fromString(requireNonBlank(uniqueId, "uniqueId")), requireNonBlank(name, "name"), requireNonBlank(javaVersion, "javaVersion"), requireNonBlank(templateName, "templateName"), parseServiceType(serviceType), List.of(), serviceCount, minCount, maxCount, minMemory, maxMemory, maxPlayers, startNewPercent);
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
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }

        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}