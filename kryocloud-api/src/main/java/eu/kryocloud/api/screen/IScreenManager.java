package eu.kryocloud.api.screen;

public interface IScreenManager {

    IScreen create(String session);

    IScreen get(String session);

    void remove(String session);

}
