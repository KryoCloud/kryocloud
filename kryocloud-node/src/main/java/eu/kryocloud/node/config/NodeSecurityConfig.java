package eu.kryocloud.node.config;

import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;

import java.nio.file.Path;

public final class NodeSecurityConfig extends Config {

    @Comment("Shared protocol token used by wrappers and plugins to authenticate against the node")
    private String token = "change-this-kryocloud-development-token-0001";

    public NodeSecurityConfig(Path path) {
        super(path);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}