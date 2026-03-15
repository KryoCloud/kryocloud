package eu.kryocloud.node.config;

import eu.kryocloud.common.config.Config;

import java.nio.file.Path;

public class LaunchConfig extends Config {

    private String host;
    private int port;

    public LaunchConfig() {
        super(Path.of("launch."));
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
