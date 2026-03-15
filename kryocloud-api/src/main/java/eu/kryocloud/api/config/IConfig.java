package eu.kryocloud.api.config;

public interface IConfig {

    void load();
    void save();
    void reload();
    void addDefault(String key, Object value);
    void put(String key, Object value);
    <T> T get(String key, Class<T> type);

}
