package io.github.mike10004.configdoclet.its;

import com.google.common.io.Files;
import io.github.mike10004.configdoclet.tests.SampleProject;
import io.github.mike10004.configdoclet.tests.TestConfig;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
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
        request.setGoals(Arrays.asList("prepare-package", "--quiet", "--batch-mode"));
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
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(outputFile)) {
            p.load(in);
        }
        assertEquals("output file is all properties comments", 0, p.size());
    }

    private static final String DEFAULT_OUTPUT_FILENAME = "config-doclet-output.properties";

    private static File resolveMavenHome() throws IOException {
        for (MavenHomeSupplier supplier : getMavenHomePathnameSuppliers()) {
            String dir = supplier.getMavenHomePathname();
            if (dir != null) {
                System.out.format("maven home resolver \"%s\" returned %s%n", supplier.describe(), dir);
                File dirPathname = new File(dir);
                if (dirPathname.isDirectory()) {
                    System.out.format("maven home resolver: using pathname at which directory exists: %s%n", dirPathname);
                    return dirPathname;
                }
            }
        }
        throw new IOException("maven home directory not found");
    }

    private interface MavenHomeSupplier {

        String describe();

        @Nullable
        String getMavenHomePathname();

        static MavenHomeSupplier of(String description, Supplier<String> supplier) {
            return new MavenHomeSupplier() {
                @Override
                public String describe() {
                    return description;
                }

                @Override
                public String getMavenHomePathname() {
                    return supplier.get();
                }
            };
        }
    }

    static class MavenOnPathResolver implements MavenHomeSupplier {

        private final Function<String, String> envValueMap;

        public MavenOnPathResolver(Function<String, String> envValueMap) {
            this.envValueMap = envValueMap;
        }

        public static MavenOnPathResolver systemPath() {
            return new MavenOnPathResolver(System::getenv);
        }

        @Override
        public String describe() {
            return getClass().getSimpleName();
        }

        protected Stream<String> streamExecutableNames() {
            return Stream.of("mvn", "mvn.cmd");
        }

        protected boolean isValidExecutable(File executable) {
            return executable.isFile();
        }

        @Nullable
        protected File getMavenHomeFromExecutable(File executable) {
            try {
                executable = executable.getCanonicalFile();
            } catch (IOException e) {
                System.err.format("could not resolve symbolic link reference: %s%n", e.toString());
            }
            File binDir = executable.getParentFile();
            if (binDir != null) {
                return binDir.getParentFile();
            }
            return null;
        }

        protected boolean isValidMavenHome(String absolutePath) {
            return absolutePath.toLowerCase().contains("maven");
        }

        @Nullable
        @Override
        public String getMavenHomePathname() {
            String pathValue = envValueMap.apply("PATH");
            String[] pathDirs = pathValue.split(Pattern.quote(File.pathSeparator));
            return Arrays.stream(pathDirs)
                    .flatMap(dir -> {
                        return streamExecutableNames().map(binname -> new File(dir, binname));
                    }).filter(this::isValidExecutable)
                    .map(this::getMavenHomeFromExecutable)
                    .filter(Objects::nonNull)
                    .map(File::getAbsolutePath)
                    .filter(this::isValidMavenHome)
                    .findFirst().orElse(null);
        }
    }

    private static Iterable<MavenHomeSupplier> getMavenHomePathnameSuppliers() {
        return java.util.List.of(
                MavenHomeSupplier.of("system property", () -> System.getProperty("configdoclet.build.maven.home")),
                MavenHomeSupplier.of("env var M2_HOME", () -> System.getenv("M2_HOME")),
                MavenOnPathResolver.systemPath(),
                MavenHomeSupplier.of("looking in /usr/local/share/maven", () -> "/usr/local/share/maven"),
                MavenHomeSupplier.of("looking in /usr/share/maven", () -> "/usr/share/maven")

        );
    }

}
