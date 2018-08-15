package io.github.mike10004.configdoclet;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

public class MavenDependencyListParserTest {

    @Test
    public void parseAll() throws Exception {
        String text = "\n" +
                "The following files have been resolved:\n" +
                "   com.google.code.findbugs:jsr305:jar:3.0.2:compile:/home/mike/.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar (optional)  -- module jsr305 (auto)\n" +
                "   com.example:with-spaces-in-path:jar:2.0:compile:/home/John Smith/.m2/repository/com/example/with-spaces-in-path/with-spaces-in-path-2.0.jar (optional)  -- module gson (auto)\n" +
                "   com.google.code.gson:gson:jar:2.8.5:compile:/home/mike/.m2/repository/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar (optional)  -- module gson (auto)\n" +
                "\n";

        List<MavenRepositoryItem> items = new MavenDependencyListParser().parseList(new StringReader(text));
        MavenRepositoryItem item0 = items.get(0);
        assertNotNull(item0.artifactPathname);
        assertEquals(new File("/home/mike/.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar"), item0.artifactPathname);
        MavenRepositoryItem withSpaces = items.get(1);
        assertEquals("rawPathname", "/home/John Smith/.m2/repository/com/example/with-spaces-in-path/with-spaces-in-path-2.0.jar", withSpaces.rawPathname);
    }

    @Test
    public void parseAll_colors() throws Exception {
        String text = "\n" +
                "The following files have been resolved:\n" +
                "   com.google.code.findbugs:jsr305:jar:3.0.2:compile:/home/mike/.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar (optional) \u001B[36m -- module jsr305\u001B[0;1;33m (auto)\u001B[m\n" +
                "   org.apache.commons:commons-lang3:jar:3.6:compile:/home/mike/.m2/repository/org/apache/commons/commons-lang3/3.6/commons-lang3-3.6.jar\u001B[36m -- module org.apache.commons.lang3\u001B[0;1m [auto]\u001B[m\n" +
                "   com.google.code.gson:gson:jar:2.8.5:compile:/home/mike/.m2/repository/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar (optional) \u001B[36m -- module gson\u001B[0;1;33m (auto)\u001B[m\n" +
                "\n";

        List<MavenRepositoryItem> items = new MavenDependencyListParser().parseList(new StringReader(text));
        MavenRepositoryItem jsr = items.get(0);
        assertEquals("rawPathname jsr", "/home/mike/.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar", jsr.rawPathname);
        MavenRepositoryItem lang3 = items.get(1);
        assertEquals("artifactId", "commons-lang3", lang3.dependency.artifact.getArtifactId());
        assertEquals("rawPathname", "/home/mike/.m2/repository/org/apache/commons/commons-lang3/3.6/commons-lang3-3.6.jar", lang3.rawPathname);
        MavenRepositoryItem gson = items.get(2);
        assertEquals("gson rawPathname", "/home/mike/.m2/repository/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar", gson.rawPathname);
    }

    @Test
    public void parseAll_windows() throws Exception {
        Assume.assumeTrue("test is only for windows", SystemUtils.IS_OS_WINDOWS);
        String text = "The following files have been resolved:" + System.lineSeparator() +
                "   com.google.code.findbugs:jsr305:jar:3.0.2:compile:C:\\Users\\appveyor\\.m2\\repository\\com\\google\\code\\findbugs\\jsr305\\3.0.2\\jsr305-3.0.2.jar (optional)  -- module jsr305 (auto)" + System.lineSeparator() +
                "   com.google.code.gson:gson:jar:2.8.5:compile:C:\\Users\\appveyor\\.m2\\repository\\com\\google\\code\\gson\\gson\\2.8.5\\gson-2.8.5.jar (optional)  -- module gson (auto)" + System.lineSeparator() +
                System.lineSeparator();

        List<MavenRepositoryItem> items = new MavenDependencyListParser().parseList(new StringReader(text));
        MavenRepositoryItem item0 = items.get(0);
        assertNotNull(item0.artifactPathname);
        assertEquals(new File("C:\\Users\\appveyor\\.m2\\repository\\com\\google\\code\\findbugs\\jsr305\\3.0.2\\jsr305-3.0.2.jar"), item0.artifactPathname);
    }

    @Test
    public void parseLine_windows() throws Exception {
        String line = "   com.google.code.findbugs:jsr305:jar:3.0.2:compile:C:\\Users\\appveyor\\.m2\\repository\\com\\google\\code\\findbugs\\jsr305\\3.0.2\\jsr305-3.0.2.jar (optional)  -- module jsr305 (auto)";
        MavenRepositoryItem item = new MavenDependencyListParser().parseLine(line);
        System.out.format("parsed: %s%n", item);
        assertEquals("raw", "C:\\Users\\appveyor\\.m2\\repository\\com\\google\\code\\findbugs\\jsr305\\3.0.2\\jsr305-3.0.2.jar", item.rawPathname);
    }

    @Test
    public void trimTrailingWhitespace() {
        String s = "This is a string   \t\n";
        String actual = MavenDependencyListParser.trimTrailingWhitespace(s);
        assertEquals("trimmed", "This is a string", actual);

        assertEquals("trimmed nothing", "", MavenDependencyListParser.trimTrailingWhitespace(""));
        assertEquals("trimmed nothing", "NO_WHITESPACE", MavenDependencyListParser.trimTrailingWhitespace("NO_WHITESPACE"));
        assertEquals("trimmed everything", "", MavenDependencyListParser.trimTrailingWhitespace("\t\n    \r\n   "));
    }
}