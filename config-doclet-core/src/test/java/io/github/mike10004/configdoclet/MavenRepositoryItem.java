package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import java.io.File;

import static java.util.Objects.requireNonNull;

public class MavenRepositoryItem {
    public final MavenDependency dependency;
    @Nullable
    public final File artifactPathname;

    public MavenRepositoryItem(MavenDependency dependency, @Nullable File artifactPathname) {
        this.dependency = requireNonNull(dependency);
        this.artifactPathname = artifactPathname;
    }
}
