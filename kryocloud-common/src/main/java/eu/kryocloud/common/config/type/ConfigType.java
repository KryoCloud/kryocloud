package eu.kryocloud.common.config.type;

import eu.kryocloud.api.config.type.IConfigTypeProvider;

public enum ConfigType {

    PROPERTIES(new PropertiesTypeProvider(), ".cfg", true),
    YAML(new YamlTypeProvider(), ".yaml", true),
    TOML(new TomlTypeProvider(), ".toml", true),
    JSON(new JsonTypeProvider(), ".json", false);

    private final IConfigTypeProvider typeProvider;
    private final String ending;
    private final boolean comments;

    ConfigType(IConfigTypeProvider typeProvider, String ending, boolean comments) {
        this.typeProvider = typeProvider;
        this.ending = ending;
        this.comments = comments;
    }

    public IConfigTypeProvider getTypeProvider() {
        return typeProvider;
    }

    public String getEnding() {
        return ending;
    }

    public boolean supportsComments() {
        return comments;
    }

    public static ConfigType fromFileName(String fileName) {
        for (ConfigType type : values()) {
            if (fileName.endsWith(type.ending)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported config type: " + fileName);
    }
}
