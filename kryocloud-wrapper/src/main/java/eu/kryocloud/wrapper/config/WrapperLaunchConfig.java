package eu.kryocloud.wrapper.config;

import eu.kryocloud.common.config.Config;

import java.nio.file.Path;

public final class WrapperLaunchConfig extends Config {

    private String wrapperId = "wrapper-1";
    private String nodeHost = "127.0.0.1";
    private int nodePort = 19132;
    private String token = "change-this-kryocloud-development-token-0001";
    private String advertisedAddress = "127.0.0.1";
    private String templatesDirectory = "templates";
    private String servicesDirectory = "services";
    private int maxMemoryMb = 4096;

    public WrapperLaunchConfig(Path file) {
        super(file);
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

    public String getTemplatesDirectory() {
        return templatesDirectory;
    }

    public void setTemplatesDirectory(String templatesDirectory) {
        this.templatesDirectory = templatesDirectory;
    }

    public String getServicesDirectory() {
        return servicesDirectory;
    }

    public void setServicesDirectory(String servicesDirectory) {
        this.servicesDirectory = servicesDirectory;
    }

    public int getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }
}