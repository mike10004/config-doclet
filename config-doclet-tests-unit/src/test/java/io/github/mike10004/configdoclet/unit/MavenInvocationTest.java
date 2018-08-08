package io.github.mike10004.configdoclet.unit;

import com.google.common.io.Files;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenInvocationTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void invoke() throws Exception {
        Path projectDir = temporaryFolder.newFolder().toPath();
        SampleProject.getDefault().copyTestProject(projectDir);
        InvocationRequest request = new DefaultInvocationRequest();
        request.setInputStream(new ByteArrayInputStream(new byte[0]));
        request.setPomFile(projectDir.resolve("pom.xml").toFile());
        request.setGoals(Collections.singletonList("javadoc:javadoc"));
        Properties properties = new Properties();
//        String docletClasspath = resolveDocletJars().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));
//        properties.setProperty("docletPath", docletClasspath);
        properties.setProperty("java.home", System.getProperty("java.home"));
        request.setProperties(properties);
        System.out.format("passing JAVA_HOME=%s%n", System.getProperty("java.home"));
        request.addShellEnvironment("JAVA_HOME", System.getProperty("java.home"));
        TestConfig tc = Tests.config();
        request.addShellEnvironment("DOCLET_ARTIFACT_GROUP_ID", tc.get("project.groupId"));
        request.addShellEnvironment("DOCLET_ARTIFACT_ARTIFACT_ID", "config-doclet-core");
        request.addShellEnvironment("DOCLET_ARTIFACT_VERSION", tc.get("project.version"));
        DefaultInvoker invoker = new DefaultInvoker();
        invoker.setMavenHome(resolveMavenHome());
        invoker.setWorkingDirectory(temporaryFolder.newFolder());
        InvocationResult result = invoker.execute(request);
        CommandLineException exception = result.getExecutionException();
        if (exception != null) {
            exception.printStackTrace(System.out);
        }
        assertEquals("exit code", 0, result.getExitCode());
        File outputFile = projectDir.resolve("target/site/apidocs/config-help/cfg-doclet-output.txt").toFile();
        assertTrue("exists: " + outputFile, outputFile.isFile());
        String content = Files.asCharSource(outputFile, UTF_8).read();
        System.out.println(content);
        assertFalse("content nonempty", content.trim().isEmpty());
    }

    private File resolveMavenHome() {
        // TODO allow specifying maven home with sysprop
        return new File("/usr/share/maven");
    }

//    private List<File> resolveDocletJars() throws IOException {
//        List<File> files = new ArrayList<>();
//        Resources.asCharSource(getClass().getResource("/dependencies.txt"), UTF_8).forEachLine(line -> {
//            if (line.startsWith("  ")) {
//                String untokenized = Splitter.on(" -- ").split(line).iterator().next();
//                Iterator<String> tokenized = Splitter.on(":").split(untokenized).iterator();
//                String path = null;
//                while (tokenized.hasNext()) {
//                    path = tokenized.next();
//                }
//                checkState(path != null);
//                files.add(new File(path));
//            }
//        });
//        return files;
//    }
}
