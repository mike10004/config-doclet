package io.github.mike10004.configdoclet.unit;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collection;

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
}