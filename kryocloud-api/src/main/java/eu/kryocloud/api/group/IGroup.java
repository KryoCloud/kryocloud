package eu.kryocloud.api.group;

import eu.kryocloud.api.service.IService;
import eu.kryocloud.api.service.ServiceType;

import java.util.Collection;
import java.util.UUID;

public interface IGroup {

    UUID uniqueId();

    String name();

    String javaVersion();

    String templateName();

    String software();

    String softwareVersion();

    ServiceType serviceType();

    Collection<IService> services();

    int serviceCount();

    int minCount();

    int maxCount();

    int minMemory();

    int maxMemory();

    int maxPlayers();

    int startNewPercent();

    int basePort();

    boolean staticServices();

    boolean installOnStart();

    void stopServices();
}
