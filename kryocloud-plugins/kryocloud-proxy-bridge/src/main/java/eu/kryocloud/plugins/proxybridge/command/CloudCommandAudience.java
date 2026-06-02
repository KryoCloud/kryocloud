package eu.kryocloud.plugins.proxybridge.command;

public interface CloudCommandAudience {

    boolean hasPermission(String permission);

    void sendMessage(String message);

}
