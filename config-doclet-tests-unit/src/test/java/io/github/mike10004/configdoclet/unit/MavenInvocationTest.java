package io.github.mike10004.configdoclet.unit;

import com.google.common.io.Files;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenInvocationTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static File mavenHomeDir;

    @BeforeClass
    public static void detectMavenHome() throws IOException {
        mavenHomeDir = resolveMavenHome();
    }

    @Test
    public void invoke() throws Exception {
        Path projectDir = temporaryFolder.newFolder().toPath();
        SampleProject.getDefault().copyTestProject(projectDir);
        InvocationRequest request = new DefaultInvocationRequest();
        request.setInputStream(new ByteArrayInputStream(new byte[0]));
        request.setPomFile(projectDir.resolve("pom.xml").toFile());
        request.setGoals(Collections.singletonList("javadoc:javadoc"));
        Properties properties = new Properties();
        properties.setProperty("java.home", System.getProperty("java.home"));
        request.setProperties(properties);
        System.out.format("passing JAVA_HOME=%s%n", System.getProperty("java.home"));
        request.addShellEnvironment("JAVA_HOME", System.getProperty("java.home"));
        TestConfig tc = Tests.config();
        request.addShellEnvironment("DOCLET_ARTIFACT_GROUP_ID", tc.get("project.groupId"));
        request.addShellEnvironment("DOCLET_ARTIFACT_ARTIFACT_ID", "config-doclet-core");
        request.addShellEnvironment("DOCLET_ARTIFACT_VERSION", tc.get("project.version"));
        DefaultInvoker invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHomeDir);
        invoker.setWorkingDirectory(temporaryFolder.newFolder());
        InvocationResult result = invoker.execute(request);
        CommandLineException exception = result.getExecutionException();
        if (exception != null) {
            exception.printStackTrace(System.out);
        }
        assertEquals("exit code", 0, result.getExitCode());
        File outputFile = projectDir.resolve("target")
                .resolve("site/apidocs") // maven-javadoc-plugin default
                .resolve("config-help")
                .resolve(DEFAULT_OUTPUT_FILENAME).toFile();
        assertTrue("exists: " + outputFile, outputFile.isFile());
        String content = Files.asCharSource(outputFile, UTF_8).read();
        System.out.println(content);
        assertFalse("content nonempty", content.trim().isEmpty());
    }

    private static final String DEFAULT_OUTPUT_FILENAME = "config-doclet-output.txt";

    private static File resolveMavenHome() throws IOException {
        for (Supplier<String> supplier : getMavenHomePathnameSuppliers()) {
            String dir = supplier.get();
            if (dir != null) {
                File dirPathname = new File(dir);
                if (dirPathname.isDirectory()) {
                    File mvnExecutable = new File(dirPathname, "bin/mvn");
                    if (!mvnExecutable.isFile()) {
                        System.err.format("resolved maven home at %s but executable %s does not exist%n", dirPathname.getAbsolutePath(), mvnExecutable.getAbsolutePath());
                    }
                }
                return dirPathname;
            }
        }
        throw new IOException("maven home directory not found");
    }

    private static Iterable<Supplier<String>> getMavenHomePathnameSuppliers() {
        return java.util.List.of(
                () -> System.getProperty("configdoclet.build.maven.home"),
                () -> System.getenv("M2_HOME"),
                () -> {
                    String[] pathDirs = System.getenv("PATH").split(Pattern.quote(File.pathSeparator));
                    return Arrays.stream(pathDirs)
                            .flatMap(dir -> {
                                return Stream.of("mvn", "mvnw.bat").map(binname -> new File(dir, binname));
                            }).filter(File::isFile)
                            .map(File::getParentFile) // to Maven home/bin dir
                            .filter(Objects::nonNull)
                            .map(File::getParentFile) // to Maven home dir
                            .filter(Objects::nonNull)
                            .map(File::getAbsolutePath)
                            .findFirst().orElse(null);
                },
                () -> "/usr/local/share/maven",
                () -> "/usr/share/maven"

        );
    }

}
