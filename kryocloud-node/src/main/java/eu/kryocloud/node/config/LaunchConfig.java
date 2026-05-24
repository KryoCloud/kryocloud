package eu.kryocloud.node.config;

import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;

import java.nio.file.Path;

public class LaunchConfig extends Config {

    @Comment("Internal KryoCloud name used for prompts, screen sessions and multi-cloud hosts")
    private String cloudName = "kryocloud";

    @Comment("Whether the first-start setup wizard has completed")
    private boolean setupComplete = false;

    @Comment("KryoCloud data directory used for configs, templates, versions and runtime files")
    private String dataDirectory = "";

    @Comment("Host to bind the node protocol to")
    private String host = "0.0.0.0";

    @Comment("Port to bind the node protocol to")
    private int port = 1130;

    @Comment("Host to bind the future web dashboard to")
    private String webHost = "127.0.0.1";

    @Comment("Port to bind the future web dashboard to")
    private int webPort = 8080;

    @Comment("File extension for files created by the node")
    private String fileExtension = ".yaml";

    @Comment("Update channel for the node")
    private String updateChannel = "stable";

    @Comment("Whether to automatically update the node")
    private boolean autoUpdate = true;

    public LaunchConfig(Path path) {
        super(path);
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public boolean isSetupComplete() {
        return setupComplete;
    }

    public void setSetupComplete(boolean setupComplete) {
        this.setupComplete = setupComplete;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
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

    public String getWebHost() {
        return webHost;
    }

    public void setWebHost(String webHost) {
        this.webHost = webHost;
    }

    public int getWebPort() {
        return webPort;
    }

    public void setWebPort(int webPort) {
        this.webPort = webPort;
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
