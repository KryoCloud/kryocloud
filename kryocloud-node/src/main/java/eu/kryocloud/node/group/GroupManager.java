package eu.kryocloud.node.group;

import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.group.IGroupManager;
import eu.kryocloud.common.config.type.ConfigType;
import eu.kryocloud.node.config.LaunchConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public final class GroupManager implements IGroupManager {

    private final ConcurrentMap<UUID, IGroup> groupsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> groupIdByName = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, GroupConfig> configsByName = new ConcurrentHashMap<>();
    private final Path groupsDirectory = Path.of("groups");
    private final ConfigType configType;

    public GroupManager(IConfigProvider configProvider) {
        if (configProvider == null) {
            throw new IllegalArgumentException("configProvider must not be null");
        }

        LaunchConfig launchConfig = configProvider.getConfig(LaunchConfig.class);

        if (launchConfig == null) {
            throw new IllegalStateException("LaunchConfig must be registered before GroupManager starts");
        }

        configType = ConfigType.fromFileName("group" + launchConfig.getFileExtension());
        loadGroups();
        createDefaultGroupIfMissing();
    }

    @Override
    public void createGroup(IGroup group) {
        if (group == null) {
            throw new IllegalArgumentException("group must not be null");
        }

        groupsById.put(group.uniqueId(), group);
        groupIdByName.put(normalize(group.name()), group.uniqueId());
    }

    @Override
    public void deleteGroup(UUID uniqueId) {
        if (uniqueId == null) {
            throw new IllegalArgumentException("uniqueId must not be null");
        }

        IGroup group = groupsById.remove(uniqueId);

        if (group == null) {
            return;
        }

        groupIdByName.remove(normalize(group.name()));
        configsByName.remove(normalize(group.name()));

        try {
            Files.deleteIfExists(groupsDirectory.resolve(group.name() + configType.getEnding()));
        } catch (Exception exception) {
            throw new RuntimeException("Failed to delete group config " + group.name(), exception);
        }
    }

    @Override
    public boolean existsGroup(String name) {
        validateName(name);
        return groupIdByName.containsKey(normalize(name));
    }

    @Override
    public IGroup groupById(UUID uniqueId) {
        if (uniqueId == null) {
            throw new IllegalArgumentException("uniqueId must not be null");
        }

        return groupsById.get(uniqueId);
    }

    @Override
    public IGroup groupByName(String name) {
        validateName(name);

        UUID uniqueId = groupIdByName.get(normalize(name));

        if (uniqueId == null) {
            return null;
        }

        return groupsById.get(uniqueId);
    }

    @Override
    public Collection<IGroup> groups() {
        return groupsById.values().stream().sorted(Comparator.comparing(IGroup::name)).toList();
    }

    public Optional<GroupConfig> config(String groupName) {
        validateName(groupName);
        return Optional.ofNullable(configsByName.get(normalize(groupName)));
    }

    private void loadGroups() {
        try {
            Files.createDirectories(groupsDirectory);

            try (Stream<Path> paths = Files.list(groupsDirectory)) {
                paths.filter(Files::isRegularFile).filter(this::isGroupConfig).sorted(Comparator.comparing(Path::getFileName)).forEach(this::loadGroup);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load groups", exception);
        }
    }

    private void loadGroup(Path path) {
        try {
            GroupConfig config = new GroupConfig(path);
            config.load();
            config.save();

            CloudGroup group = config.toGroup();
            createGroup(group);
            configsByName.put(normalize(group.name()), config);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load group config " + path, exception);
        }
    }

    private void createDefaultGroupIfMissing() {
        if (existsGroup("Lobby")) {
            return;
        }

        Path path = groupsDirectory.resolve("Lobby" + configType.getEnding());
        GroupConfig config = new GroupConfig(path);
        config.save();

        CloudGroup group = config.toGroup();
        createGroup(group);
        configsByName.put(normalize(group.name()), config);
    }

    private boolean isGroupConfig(Path path) {
        return path.getFileName().toString().endsWith(configType.getEnding());
    }

    private void validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    private String normalize(String value) {
        return value.toLowerCase();
    }
}