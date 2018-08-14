package io.github.mike10004.configdoclet;

import com.google.gson.Gson;
import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.annotation.Nullable;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Doclet that produces documentation on configuration settings your code uses.
 */
public class ConfigDoclet implements Doclet {

    private final Level defaultLevel = Level.FINER;

    private static final Logger log = Logger.getLogger(ConfigDoclet.class.getName());

    private static final String DEFAULT_OUTPUT_FILENAME = "config-doclet-output.txt";

    static final String SYSPROP_PRINT_EXTRA_DIAGNOSTICS = "configdoclet.diagnostics.extras.print";
    static final String OPT_OUTPUT_DIRECTORY = "-d";
    static final String OPT_OUTPUT_FILENAME = "--output-filename";
    static final String OPT_FIELD_NAME_PATTERN = "--field-pattern";
    static final String OPT_FIELD_NAME_REGEX = "--field-regex";
    static final String OPT_OUTPUT_FORMAT = "-outputformat";
    static final String OPT_APPEND_SETTINGS = "--append-settings";
    static final String OUTPUT_FORMAT_PROPERTIES = "properties";
    static final String OUTPUT_FORMAT_JSON = "json";
    static final IOCase DEFAULT_PATTERN_CASE_SENSITIVITY = IOCase.SENSITIVE;
    static final String TAG_CFG_DESCRIPTION = "cfg.description";
    static final String TAG_CFG_EXAMPLE = "cfg.example";
    static final String TAG_CFG_DEFAULT_VALUE = "cfg.default";
    static final String TAG_CFG_KEY = "cfg.key";
    static final String TAG_CFG_INCLUDE = "cfg.include";
    static final String TAG_CFG_SORT_KEY = "cfg.sortKey";

    private Reporter reporter;
    private final Optionage optionage;

    /**
     * Constructs an instance of the class.
     */
    public ConfigDoclet() {
        this(Optionage.compose(CliOptionage.standard(), PropertyOptionage.system()));
    }

    ConfigDoclet(Optionage optionage) {
        this.optionage = requireNonNull(optionage);
    }

    static Comparator<ConfigSetting> settingOrdering() {
        return Comparator.comparing(ConfigSetting::getSortKey)
                .thenComparing(s -> s.key);
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    Set<String> buildActionableTagSet() {
        return Set.of(TAG_CFG_DEFAULT_VALUE, TAG_CFG_DESCRIPTION, TAG_CFG_EXAMPLE, TAG_CFG_KEY, TAG_CFG_SORT_KEY, TAG_CFG_INCLUDE);
    }

    private static boolean isPrintExtraDiagnostics() {
        return Boolean.parseBoolean(System.getProperty(SYSPROP_PRINT_EXTRA_DIAGNOSTICS));
    }

    private void extraDiagnostic(Supplier<String> supplier) {
        extraDiagnostic(Diagnostic.Kind.NOTE, supplier);
    }

    @SuppressWarnings("SameParameterValue")
    private void extraDiagnostic(Diagnostic.Kind kind, Supplier<String> supplier) {
        if (isPrintExtraDiagnostics()) {
            reporter.print(kind, supplier.get());
        }
    }

    static List<String> tokenizePatterns(String untokenizedPatterns) {
        requireNonNull(untokenizedPatterns);
        List<String> patterns = new ArrayList<>();
        String[] patternTokens = untokenizedPatterns.split("[\\s,]+");
        for (String token : patternTokens) {
            token = token.trim();
            if (!token.isEmpty()) {
                patterns.add(token);
            }
        }
        return Collections.unmodifiableList(patterns);
    }

    @SuppressWarnings("SameParameterValue")
    static Predicate<? super CharSequence> constructPatternNamePredicate(String untokenizedPatterns, IOCase sensitivity) {
        List<String> patterns = tokenizePatterns(untokenizedPatterns);
        return new Predicate<>() {
            @Override
            public boolean test(CharSequence name) {
                String nameStr = name.toString();
                for (String wildcardPattern : patterns) {
                    boolean match = FilenameUtils.wildcardMatch(nameStr, wildcardPattern, sensitivity);
                    if (match) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return "PatternNamePredicate" + patterns.toString();
            }
        };
    }

    static Predicate<? super CharSequence> constructRegexNamePredicate(String fieldNameRegex) {
        requireNonNull(fieldNameRegex);
        return new Predicate<>() {

            @Override
            public boolean test(CharSequence name) {
                String nameStr = name.toString();
                return nameStr.matches(fieldNameRegex);
            }

            @Override
            public String toString() {
                return "RegexNamePredicate{" + fieldNameRegex + "}";
            }
        };
    }

    Predicate<? super CharSequence> constructElementNamePredicate() {
        @Nullable String fieldNamePattern = optionage.getOptionString(OPT_FIELD_NAME_PATTERN, null);
        @Nullable String fieldNameRegex = optionage.getOptionString(OPT_FIELD_NAME_REGEX, null);
        if (fieldNamePattern != null && fieldNameRegex != null) {
            reporter.print(Diagnostic.Kind.WARNING, "name predicate regex and pattern are both specified; only one will be used, and you don't know which");
        }
        if (fieldNamePattern != null) {
            // TODO support setting that controls pattern case sensitivity
            return constructPatternNamePredicate(fieldNamePattern, DEFAULT_PATTERN_CASE_SENSITIVITY);
        }
        if (fieldNameRegex != null) {
            return constructRegexNamePredicate(fieldNameRegex);
        }
        return startsWithAny("PROP_", "PROPERTY_", "CFG_", "CONFIG_");
    }

    @SuppressWarnings("SameParameterValue")
    static Predicate<? super CharSequence> startsWithAny(String prefix1, String...others) {
        Set<String> set = Stream.concat(Stream.of(prefix1), Stream.of(others)).collect(Collectors.toSet());
        return new Predicate<>() {
            @Override
            public boolean test(CharSequence name) {
                String nameStr = name.toString();
                return set.stream().anyMatch(nameStr::startsWith);
            }

            @Override
            public String toString() {
                return "StartsWithAnyOf" + set.toString();
            }
        };
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_6;
    }

    private boolean isActionableEnclosedElement(Element element, Predicate<? super CharSequence> elementNamePredicate, Function<? super Element, DocCommentTree> lazyDocCommentTree) {
        Predicate<? super Element> deprecationPredicate = createDeprecationPredicate(lazyDocCommentTree);
        return element.getKind() == ElementKind.FIELD
                && element.getModifiers().contains(Modifier.STATIC)
                && element.getModifiers().contains(Modifier.FINAL)
                && element instanceof VariableElement
                && deprecationPredicate.test(element)
                && elementNamePredicate.test(element.getSimpleName());
    }

    /**
     * Returns a predicate that evaluates to true on an element if it is to be considered actionable.
     * This predicate examines the element's deprecation annotation and if it is deprecated, the element
     * is only included if there is an {@link #TAG_CFG_INCLUDE} tag.
     */
    private Predicate<? super Element> createDeprecationPredicate(Function<? super Element, DocCommentTree> lazyDocCommentTree) {
        // TODO support option that specifies that deprecated elements should be included
        return element -> {
            Deprecated deprecated = element.getAnnotation(Deprecated.class);
            if (deprecated != null) {
                DocCommentTree tree = lazyDocCommentTree.apply(element);
                if (tree != null) {
                    boolean explicitInclude = tree.getBlockTags().stream().anyMatch(t -> isCfgTag(t, TAG_CFG_INCLUDE));
                    return explicitInclude;
                }
            }
            return true; // not deprecated => include
        };
    }

    private boolean isActionableEnclosingElement(Element element) {
        ElementKind kind = element.getKind();
        switch (kind) {
            case CLASS:
            case ENUM:
            case INTERFACE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        List<ConfigSetting> items = Collections.synchronizedList(new ArrayList<>());
        Set<? extends Element> includedElements = environment.getIncludedElements();
        maybeDumpAll("included elements", includedElements);
        List<VariableElement> variableElements = includedElements.stream()
                .filter(this::isActionableEnclosingElement)
                .filter(TypeElement.class::isInstance)
                .map(e -> (TypeElement) e)
                .flatMap(classElement -> classElement.getEnclosedElements().stream())
                .filter(VariableElement.class::isInstance)
                .map(VariableElement.class::cast)
                .collect(Collectors.toList());
        LinkResolver linkResolver = new CollectionLinkResolver(variableElements);
        Set<String> actionableTags = buildActionableTagSet();
        Predicate<? super CharSequence> namePredicate = constructElementNamePredicate();
        maybeDumpAll("variable elements", variableElements);
        Function<? super Element, DocCommentTree> commentTreeProvider = element -> {
            return environment.getDocTrees().getDocCommentTree(element);
        };
        List<VariableElement> relevantFields = variableElements.stream()
                .filter(element -> isActionableEnclosedElement(element, namePredicate, commentTreeProvider))
                .collect(Collectors.toList());
        reporter.print(Diagnostic.Kind.NOTE, String.format("%d of %d variable elements are relevant (used name predicate %s)", relevantFields.size(), variableElements.size(), namePredicate));
        maybeDumpAll("relevant and actionable elements", relevantFields);
        relevantFields.forEach(enclosed -> {
                    log.log(defaultLevel, () -> String.format("enclosed: kind=%s; name=%s", enclosed.getKind(), enclosed.getSimpleName()));
                    DocCommentTree tree = commentTreeProvider.apply(enclosed);
                    String configKey = extractConfigKey(enclosed, tree);
                    if (configKey != null) {
                        ConfigSetting.Builder b = prepareBuilder(enclosed, configKey);
                        if (tree != null) {
                            LinkValueRenderer linkValueRenderer = new LinkValueRenderer(enclosed, linkResolver, LinkValueRenderer.RenderMode.VALUE_ONLY);
                            CommentRenderer textRenderer = new TextCommentRenderer(new LinkValueRenderer(enclosed, linkResolver, LinkValueRenderer.RenderMode.PARENTHESIZED_VALUE));
                            String description = textRenderer.render(tree.getFullBody());
                            b.description(description);
                            CollectingScanner visitor = new CollectingScanner(actionableTags, enclosed, b, textRenderer, linkValueRenderer);
                            //noinspection RedundantCast
                            visitor.scan(tree, (Void) null);
                        } else {
                            reporter.print(Diagnostic.Kind.NOTE, String.format("element has no comment: %s", enclosed.getSimpleName()));
                        }
                        ConfigSetting item = b.build();
                        items.add(item);
                    } else {
                        reporter.print(Diagnostic.Kind.NOTE, String.format("element does not have constant value or %s defined in comment: %s", TAG_CFG_KEY, enclosed.getSimpleName()));
                    }
                });
        List<ConfigSetting> others = appendOthers(optionage.getOptionString(OPT_APPEND_SETTINGS, null));
        items.addAll(others);
        boolean retval = produceOutput(items);
        return retval;
    }

    private static boolean isCfgTag(DocTree tree, String tagName) {
        return tree instanceof BlockTagTree
                && tagName.equalsIgnoreCase(((BlockTagTree)tree).getTagName());
    }

    @Nullable
    private String extractConfigKey(VariableElement element, DocCommentTree docCommentTree) {
        Object constValue = element.getConstantValue();
        if (constValue == null && docCommentTree != null) {
            constValue = docCommentTree.getBlockTags().stream()
                    .filter(tree -> ConfigDoclet.isCfgTag(tree, TAG_CFG_KEY))
                    .filter(UnknownBlockTagTree.class::isInstance)
                    .map(tree -> CommentRenderer.concatenateText(((UnknownBlockTagTree)tree).getContent()))
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .findFirst().orElse(null);
        }
        return constValue == null ? null : constValue.toString();
    }

    private Charset getAppendOthersCharset() {
        // TODO support option to specify this
        return StandardCharsets.UTF_8;
    }

    protected List<ConfigSetting> appendOthers(@Nullable String jsonPathnames) {
        if (jsonPathnames == null) {
            return Collections.emptyList();
        }
        String[] pathnames = jsonPathnames.split(File.pathSeparator);
        List<ConfigSetting> others = new ArrayList<>();
        for (String pathname : pathnames) {
            if (!pathname.isEmpty()) {
                File jsonFile = new File(pathname);
                ConfigSetting[] some;
                try (Reader reader = new InputStreamReader(new FileInputStream(jsonFile), getAppendOthersCharset())) {
                    some = new Gson().fromJson(reader, ConfigSetting[].class);
                } catch (IOException e) {
                    // TODO support setting to suppress this exception
                    throw new RuntimeException(e);
                }
                if (some != null) {
                    others.addAll(Arrays.asList(some));
                }
            }
        }
        return others;
    }

    private void maybeDumpAll(String tag, Collection<? extends Element> variableElements) {
        extraDiagnostic(Diagnostic.Kind.NOTE, () -> {
            String elementsDebug = variableElements.stream()
                    .map(el -> new ToStringHelper(el)
                            .add("kind", el.getKind())
                            .add("modifiers", el.getModifiers())
                            .add("name", el.getSimpleName().toString())
                            .toString())
                    .collect(Collectors.joining(System.lineSeparator()));
            return String.format("%s:%n%s%n", tag, elementsDebug);
        });
    }

    private ConfigSetting.Builder prepareBuilder(@SuppressWarnings("unused") VariableElement element, Object constValue) {
        ConfigSetting.Builder b = ConfigSetting.builder(constValue.toString());
        return b;
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return optionage.getSupportedOptions();
    }

    protected Charset getOutputCharset() {
        // TODO get charset from options
        return StandardCharsets.UTF_8;
    }

    protected OutputFormatter getOutputFormatter() {
        String format = optionage.getOptionString(OPT_OUTPUT_FORMAT, OUTPUT_FORMAT_PROPERTIES);
        switch (format) {
            case OUTPUT_FORMAT_PROPERTIES:
                return new PropertiesOutputFormatter();
            case OUTPUT_FORMAT_JSON:
                return new GsonOutputFormatter();
            default:
                throw new IllegalArgumentException("invalid output format");

        }
    }

    private void maybeWarnAboutDuplicates(List<ConfigSetting> settings) {
        List<String> allKeys =  settings.stream().map(s -> s.key).collect(Collectors.toList());
        Set<String> uniqueKeys = new HashSet<>(allKeys);
        int numDupes = settings.size() - uniqueKeys.size();
        if (numDupes > 0) {
            reporter.print(Diagnostic.Kind.WARNING, String.format("%s duplicate key(s) to be documented", numDupes));
            Set<String> dupes = allKeys.stream().filter(key -> allKeys.stream().filter(key::equals).count() > 1).collect(Collectors.toSet());
            extraDiagnostic(Diagnostic.Kind.WARNING, () -> String.format("duplicate keys: %s", dupes.stream().collect(Collectors.joining(System.lineSeparator()))));
        }
    }

    protected boolean produceOutput(List<ConfigSetting> items) {
        reporter.print(Diagnostic.Kind.NOTE, String.format("writing help output on %d settings", items.size()));
        maybeWarnAboutDuplicates(items);
        items = items.stream().sorted(settingOrdering()).collect(Collectors.toList());
        OutputFormatter formatter = getOutputFormatter();
        File outputFile = resolveOutputPath().toFile();
        if (!outputFile.getParentFile().isDirectory()) {
            //noinspection ResultOfMethodCallIgnored // will fail on open if dir could not be created
            outputFile.getParentFile().mkdirs();
        }
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), getOutputCharset()))) {
            formatter.format(items, out);
        } catch (IOException e) {
            log.log(Level.SEVERE, "failed to write output file", e);
            reporter.print(Diagnostic.Kind.ERROR, "failed to write to output file " + outputFile + " due to IOException " + e.getMessage());
            return false;
        }
        return true;
    }

    private class CollectingScanner extends ActionableTagScanner<Void, Void> {

        @SuppressWarnings("unused")
        private final VariableElement element;
        private final ConfigSetting.Builder itemBuilder;
        private final CommentRenderer linkValueRenderer;
        private final CommentRenderer textRenderer;
        private final CommentRenderer simpleRenderer;

        public CollectingScanner(Set<String> actionableTags, VariableElement element, ConfigSetting.Builder itemBuilder, CommentRenderer textRenderer, CommentRenderer linkValueRenderer) {
            super(actionableTags);
            this.element = element;
            this.linkValueRenderer = linkValueRenderer;
            this.textRenderer = textRenderer;
            simpleRenderer = new SimpleRenderer();
            this.itemBuilder = itemBuilder;
        }

        @Override
        protected Void processActionableTag(BlockTagTree node, Void nothing) {
            String tagName = node.getTagName();
            if (TAG_CFG_DESCRIPTION.equals(tagName)) {
                addDescription(node);
            } else if (TAG_CFG_EXAMPLE.equals(tagName)) {
                addExample(node);
            } else if (TAG_CFG_DEFAULT_VALUE.equals(tagName)) {
                addDefault(node);
            }  else if (TAG_CFG_SORT_KEY.equals(tagName)) {
                addSortKey(node);
            } else //noinspection StatementWithEmptyBody
                if (TAG_CFG_INCLUDE.equals(tagName)) {
                // nothing to do
            } else {
                log.warning(() -> String.format("unsupported tag %s", node.getTagName()));
            }
            //noinspection RedundantCast
            return (Void) null;
        }

        private void addSortKey(BlockTagTree node) {
            String sortKey = simpleRenderer.render(Collections.singleton(node));
            if (sortKey != null) {
                sortKey = sortKey.trim();
                if (!sortKey.isEmpty()) {
                    itemBuilder.sortKey(sortKey);
                }
            }
        }

        private void addExample(BlockTagTree node) {
            itemBuilder.exampleValue(simpleRenderer.render(Collections.singleton(node)));
        }

        private void addDescription(BlockTagTree node) {
            Collection<? extends DocTree> targets;
            if (node instanceof UnknownBlockTagTree) {
                targets = ((UnknownBlockTagTree)node).getContent();
            } else {
                targets = Collections.singleton(node);
            }
            String text = textRenderer.render(targets);
            itemBuilder.description(text);
        }

        private void addDefault(BlockTagTree node) {
            String defaultValue = null;
            if (node instanceof UnknownBlockTagTree) {
                List<? extends DocTree> content = ((UnknownBlockTagTree) node).getContent();
                defaultValue = linkValueRenderer.render(content);
            }
            if (defaultValue == null) {
                defaultValue = simpleRenderer.render(Collections.singleton(node));
            }
            itemBuilder.defaultValue(defaultValue);
        }

    }

    protected Path resolveOutputPath() {
        String defaultValue = new File(System.getProperty("user.dir")).getAbsolutePath();
        String outputDirectory = optionage.getOptionString(OPT_OUTPUT_DIRECTORY, defaultValue);
        return new File(outputDirectory).toPath().resolve(getOutputFilename());
    }

    protected String getOutputFilename() {
        return optionage.getOptionString(OPT_OUTPUT_FILENAME, DEFAULT_OUTPUT_FILENAME);
    }

}
