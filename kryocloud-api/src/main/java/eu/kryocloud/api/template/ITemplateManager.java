package eu.kryocloud.api.template;

import java.util.Collection;
import java.util.UUID;

public interface ITemplateManager {

    void createTemplate(ITemplate template);
    void deleteTemplate(UUID uniqueId);

    boolean existsTemplate(String name);

    ITemplate templateById(UUID uniqueId);
    ITemplate templateByName(String name);
    Collection<ITemplate> templates();

}
