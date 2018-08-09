package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class MavenCoordinates {

    private final String groupId;
    private final String artifactId;
    private final String version;
    @Nullable
    private final String classifier;

    public MavenCoordinates(String groupId, String artifactId, String version, @Nullable String classifier) {
        this.groupId = checkArg(groupId, "groupId");
        this.artifactId = checkArg(artifactId, "artifactId");
        this.version = checkArg(version, "version");
        this.classifier = classifier;
    }

    private static String checkArg(String value, String fieldName) {
        checkArgument(value != null && !value.isEmpty(), "%s null or empty", fieldName);
        checkArgument(!value.startsWith("${"), "invalid %s: %s", fieldName, value);
        return value;
    }

//        public static MavenCoordinates fromProperties(Properties p) {
//            return new MavenCoordinates(p.getProperty("project.groupId"), p.getProperty("project.artifactId"), p.getProperty("project.version"), p.getProperty("project.classifier"));
//        }

    @Override
    public String toString() {
        return classifier == null
                ? String.format("%s:%s:%s", groupId, artifactId, version)
                : String.format("%s:%s:%s:%s", groupId, artifactId, version, classifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenCoordinates that = (MavenCoordinates) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(classifier, that.classifier);
    }

    @Override
    public int hashCode() {

        return Objects.hash(groupId, artifactId, version, classifier);
    }

    private static final String TYPE_JAR = "jar";
    private static final String TYPE_DEFAULT = TYPE_JAR;

    public String constructStandardFilename(@Nullable String type) {
        if (type == null) {
            type = TYPE_DEFAULT;
        }
        return String.format("%s-%s.%s", artifactId, version, type);
    }

//        public String constructStandardJarFilename() {
//            return constructStandardFilename(TYPE_JAR);
//        }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    @SuppressWarnings("SameParameterValue")
    public String getClassifier(String defaultValue) {
        requireNonNull(defaultValue, "defaultValue");
        return classifier == null ? defaultValue : classifier;
    }

//        private static final Ordering<String> STRING_ORDERING = Ordering.natural();
//
//        private static Ordering<MavenCoordinates> orderingByString(Function<MavenCoordinates, String> fn) {
//            return STRING_ORDERING.onResultOf(fn::apply);
//        }
//
//        private static final Ordering<MavenCoordinates> DEFAULT_ORDERING = orderingByString(MavenCoordinates::getGroupId)
//                .compound(orderingByString(MavenCoordinates::getArtifactId))
//                .compound(orderingByString(MavenCoordinates::getVersion))
//                .compound(orderingByString(coords -> coords.getClassifier("")));
//
//        public static Ordering<MavenCoordinates> defaultOrdering() {
//            return DEFAULT_ORDERING;
//        }
}
