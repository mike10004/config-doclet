package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class MavenDependency {

    public final MavenCoordinates artifact;
    @Nullable
    public final String type;
    @Nullable
    public final String scope;

    public MavenDependency(MavenCoordinates artifact, @Nullable String type, @Nullable String scope) {
        this.artifact = requireNonNull(artifact, "artifact");
        this.type = type;
        this.scope = scope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenDependency that = (MavenDependency) o;
        return Objects.equals(artifact, that.artifact) &&
                Objects.equals(type, that.type) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact, type, scope);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
                .add("artifact", artifact)
                .add("type", type)
                .add("scope", scope)
                .toString();
    }

    @SuppressWarnings("unused")
    public String constructFilename() {
        return artifact.constructStandardFilename(type);
    }

}
