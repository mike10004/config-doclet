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
                "   com.google.code.gson:gson:jar:2.8.5:compile:/home/mike/.m2/repository/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar (optional)  -- module gson (auto)\n" +
                "\n";

        List<MavenRepositoryItem> items = new MavenDependencyListParser().parseList(new StringReader(text));
        MavenRepositoryItem item0 = items.get(0);
        assertNotNull(item0.artifactPathname);
        assertEquals(new File("/home/mike/.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar"), item0.artifactPathname);
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
}