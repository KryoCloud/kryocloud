package eu.kryocloud.api.screen;

public interface IScreen {

    void start(String command) throws Exception;

    void send(String command) throws Exception;

    String capture() throws Exception;

    void stop() throws Exception;

    boolean exists() throws Exception;

}
