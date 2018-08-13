package io.github.mike10004.configdoclet;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import io.github.mike10004.configdoclet.unit.SampleProject;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigDocletTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final Charset STD_CHARSET = UTF_8;

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
        execute(new String[]{"--output-format=" + ConfigDoclet.OUTPUT_FORMAT_PROPERTIES});
    }

    @Test
    public void defaultPath_json() throws Exception  {
        String output = execute(new String[]{"--output-format=" + ConfigDoclet.OUTPUT_FORMAT_JSON});
        List<ConfigSetting> items = Arrays.asList(new Gson().fromJson(output, ConfigSetting[].class));
        assertNotNull("deserialized", items);
        Stream<String> required = Stream.of("app.server.attire", "app.choice.default", "app.undocumentedSetting");
        required.forEach(settingKey -> {
            assertTrue("setting " + settingKey, items.stream().anyMatch(setting -> settingKey.equals(setting.key)));
        });
        List<Pair<ConfigSetting, ConfigSetting>> missing = new ArrayList<>();
        Set<ConfigSetting> expected = loadExpectedSettingsDefault();
        expected.forEach(setting -> {
            if (!items.contains(setting)) {
                @Nullable ConfigSetting closest = items.stream().filter(present -> setting.key.equals(present.key)).findAny().orElse(null);
                missing.add(Pair.of(setting, closest));
            }
        });
        if (!missing.isEmpty()) {
            System.out.format("could not match expected settings %s%n", missing.stream().map(Pair::getLeft).map(s -> s.key).collect(Collectors.toList()));
        }
        assertEquals("expected no missing items", Collections.emptyList(), missing);
        assertEquals("expected settings size", expected.size(), items.size());
    }

    @Test
    public void defaultPath_restricted_appDestination() throws Exception {
        ConfigSetting expected = ConfigSetting.builder("app.destination")
                .description("this description overrides the sentences")
                .defaultValue("stdout")
                .exampleValue("stderr")
                .exampleValue("null")
                .build();
        testDefaultPathForSingleSetting("CFG_DESTINATION", expected);
    }

    @Test
    public void defaultPath_restricted_appMessage() throws Exception {
        ConfigSetting expected = ConfigSetting.builder("app.message")
                .description("Message configuration property name. This second sentence contains some detail about the property.")
                .defaultValue("Hello, world!")
                .exampleValue("Looking good, Billy Ray!")
                .exampleValue("Feeling good, Louis!")
                .build();
        testDefaultPathForSingleSetting("CFG_MESSAGE", expected);
    }

    @SuppressWarnings("UnusedReturnValue")
    private ConfigSetting testDefaultPathForSingleSetting(String settingKey, ConfigSetting expected) throws Exception {
        String[] args = {
                ConfigDoclet.OPT_OUTPUT_FORMAT, ConfigDoclet.OUTPUT_FORMAT_JSON,
                ConfigDoclet.OPT_FIELD_NAME_PATTERN + "=" + settingKey
        };
        String output = execute(args);
        ConfigSetting[] settings = new Gson().fromJson(output, ConfigSetting[].class);
        checkState(settings.length == 1);
        ConfigSetting actual = settings[0];
        if (!expected.equals(actual)) {
            System.err.format("expected:%n%n%s%n%nactual:%n%n%s%n%n", expected.toStringWithExamples(), actual.toStringWithExamples());
        }
        assertEquals("app.destination", expected, settings[0]);
        return settings[0];
    }

    @Test
    public void useFieldNameRegex() throws Exception {
        String regex = "^WACKY_.*$";
        String[] args = {
                ConfigDoclet.OPT_OUTPUT_FORMAT, ConfigDoclet.OUTPUT_FORMAT_JSON,
                ConfigDoclet.OPT_FIELD_NAME_REGEX + "=" + regex
        };
        String output = execute(args);
        ConfigSetting[] settings = new Gson().fromJson(output, ConfigSetting[].class);
        assertEquals("settings.length", 1, settings.length);
        assertEquals("settings[0].key", "wacky.setting.name", settings[0].key);
    }

    @Test
    public void useFieldNamePattern() throws Exception {
        String pattern = "WACKY_*";
        String[] args = {
                ConfigDoclet.OPT_OUTPUT_FORMAT, ConfigDoclet.OUTPUT_FORMAT_JSON,
                ConfigDoclet.OPT_FIELD_NAME_PATTERN + "=" + pattern
        };
        String output = execute(args);
        ConfigSetting[] settings = new Gson().fromJson(output, ConfigSetting[].class);
        assertEquals("settings.length", 1, settings.length);
        assertEquals("settings[0].key", "wacky.setting.name", settings[0].key);
    }

    private String buildClasspath() throws URISyntaxException, IOException {
        CharSource cs = Resources.asCharSource(getClass().getResource("/dependencies.txt"), UTF_8);
        List<MavenRepositoryItem> repoItems;
        try (Reader reader = cs.openStream()) {
            repoItems = new MavenDependencyListParser().parseList(reader);
        }
        String docletPart = new File(Tests.config().get("project.build.outputDirectory")).getAbsolutePath();
        Stream<String> cpComponents = repoItems.stream()
                .map(item -> item.artifactPathname)
                .filter(Objects::nonNull)
                .map(File::getAbsolutePath);
        String cp = Stream.concat(Stream.of(docletPart), cpComponents)
                .collect(Collectors.joining(File.pathSeparator));
        return cp;
    }

    private String execute(String[] moreArgs) throws Exception  {
        return execute(moreArgs, new String[]{"com.example"});
    }

    private String execute(String[] moreArgs, String[] packages) throws Exception  {
        File sourcepath = prepareProject().toPath().resolve("src/main/java").toFile();
        System.out.format("using sourcepath %s%n", sourcepath);
        checkState(sourcepath.isDirectory(), "not a directory: %s", sourcepath);
        String docletClasspath = buildClasspath();
        File outputDir = temporaryFolder.newFolder();
        System.out.format("docletClasspath = %s%n", docletClasspath);
        String[] commonArgs = {"-doclet", ConfigDoclet.class.getName(),
                "-docletpath", docletClasspath,
                "-charset", "UTF-8",
                "-sourcepath", sourcepath.getAbsolutePath(),
                "-d", outputDir.getAbsolutePath(),
        };
        List<String> allArgsList = new ArrayList<>();
        allArgsList.addAll(Arrays.asList(commonArgs));
        allArgsList.addAll(Arrays.asList(moreArgs));
        allArgsList.addAll(Arrays.asList(packages));
        String[] allArgs = allArgsList.toArray(new String[0]);
        System.out.format("arguments: %s%n", Arrays.toString(allArgs));
        ToolResult result = invokeJavadocStart(allArgs);
        assertEquals("exit code", 0, result.exitCode);
        Collection<File> filesInOutputDir = org.apache.commons.io.FileUtils.listFiles(outputDir, null, true);
        assertEquals("one file in output dir", 1, filesInOutputDir.size());
        File outputFile = filesInOutputDir.iterator().next();
        String output = com.google.common.io.Files.asCharSource(outputFile, UTF_8).read();
        System.out.println(output);
        return output;
    }

    private Set<ConfigSetting> loadExpectedSettingsDefault() throws IOException {
        String json = Resources.toString(getClass().getResource("/expected-settings-default.json"), UTF_8);
        ConfigSetting[] settings = new Gson().fromJson(json, ConfigSetting[].class);
        return Set.of(settings);
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
        Set<String> actionableTagsByDefault = doclet.buildActionableTagSet();
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
        org.apache.commons.io.FilenameUtils.class.getName();
    }


    @Test
    public void constructRegexNamePredicate() {
        String regex = "^APPCONFIGPROP_.*$";
        Predicate<? super CharSequence> predicate = ConfigDoclet.constructRegexNamePredicate(regex);
        ImmutableMap.<String, Boolean>builder()
                .put("APPCONFIGPROP_BROWSER_EXECUTABLE_PATH_CHROME", true)
                .put("SYSPROP_HELLO", false)
                .put("CFG_BOOYAH", false)
                .build()
                .forEach((name, expected) -> {
                    boolean actual = predicate.test(name);
                    assertEquals("evaluation on " + name, expected, actual);
                });
    }

    @Test
    public void constructPatternNamePredicate() {
        String patternsToken = "APPCONFIGPROP_*,CFG_* FOO_?AR";
        Predicate<? super CharSequence> predicate = ConfigDoclet.constructPatternNamePredicate(patternsToken, ConfigDoclet.DEFAULT_PATTERN_CASE_SENSITIVITY);
        ImmutableMap.<String, Boolean>builder()
                .put("APPCONFIGPROP_BROWSER_EXECUTABLE_PATH_CHROME", true)
                .put("SYSPROP_HELLO", false)
                .put("CFG_BOOYAH", true)
                .put("FOO_BAR", true)
                .put("FOO_CHAR", false)
                .build()
                .forEach((name, expected) -> {
                    boolean actual = predicate.test(name);
                    assertEquals("evaluation on " + name, expected, actual);
                });
    }
}