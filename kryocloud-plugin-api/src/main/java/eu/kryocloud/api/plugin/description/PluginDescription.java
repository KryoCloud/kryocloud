package eu.kryocloud.api.plugin.description;

import eu.kryocloud.api.plugin.annotation.CloudPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record PluginDescription(String id, String name, String version, String description, List<String> authors, List<String> dependencies, List<String> softDependencies) {

    public PluginDescription {
        id = requireIdentifier(id, "id");
        name = requireText(name, "name");
        version = requireText(version, "version");
        description = description == null ? "" : description.trim();
        authors = normalizeList(authors, false);
        dependencies = normalizeList(dependencies, true);
        softDependencies = normalizeList(softDependencies, true);
    }

    public static PluginDescription simple(String id, String name, String version) {
        return builder()
                .id(id)
                .name(name)
                .version(version)
                .build();
    }

    public static PluginDescription from(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        CloudPlugin annotation = type.getAnnotation(CloudPlugin.class);

        if (annotation != null) {
            return builder()
                    .id(annotation.id())
                    .name(annotation.name())
                    .version(annotation.version())
                    .description(annotation.description())
                    .authors(List.of(annotation.authors()))
                    .dependencies(List.of(annotation.dependencies()))
                    .softDependencies(List.of(annotation.softDependencies()))
                    .build();
        }

        String simpleName = type.getSimpleName();
        String id = simpleName.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);

        return builder()
                .id(id)
                .name(simpleName)
                .version("1.0.0")
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private String name;
        private String version;
        private String description = "";
        private final List<String> authors = new ArrayList<>();
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> softDependencies = new ArrayList<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder author(String author) {
            if (author == null || author.isBlank()) {
                return this;
            }

            this.authors.add(author.trim());
            return this;
        }

        public Builder authors(List<String> authors) {
            if (authors == null) {
                return this;
            }

            authors.forEach(this::author);
            return this;
        }

        public Builder dependency(String dependency) {
            if (dependency == null || dependency.isBlank()) {
                return this;
            }

            this.dependencies.add(dependency.trim());
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            if (dependencies == null) {
                return this;
            }

            dependencies.forEach(this::dependency);
            return this;
        }

        public Builder softDependency(String dependency) {
            if (dependency == null || dependency.isBlank()) {
                return this;
            }

            this.softDependencies.add(dependency.trim());
            return this;
        }

        public Builder softDependencies(List<String> dependencies) {
            if (dependencies == null) {
                return this;
            }

            dependencies.forEach(this::softDependency);
            return this;
        }

        public PluginDescription build() {
            return new PluginDescription(id, name, version, description, authors, dependencies, softDependencies);
        }

    }

    private static String requireIdentifier(String value, String field) {
        String text = requireText(value, field).toLowerCase(Locale.ROOT);

        if (!text.matches("[a-z0-9][a-z0-9_-]{1,63}")) {
            throw new IllegalArgumentException(field + " must match [a-z0-9][a-z0-9_-]{1,63}");
        }

        return text;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value.trim();
    }

    private static List<String> normalizeList(List<String> values, boolean identifier) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(value -> identifier ? value.toLowerCase(Locale.ROOT) : value)
                .distinct()
                .toList();
    }

}
