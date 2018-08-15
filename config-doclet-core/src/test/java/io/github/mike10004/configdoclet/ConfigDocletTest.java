package io.github.mike10004.configdoclet;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import io.github.mike10004.configdoclet.tests.SampleProject;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    private File prepareProject() throws URISyntaxException, IOException {
        File dir = temporaryFolder.newFolder();
        SampleProject.getDefault().copyTestProject(dir.toPath());
        return dir;
    }

    @Test
    public void defaultPath_properties() throws Exception  {
        String headerContent = "This is the header" + System.lineSeparator();
        File headerFile = temporaryFolder.newFile();
        java.nio.file.Files.write(headerFile.toPath(), headerContent.getBytes());
//        String bottom = StringEscapeUtils.escapeHtml4("# This is the bottom" + System.lineSeparator());
        String bottom = "This is the bottom" + System.lineSeparator();
        String output = execute(new String[]{
                "--output-format=" + ConfigDoclet.OUTPUT_FORMAT_PROPERTIES,
                ConfigDoclet.OPT_HEADER, headerFile.toURI().toString(),
                ConfigDoclet.OPT_FOOTER, bottom,
        });
        output = output.trim();
        assertTrue("starts with header", output.startsWith("#" + headerContent));
        String expectedTail = "#" + bottom + "#";
        if (!output.endsWith(expectedTail)) {
            String actualTail = output.substring(output.length() - expectedTail.length() * 2);
            System.out.format("actual tail: \"%s\"%n", StringEscapeUtils.escapeJava(actualTail));
        }
        assertTrue(String.format("expect at bottom: \"%s\"", StringEscapeUtils.escapeJava(expectedTail)), output.endsWith(expectedTail));
    }

    @Test
    public void defaultPath_json() throws Exception  {
        String output = execute(new String[]{"--output-format=" + ConfigDoclet.OUTPUT_FORMAT_JSON});
        List<ConfigSetting> items = Arrays.asList(new Gson().fromJson(output, ConfigSetting[].class));
        assertNotNull("deserialized", items);
        Stream<String> required = Stream.of(
                "app.server.attire",
                "app.choice.default",
                "app.undocumentedSetting",
                "app.nestedClass.private.bar",
                "app.nestedClass.public.foo",
                "app.runtimeKey",
                "app.emptyDefault",
                "app.something.deprecated.butImportant"
        );
        List<String> absentRequired = new ArrayList<>();
        required.forEach(settingKey -> {
            if (items.stream().noneMatch(setting -> settingKey.equals(setting.key))) {
                absentRequired.add(settingKey);
            }
        });
        assertEquals("expected but absent", Collections.emptyList(), absentRequired);
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
        if (expected.size() != items.size()) {
            System.out.format("expected: %s%n", expected.stream().map(s -> s.key).sorted().collect(Collectors.toList()));
            System.out.format("  actual: %s%n", items.stream().map(s -> s.key).sorted().collect(Collectors.toList()));
        }
        assertEquals("expected settings size", expected.size(), items.size());
        if (isSortedBySortKey(items) == isSortedByConfigKey(items)) {
            List<ConfigSetting> sortedBySortKey = sortKeyOrdering().immutableSortedCopy(items);
            List<ConfigSetting> sortedByConfigKey = configKeyOrdering().immutableSortedCopy(items);
            for (int i = 0; i < sortedBySortKey.size(); i++) {
                ConfigSetting s1 = sortedByConfigKey.get(i);
                ConfigSetting s2 = sortedBySortKey.get(i);
                System.out.format("[%2d] %-38s %-38s %-38s %-38s%n", i, s1.key, s2.key, s1.getSortKey(), s2.getSortKey());
            }
        }
        assertTrue("expect sorting by getSortKey()", isSortedBySortKey(items));
        assertFalse("expected item to be sorted by .getSortKey() instead of .key", isSortedByConfigKey(items));
    }

    private static Ordering<ConfigSetting> sortKeyOrdering() {
        Comparator<ConfigSetting> comp = Comparator.comparing(ConfigSetting::getSortKey);
        return Ordering.from(comp);
    }

    private static Ordering<ConfigSetting> configKeyOrdering() {
        Comparator<ConfigSetting> comp = Comparator.comparing(setting -> setting.key);
        return Ordering.from(comp);
    }

    private static boolean isSortedByConfigKey(List<ConfigSetting> settings) {
        return configKeyOrdering().isOrdered(settings);
    }

    private static boolean isSortedBySortKey(List<ConfigSetting> settings) {
        return sortKeyOrdering().isOrdered(settings);
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

    @Test
    public void defaultPath_restricted_simpleBoolean() throws Exception {
        ConfigSetting expected = ConfigSetting.builder("app.simpleBoolean")
                .description("Setting that specifies a simple boolean as its default value.")
                .defaultValue("false")
                .build();
        ConfigSetting actual = testDefaultPathForSingleSetting("CFG_SIMPLE_BOOLEAN_DEFAULT", expected);
        PrintWriter pw = new PrintWriter(System.out);
        new PropertiesOutputFormatter().format(actual, pw);
        pw.flush();
        System.out.println();
    }

    @Test
    public void defaultPath_restricted_emptyDefault() throws Exception {
        ConfigSetting expected = ConfigSetting.builder("app.emptyDefault")
                .defaultValue("")
                .exampleValue("foo")
                .exampleValue("bar")
                .build();
        ConfigSetting actual = testDefaultPathForSingleSetting("CFG_EMPTY_DEFAULT", expected, true);
        assertEquals("key", expected.key, actual.key);
        assertEquals("default", expected.defaultValue, actual.defaultValue);
        assertEquals("num exmaples", expected.exampleValues.size(), actual.exampleValues.size());
        assertEquals("e1", expected.exampleValues.get(0).value, actual.exampleValues.get(0).value);
        assertEquals("e2", expected.exampleValues.get(1).value, actual.exampleValues.get(1).value);
    }

    @Test
    public void defaultPath_inlineCode() throws Exception {
        ConfigSetting expected = ConfigSetting.builder("app.important.pathname")
                .description("Setting specifying a pathname. This demonstrates rendering of  special text  in\n inline code spans.")
                .exampleValue("/home/elizabeth/Documents/cool.txt")
                .build();
        testDefaultPathForSingleSetting("CFG_USING_CODE", expected);
    }

    @SuppressWarnings("UnusedReturnValue")
    private ConfigSetting testDefaultPathForSingleSetting(String fieldNamePattern, ConfigSetting expected) throws Exception {
        return testDefaultPathForSingleSetting(fieldNamePattern, expected, false);
    }

    private ConfigSetting testDefaultPathForSingleSetting(String fieldNamePattern, ConfigSetting expected, boolean ignoreEqualityCheck) throws Exception {
        String[] args = {
                ConfigDoclet.OPT_OUTPUT_FORMAT, ConfigDoclet.OUTPUT_FORMAT_JSON,
                ConfigDoclet.OPT_FIELD_NAME_PATTERN + "=" + fieldNamePattern
        };
        String output = execute(args);
        ConfigSetting[] settings = new Gson().fromJson(output, ConfigSetting[].class);
        checkState(settings.length == 1, "%s settings parsed (expected exactly 1)", settings.length);
        ConfigSetting actual = settings[0];
        if (!ignoreEqualityCheck) {
            if (!expected.equals(actual)) {
                System.err.format("expected:%n%n%s%n%nactual:%n%n%s%n%n", expected.toStringWithExamples(), actual.toStringWithExamples());
            }
            assertEquals("expected setting", expected, settings[0]);
        }
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

    private static final String SYSPROP_DUMP_PARSED_DEPENDENCIES = "configdoclet.build.tests.dumpClasspath";

    private static boolean isDumpParsedDependencies() {
        return Boolean.parseBoolean(System.getProperty(SYSPROP_DUMP_PARSED_DEPENDENCIES, "false"));
    }

    private static final Supplier<String> classpathSupplier = Suppliers.memoize(() -> {
        URL depsResource = ConfigDocletTest.class.getResource("/dependencies.txt");
        CharSource cs = Resources.asCharSource(depsResource, UTF_8);
        List<MavenRepositoryItem> repoItems;
        try (Reader reader = cs.openStream()) {
            repoItems = new MavenDependencyListParser().parseList(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (isDumpParsedDependencies()) {
            try {
                System.out.format("%s:%n%n", depsResource);
                cs.copyTo(System.out);
                System.out.format("%n%n%n");
                repoItems.forEach(item -> System.out.format("%s%n", item));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String docletPart = new File(Tests.config().get("project.build.outputDirectory")).getAbsolutePath();
        Stream<String> cpComponents = repoItems.stream()
                .map(item -> item.artifactPathname)
                .filter(Objects::nonNull)
                .map(File::getAbsolutePath);
        String cp = Stream.concat(Stream.of(docletPart), cpComponents)
                .collect(Collectors.joining(File.pathSeparator));
        if (isDumpParsedDependencies()) {
            System.out.format("classpath: %s%n", cp);
        }
        return cp;
    });

    private String execute(String[] moreArgs) throws Exception  {
        return execute(moreArgs, new String[]{"com.example"});
    }

    private static final boolean PRINT_EXTRA_DIAGNOSTICS = true;

    private String execute(String[] moreArgs, String[] packages) throws Exception  {
        File sourcepath = prepareProject().toPath().resolve("src/main/java").toFile();
        System.out.format("using sourcepath %s%n", sourcepath);
        checkState(sourcepath.isDirectory(), "not a directory: %s", sourcepath);
        String docletClasspath = classpathSupplier.get();
        File outputDir = temporaryFolder.newFolder();
        System.out.format("docletClasspath = %s%n", docletClasspath);
        System.setProperty(ConfigDoclet.SYSPROP_PRINT_EXTRA_DIAGNOSTICS, String.valueOf(PRINT_EXTRA_DIAGNOSTICS));
        String[] commonArgs = {
                "-private",
                "-doclet", ConfigDoclet.class.getName(),
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
        if (result.exitCode != 0) {
            System.out.format("exit code %s%n", result.exitCode);
            System.out.format("==== stdout ====%n%s%n==== end stdout ====%n%n", result.stdout.dump(Charset.defaultCharset()));
            System.out.format("==== stderr ====%n%s%n==== end stderr ====%n%n", result.stderr.dump(Charset.defaultCharset()));
        }
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

    @Test
    public void hasHeaderOption() {
        boolean actual = new ConfigDoclet().getSupportedOptions().stream().anyMatch(option -> {
            return option.getNames().contains(ConfigDoclet.OPT_HEADER);
        });
        assertTrue("has option: " + actual, actual);
    }

    @Test
    public void hasBottomOption() {
        boolean actual = new ConfigDoclet().getSupportedOptions().stream().anyMatch(option -> {
            return option.getNames().contains(ConfigDoclet.OPT_FOOTER);
        });
        assertTrue("has option: " + actual, actual);
    }
}