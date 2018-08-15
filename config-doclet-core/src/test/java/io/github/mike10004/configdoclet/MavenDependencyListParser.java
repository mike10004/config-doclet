package io.github.mike10004.configdoclet;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CharacterPredicate;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;

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

    static String trimTrailingWhitespace(String str) {
        return trimTrailing(str, Character::isWhitespace);
    }

    interface PrimitiveCharPredicate {
        boolean test(char ch);

        default PrimitiveCharPredicate or(PrimitiveCharPredicate other) {
            return ch -> PrimitiveCharPredicate.this.test(ch) || other.test(ch);
        }
    }

    static String retainWhile(String s, PrimitiveCharPredicate predicate) {
        for (int i = 0; i < s.length(); i++) {
            if (!predicate.test(s.charAt(i))) {
                return s.substring(0, i);
            }
        }
        return s;
    }

    static String trimTrailing(String str, PrimitiveCharPredicate predicate) {
        int stop = str.length();
        for (int i = str.length() - 1; i >= 0; i--) {
            char ch = str.charAt(i);
            if (!predicate.test(ch)) {
                break;
            }
            stop = i;
        }
        return str.substring(0, stop);
    }

    protected MavenRepositoryItem parseLine(String line) {
        line = line.split("\\s*--\\s*", 2)[0];
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
            rawArtifactPathname = cleanRawPathname(rawArtifactPathname);
        }
        Optional<File> f = Optional.ofNullable(rawArtifactPathname).map(File::new);
        MavenCoordinates coords = new MavenCoordinates(groupId, artifactId, version, null);
        MavenDependency dep = new MavenDependency(coords, type, scope);
        return new MavenRepositoryItem(dep, f.orElse(null), rawArtifactPathname);
    }

    static String cleanRawPathname(String s) {
        if (s.contains("(optional)")) {
            Matcher m = Pattern.compile("^(.*)\\(optional\\).*$").matcher(s);
            if (m.find()) {
                s = m.group(1);
            }
        }
        s = retainWhile(s, IS_ASCII_NON_CONTROL);
        s = trimTrailingWhitespace(s);
        return s;
    }

    private static final CharMatcher ASCII = CharMatcher.ascii();

    private static final PrimitiveCharPredicate IS_ASCII_NON_CONTROL = ch -> {
        return ASCII.matches(ch) && !Character.isISOControl(ch);
    };

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
