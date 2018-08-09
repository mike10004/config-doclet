package io.github.mike10004.configdoclet;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class MavenDependency {

    public final MavenCoordinates artifact;
    public final String type;
    public final String scope;

    public MavenDependency(MavenCoordinates artifact, String type, String scope) {
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

//        private static final Ordering<MavenDependency> DEFAULT_ORDERING = MavenCoordinates.defaultOrdering().<MavenDependency>onResultOf(dep -> dep.artifact)
//                .compound(Ordering.<String>natural().<MavenDependency>onResultOf(dep -> Strings.nullToEmpty(dep.type)))
//                .compound(Ordering.<String>natural().<MavenDependency>onResultOf(dep -> Strings.nullToEmpty(dep.scope)));
//
//        public static Ordering<MavenDependency> defaultOrdering() {
//            return DEFAULT_ORDERING;
//        }
}
