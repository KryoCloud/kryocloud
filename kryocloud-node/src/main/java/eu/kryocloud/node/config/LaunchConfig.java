package eu.kryocloud.node.config;

import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;

import java.nio.file.Path;
import eu.kryocloud.common.config.ConfigPathResolver;

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


    @Comment("File extension for files created by the node")
    private String fileExtension = ".yaml";

    @Comment("Whether the config format setup question has been answered")
    private boolean configFormatConfigured = false;

    @Comment("Internal setup version for the config format migration")
    private int configFormatSetupVersion = 0;

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


    public String getFileExtension() {
        return fileExtension;
    }

    public boolean isConfigFormatConfigured() {
        return configFormatConfigured;
    }

    public void setConfigFormatConfigured(boolean configFormatConfigured) {
        this.configFormatConfigured = configFormatConfigured;
    }

    public int getConfigFormatSetupVersion() {
        return configFormatSetupVersion;
    }

    public void setConfigFormatSetupVersion(int configFormatSetupVersion) {
        this.configFormatSetupVersion = configFormatSetupVersion;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = ConfigPathResolver.normalizeExtension(fileExtension);
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
