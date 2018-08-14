package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class MavenRepositoryItem {

    public final MavenDependency dependency;

    @Nullable
    public final File artifactPathname;

    public MavenRepositoryItem(MavenDependency dependency, @Nullable File artifactPathname) {
        this.dependency = requireNonNull(dependency);
        this.artifactPathname = artifactPathname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MavenRepositoryItem)) return false;
        MavenRepositoryItem that = (MavenRepositoryItem) o;
        return Objects.equals(dependency, that.dependency) &&
                Objects.equals(artifactPathname, that.artifactPathname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency, artifactPathname);
    }

    @Override
    public String toString() {
        return "MavenRepositoryItem{" +
                "dependency=" + dependency +
                ", artifactPathname=" + artifactPathname +
                '}';
    }
}
