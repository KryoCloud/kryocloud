package eu.kryocloud.api.service;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.player.IPlayer;
import eu.kryocloud.api.template.ITemplate;
import eu.kryocloud.api.worker.IWorker;

import java.util.Collection;
import java.util.UUID;

public interface IService {

    UUID uniqueId();

    String name();
    String javaVersion();
    String groupName();
    String templateName();
    String host();

    ServiceType serviceType();

    IGroup group();
    ITemplate template();
    IWorker worker();

    Collection<IPlayer> onlinePlayers();

    int serviceNumber();
    int minMemory();
    int maxMemory();
    int maxPlayers();
    int port();

    boolean staticDirectory();

    void start();
    void stop();
    void restart();

}
