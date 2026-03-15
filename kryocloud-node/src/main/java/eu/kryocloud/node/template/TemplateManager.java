package eu.kryocloud.node.template;

import eu.kryocloud.api.template.ITemplate;
import eu.kryocloud.api.template.ITemplateManager;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class TemplateManager implements ITemplateManager {

    @Override
    public void createTemplate(ITemplate template) {

    }

    @Override
    public void deleteTemplate(UUID uniqueId) {

    }

    @Override
    public boolean existsTemplate(String name) {
        return false;
    }

    @Override
    public ITemplate templateById(UUID uniqueId) {
        return null;
    }

    @Override
    public ITemplate templateByName(String name) {
        return null;
    }

    @Override
    public Collection<ITemplate> templates() {
        return List.of();
    }
}
