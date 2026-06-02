package eu.kryocloud.api.plugin.logging;

public interface IPluginLogger {

    void info(String message);

    void success(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable throwable);

}
