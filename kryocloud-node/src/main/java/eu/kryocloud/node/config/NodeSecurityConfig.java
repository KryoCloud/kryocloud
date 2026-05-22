package eu.kryocloud.node.config;

import eu.kryocloud.common.config.Config;

import java.nio.file.Path;

public final class NodeSecurityConfig extends Config {

    private String token = "change-this-kryocloud-development-token-0001";

    public NodeSecurityConfig(Path file) {
        super(file);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}