package eu.kryocloud.api.server;

public interface IServerManager {

    ICloudServer create(String name, String path, String jar);
    ICloudServer get(String name);
    void remove(String name);

}
