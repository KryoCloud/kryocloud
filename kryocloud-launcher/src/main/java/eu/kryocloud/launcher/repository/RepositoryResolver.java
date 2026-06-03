package eu.kryocloud.launcher.repository;

import java.util.List;

public final class RepositoryResolver {

    public List<Repository> repositories() {
        return List.of(
                new Repository("central", "https://repo1.maven.org/maven2"),
                new Repository("papermc", "https://repo.papermc.io/repository/maven-public")
        );
    }
}
