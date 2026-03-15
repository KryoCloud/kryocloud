package eu.kryocloud.common.config.type;

import eu.kryocloud.api.config.type.IConfigTypeProvider;

public enum ConfigType {

    YAML(new YamlTypeProvider(), ".yaml"),
    TOML(new TomlTypeProvider(), ".toml");

    private final IConfigTypeProvider typeProvider;
    private final String ending;

    ConfigType(IConfigTypeProvider typeProvider, String ending) {
        this.typeProvider = typeProvider;
        this.ending = ending;
    }

    public IConfigTypeProvider getTypeProvider() {
        return typeProvider;
    }

    public String getEnding() {
        return ending;
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
