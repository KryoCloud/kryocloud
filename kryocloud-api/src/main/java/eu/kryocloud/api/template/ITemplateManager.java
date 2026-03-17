package eu.kryocloud.api.template;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public interface ITemplateManager {

    void createTemplate(ITemplate template);
    void deleteTemplate(String name);

    boolean existsTemplate(String name);

    ITemplate template(String name);
    HashMap<String, ITemplate> templates();

}
