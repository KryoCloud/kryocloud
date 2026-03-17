package eu.kryocloud.node.config;

import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;

import java.nio.file.Path;

public class LaunchConfig extends Config {

    @Comment("Host to bind the node to")
    private String host = "127.0.0.1";

    @Comment("Port to bind the node to")
    private int port = 1130;

    @Comment("File extension for files created by the node")
    private String fileExtension = ".yaml";

    @Comment("Update channel for the node")
    private String updateChannel = "stable";

    @Comment("Whether to automatically update the node")
    private boolean autoUpdate = true;

    public LaunchConfig(Path path) {
        super(path);
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

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getUpdateChannel() {
        return updateChannel;
    }

    public void setUpdateChannel(String updateChannel) {
        this.updateChannel = updateChannel;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }
}
