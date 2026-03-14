package eu.kryocloud.api.template;

import java.nio.file.Path;
import java.util.UUID;

public interface ITemplate {

    UUID uniqueId();

    String name();

    Path path();

}
