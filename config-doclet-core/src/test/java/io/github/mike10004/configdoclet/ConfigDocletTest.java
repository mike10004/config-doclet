package io.github.mike10004.configdoclet;

import com.google.gson.Gson;
import io.github.mike10004.configdoclet.unit.SampleProject;
import jdk.javadoc.doclet.Reporter;
import org.apache.commons.io.output.TeeOutputStream;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfigDocletTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final Charset STD_CHARSET = StandardCharsets.UTF_8;

    // http://in.relation.to/2017/12/06/06-calling-jdk-tools-programmatically-on-java-9/
    private ToolResult invokeJavadocStart(String[] commandLineArgs) throws Exception {
        java.util.spi.ToolProvider javadoc = java.util.spi.ToolProvider.findFirst("javadoc").orElseThrow(() -> new IllegalStateException("no javadoc tool available"));
        ByteBucket stdout = new ByteBucket(1024), stderr = new ByteBucket(1024);
        TeeOutputStream duplexStderr = new TeeOutputStream(stderr.stream(), System.err);
        TeeOutputStream duplexStdout = new TeeOutputStream(stdout.stream(), System.out);
        int exitCode = javadoc.run(new PrintStream(duplexStdout, true, STD_CHARSET.name()), new PrintStream(duplexStderr, true, STD_CHARSET.name()), commandLineArgs);
        return new ToolResult(exitCode, stdout, stderr);
    }

    private static class ToolResult {

        public final int exitCode;
        public final ByteBucket stdout, stderr;

        private ToolResult(int exitCode, ByteBucket stdout, ByteBucket stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }

    private static class ByteBucket {

        private final ByteArrayOutputStream collector;

        public ByteBucket(int initCapacity) {
            collector = new ByteArrayOutputStream(initCapacity);
        }

        public OutputStream stream() {
            return collector;
        }

        public byte[] dump() {
            return collector.toByteArray();
        }

        public String dump(Charset charset) {
            return new String(dump(), charset);
        }
    }

    private File prepareProject() throws URISyntaxException, IOException {
        File dir = temporaryFolder.newFolder();
        SampleProject.getDefault().copyTestProject(dir.toPath());
        return dir;
    }

    @Test
    public void defaultPath_properties() throws Exception  {
        defaultPath(ConfigDoclet.OUTPUT_PROPERTIES);
    }

    @Test
    public void defaultPath_json() throws Exception  {
        String output = defaultPath(ConfigDoclet.OUTPUT_JSON);
        ConfigSetting[] items = new Gson().fromJson(output, ConfigSetting[].class);
        assertNotNull("deserialized", items);
        ConfigSetting[] expected = new Gson().fromJson(EXPECTED_DEFAULT_MODEL_ITEMS_JSON, ConfigSetting[].class);
        assertArrayEquals("items", expected, items);
    }

    private String buildClasspath() throws URISyntaxException {
        return Stream.of(new File(Tests.config().get("project.build.outputDirectory")),
                new File(getClass().getResource("/gson-2.8.5.jar").toURI()))
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));
    }

    private String defaultPath(String outputFormat) throws Exception  {
        File sourcepath = prepareProject().toPath().resolve("src/main/java").toFile();
        System.out.format("using sourcepath %s%n", sourcepath);
        checkState(sourcepath.isDirectory(), "not a directory: %s", sourcepath);
        String docletClasspath = buildClasspath();
        File outputDir = temporaryFolder.newFolder();
        System.out.format("docletClasspath = %s%n", docletClasspath);
        int exitCode = invokeJavadocStart(new String[]{"-doclet", ConfigDoclet.class.getName(),
                "-docletpath", docletClasspath,
                "-charset", "UTF-8",
                "-sourcepath", sourcepath.getAbsolutePath(),
                "-d", outputDir.getAbsolutePath(),
                "--output-format=" + outputFormat,
                "com.example",
        }).exitCode;
        assertEquals("exit code", 0, exitCode);
        Collection<File> filesInOutputDir = org.apache.commons.io.FileUtils.listFiles(outputDir, null, true);
        assertEquals("one file in output dir", 1, filesInOutputDir.size());
        File outputFile = filesInOutputDir.iterator().next();
        String output = com.google.common.io.Files.asCharSource(outputFile, StandardCharsets.UTF_8).read();
        System.out.println(output);
        return output;
    }

    private static final String EXPECTED_DEFAULT_MODEL_ITEMS_JSON = "[\n" +
            "  {\n" +
            "    \"key\": \"app.message\",\n" +
            "    \"description\": \"Message configuration property name. This second sentence contains some detail about the property.\",\n" +
            "    \"defaultValue\": \"Hello, world!\",\n" +
            "    \"exampleValues\": [\n" +
            "      {\n" +
            "        \"value\": \" Looking good, Billy Ray!\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \" Feeling good, Louis!\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"key\": \"app.destination\",\n" +
            "    \"description\": \" this description overrides the sentences\",\n" +
            "    \"defaultValue\": \"stdout\",\n" +
            "    \"exampleValues\": [\n" +
            "      {\n" +
            "        \"value\": \" stderr\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \" null\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"key\": \"cfg.numRepetitions\",\n" +
            "    \"description\": \"Setting specifying number of repetitions. A value of N means that the message is printed N times.\",\n" +
            "    \"defaultValue\": \"1\",\n" +
            "    \"exampleValues\": []\n" +
            "  }\n" +
            "]";

    @Test
    public void numActionableTags() {
        List<Field> cfgConstants = Stream.of(ConfigDoclet.class.getDeclaredFields())
                .filter(field -> {
                    return Modifier.isStatic(field.getModifiers())
                            && Modifier.isFinal(field.getModifiers())
                            && field.getName().startsWith("TAG_");
                }).collect(Collectors.toList());
        ConfigDoclet doclet = new ConfigDoclet();
        doclet.init(Locale.getDefault(), EasyMock.createMock(Reporter.class));
        Set<String> actionableTagsByDefault = doclet.actionableTags;
        List<String> values = cfgConstants.stream().map(f -> {
            boolean acc = f.canAccess(null);
            try {
                f.setAccessible(true);
                return (String) f.get(null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                f.setAccessible(acc);
            }
        }).collect(Collectors.toList());
        assertEquals("tags", new HashSet<>(values), new HashSet<>(actionableTagsByDefault));
    }

//    @Test
//    public void constructSignature() throws Exception {
//        TypeElement classElement;
//    }
}