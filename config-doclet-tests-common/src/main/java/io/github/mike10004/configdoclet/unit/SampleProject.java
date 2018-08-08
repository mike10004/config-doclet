package io.github.mike10004.configdoclet.unit;

import com.google.common.collect.Ordering;
import com.sun.nio.file.ExtendedCopyOption;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public class SampleProject {

    private final String pomResourcePath;

    public SampleProject(String pomResourcePath) {
        this.pomResourcePath = pomResourcePath;
    }

    static void unpackJar(URL pomUrl, Path destDir) throws IOException {
        JarURLConnection conn = (JarURLConnection) pomUrl.openConnection();
        JarFile jarFile = conn.getJarFile();
        String pomEntryName = conn.getEntryName();
        String pomParentDir = FilenameUtils.getPathNoEndSeparator(pomEntryName) + "/";
        Predicate<? super JarEntry> filter = entry -> {
            return entry.getName().startsWith(pomParentDir);
        };
        Function<String, String> nameMapper = entryName -> {
            return StringUtils.removeStart(entryName, pomParentDir);
        };
        List<JarEntry> relevants = jarFile.stream().filter(filter)
                .sorted(orderingByEntryNameLengthThenAlphabetical())
                .collect(Collectors.toList());
        for (JarEntry entry : relevants) {
            Path destFilePathname = destDir.resolve(nameMapper.apply(entry.getName()));
            if (entry.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                destFilePathname.toFile().mkdirs();
                continue;
            }
            File destFileParent = destFilePathname.toFile().getParentFile();
            if (!destFileParent.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                destFileParent.mkdirs();
            }
            try (InputStream entryInput = jarFile.getInputStream(entry)) {
                java.nio.file.Files.copy(entryInput, destFilePathname);
            }
        }
    }

    public void copyTestProject(Path destination) throws URISyntaxException, IOException {
        URL pomResource = getClass().getResource(pomResourcePath);
        if (pomResource == null) {
            throw new FileNotFoundException(pomResourcePath);
        }
        switch (pomResource.getProtocol()) {
            case "file":
                Path pomPath = Paths.get(pomResource.toURI());
                File pomParentDir = pomPath.getParent().toFile();
                FileUtils.copyDirectory(pomParentDir, destination.toFile());
                break;
            case "jar":
                unpackJar(pomResource, destination);
                break;
            default:
                throw new IOException("unsupported protocol: " + pomResource);
        }
    }

    public static SampleProject getDefault() {
        return new SampleProject("/documented-project/pom.xml");
    }

    private static Comparator<JarEntry> orderingByEntryNameLengthThenAlphabetical() {
        Ordering<JarEntry> nameLengthOrdering = Ordering.<Integer>natural().onResultOf(entry -> entry.getName().length());
        Ordering<JarEntry> nameAlphaOrdering = Ordering.<String>natural().onResultOf(JarEntry::getName);
        return nameLengthOrdering.thenComparing(nameAlphaOrdering);
    }
}
