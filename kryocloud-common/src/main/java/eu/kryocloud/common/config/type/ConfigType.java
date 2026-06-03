package eu.kryocloud.common.config.type;

import eu.kryocloud.api.config.type.IConfigTypeProvider;

import java.util.Locale;

public enum ConfigType {

    PROPERTIES(new PropertiesTypeProvider(), ".cfg", true, "cfg", "properties", "property"),
    YAML(new YamlTypeProvider(), ".yaml", true, "yaml", "yml"),
    TOML(new TomlTypeProvider(), ".toml", true, "toml"),
    JSON(new JsonTypeProvider(), ".json", false, "json");

    private final IConfigTypeProvider typeProvider;
    private final String ending;
    private final boolean comments;
    private final String[] aliases;

    ConfigType(IConfigTypeProvider typeProvider, String ending, boolean comments, String... aliases) {
        this.typeProvider = typeProvider;
        this.ending = ending;
        this.comments = comments;
        this.aliases = aliases;
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

    public boolean matchesFileName(String fileName) {
        String normalized = normalize(fileName);
        return normalized.endsWith(ending) || this == YAML && normalized.endsWith(".yml");
    }

    public static ConfigType fromFileName(String fileName) {
        for (ConfigType type : values()) {
            if (type.matchesFileName(fileName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported config type: " + fileName);
    }

    public static ConfigType fromName(String value) {
        String normalized = normalize(value).replace(".", "");

        for (ConfigType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }

            for (String alias : type.aliases) {
                if (alias.equalsIgnoreCase(normalized)) {
                    return type;
                }
            }
        }

        throw new IllegalArgumentException("Unsupported config type: " + value);
    }

    public static String supportedNames() {
        return "yaml, toml, json, cfg";
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
