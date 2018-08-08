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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private ToolResult invokeJavadocStart0(String[] commandLineArgs) throws Exception {
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

    private int invokeJavadocStart(String[] commandLineArgs) throws Exception {
        Class<?> contextClass = Class.forName("com.sun.tools.javac.util.Context");
        Object context = contextClass.getConstructor().newInstance();
        Reflections.invoke(Class.forName("jdk.javadoc.internal.tool.Messager"), null, "preRegister", new Class[]{contextClass, String.class}, new Object[]{context, "SomeProgram"});
        System.out.format("registered log in %s%n", context);
        Class<?> startClass = Class.forName("jdk.javadoc.internal.tool.Start");
        Constructor<?>[] ctors = startClass.getConstructors();
        Constructor<?> ctor = Stream.of(ctors).filter(c -> c.getParameters().length == 1 && contextClass.equals(c.getParameters()[0].getType()))
                .findFirst().orElseThrow(() -> new IllegalStateException("constructor accepting Context arg not found"));
        ctor.setAccessible(true);
        Object startInstance = ctor.newInstance(context);
        Method beginMethod = startClass.getDeclaredMethod("begin", String[].class);
        beginMethod.setAccessible(true);
        Object[] invokeMethodArgs = {
                commandLineArgs
        };
        Object result = beginMethod.invoke(startInstance, invokeMethodArgs);
        System.out.println(result);
        int exitCode = parseExitCode(result);
        return exitCode;
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

    private String defaultPath(String outputFormat) throws Exception  {
        File sourcepath = prepareProject().toPath().resolve("src/main/java").toFile();
        File docletClasspath = new File(Tests.config().get("project.build.outputDirectory"));
        File outputDir = temporaryFolder.newFolder();
        System.out.format("docletClasspath = %s%n", docletClasspath);
        int exitCode = invokeJavadocStart0(new String[]{"-doclet", ConfigDoclet.class.getName(),
                "-docletpath", docletClasspath.getAbsolutePath(),
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

    private static int parseExitCode(Object result) {
        Matcher m = Pattern.compile("\\w+\\((\\d+)\\)").matcher(result.toString());
        checkState(m.find());
        return Integer.parseInt(m.group(1));
    }

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