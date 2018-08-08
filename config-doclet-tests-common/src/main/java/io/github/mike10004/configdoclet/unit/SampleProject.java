package io.github.mike10004.configdoclet.unit;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SampleProject {

    private final String pomResourcePath;

    public SampleProject(String pomResourcePath) {
        this.pomResourcePath = pomResourcePath;
    }

    public void copyTestProject(Path destination) throws URISyntaxException, IOException {
        URL pomResource = getClass().getResource(pomResourcePath);
        String pomResourceStr = pomResource.toString();
        if (!pomResource.getProtocol().equals("file")) {
            throw new IllegalStateException("pom is not a file");
        }
        Path pomPath = Paths.get(pomResource.toURI());
        File pomParentDir = pomPath.getParent().toFile();
        FileUtils.copyDirectory(pomParentDir, destination.toFile());
    }

    public static SampleProject getDefault() {
        return new SampleProject("/documented-project/pom.xml");
    }
}
