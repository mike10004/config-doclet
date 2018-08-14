package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

public class MavenCoordinates {

    private static final String TYPE_JAR = "jar";
    private static final String TYPE_DEFAULT = TYPE_JAR;

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

    public String constructStandardFilename(@Nullable String type) {
        if (type == null) {
            type = TYPE_DEFAULT;
        }
        return String.format("%s-%s.%s", artifactId, version, type);
    }

    @SuppressWarnings("unused")
    public String getGroupId() {
        return groupId;
    }

    @SuppressWarnings("unused")
    public String getArtifactId() {
        return artifactId;
    }

    @SuppressWarnings("unused")
    public String getVersion() {
        return version;
    }

    @SuppressWarnings("unused")
    @Nullable
    public String getClassifier() {
        return classifier;
    }
}
