package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class MavenDependencyListParser {

    public List<MavenRepositoryItem> parseList(Reader reader) throws IOException {
        ListLineParser parser = new ListLineParser();
        try (BufferedReader breader = new BufferedReader(reader)) {
            String line;
            while ((line = breader.readLine()) != null) {
                boolean keepGoing = parser.processLine(line);
                if (!keepGoing) {
                    break;
                }
            }
        }
        return Collections.unmodifiableList(parser.getResult());
    }

    private static final String TOKEN_SPLITTER_REGEX = ":";
    private static final int MAX_ARTIFACT_TOKENS = 6;

    protected MavenRepositoryItem parseLine(String line) {
        line = line.split("\\s*--\\s*", 2)[0];
        if (line.matches(".*\\Q (optional)\\E")) {
            line = StringUtils.removeEnd(line, " (optional)");
        }
        Iterator<String> tokens = Arrays.asList(line.split(TOKEN_SPLITTER_REGEX, MAX_ARTIFACT_TOKENS)).iterator();
        // e.g. "org.hamcrest:hamcrest-core:jar:1.3:test"
        checkState(tokens.hasNext(), "empty: %s", line);
        String groupId = tokens.next();
        checkState(tokens.hasNext(), "artifactId absent: %s", line);
        String artifactId = tokens.next();
        checkState(tokens.hasNext(), "type absent: %s", line);
        String type = tokens.next();
        checkState(tokens.hasNext(), "version absent: %s", line);
        String version = tokens.next();
        checkState(tokens.hasNext(), "scope: %s", line);
        String scope = tokens.next();
        @Nullable String rawArtifactPathname = null;
        if (tokens.hasNext()) {
            rawArtifactPathname = tokens.next();
        }
        Optional<File> f = Optional.ofNullable(rawArtifactPathname).map(File::new);
        MavenCoordinates coords = new MavenCoordinates(groupId, artifactId, version, null);
        MavenDependency dep = new MavenDependency(coords, type, scope);
        return new MavenRepositoryItem(dep, f.orElse(null), rawArtifactPathname);
    }

    private class ListLineParser {

        private final List<MavenRepositoryItem> entries = new ArrayList<>();

        public boolean processLine(String line) {
            if (line.startsWith(" ")) {
                line = line.trim();
                if (!line.isEmpty()) {
                    entries.add(parseLine(line));
                }
            }
            return true;
        }

        public List<MavenRepositoryItem> getResult() {
            return entries;
        }
    }


}
