package io.github.mike10004.configdoclet.unit;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.*;

public class SampleProjectTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @org.junit.Test
    public void copyTestProject() throws Exception {
        File dir = temporaryFolder.newFolder();
        SampleProject.getDefault().copyTestProject(dir.toPath());
        Collection<File> files = FileUtils.listFiles(dir, null, true);
        assertEquals("pom.xml", 1L, files.stream().filter(f -> "pom.xml".equals(f.getName())).count());
        assertEquals("App.java", 1L, files.stream().filter(f -> "pom.xml".equals(f.getName())).count());
    }

    @Test
    public void unpackJar() throws Exception {
        URI jarFileUri = getClass().getResource("/example-built.jar").toURI();
        checkState("file".equals(jarFileUri.getScheme()));
        URL pomUrl = new URL("jar:" + jarFileUri.toString() + "!" + "/documented-project/pom.xml");
        System.out.format("pom URL: %s%n", pomUrl);
        Path dir = temporaryFolder.newFolder().toPath();
        SampleProject.unpackJar(pomUrl, dir);
        Path parent = temporaryFolder.getRoot().toPath();
        Collection<File> unpacked = FileUtils.listFiles(dir.toFile(), null, true);
        unpacked.forEach(file -> {
            System.out.format("%s%n", parent.relativize(file.toPath()));
        });
        File expectedPomFile = dir.resolve("pom.xml").toFile();
        assertTrue(expectedPomFile + " exists", unpacked.stream().anyMatch(expectedPomFile::equals));
        File expectedAppJavaFile = dir.resolve("src/main/java/com/example/App.java").toFile();
        assertTrue(expectedAppJavaFile + " exists", unpacked.stream().anyMatch(expectedAppJavaFile::equals));
    }
}