package io.github.mike10004.configdoclet;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.annotation.Nullable;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class ConfigDoclet implements Doclet {

    private final Level defaultLevel = Level.FINER;

    private static final Logger log = Logger.getLogger(ConfigDoclet.class.getName());

    private static final String TAG_CFG_DESCRIPTION = "cfg.description";
    private static final String TAG_CFG_EXAMPLE = "cfg.example";
    private static final String TAG_CFG_DEFAULT_VALUE = "cfg.default";

    final Set<String> actionableTags;
    private Predicate<? super CharSequence> elementNamePredicate;
    private Reporter reporter;
    private final Optionage optionage;

    public ConfigDoclet() {
        this(Optionage.compose(CliOptionage.standard(), PropertyOptionage.system()));
    }

    ConfigDoclet(Optionage optionage) {
        actionableTags = new HashSet<>();
        this.optionage = requireNonNull(optionage);
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        actionableTags.add(TAG_CFG_DESCRIPTION);
        actionableTags.add(TAG_CFG_EXAMPLE);
        actionableTags.add(TAG_CFG_DEFAULT_VALUE);
        elementNamePredicate = startsWithAny("PROP_", "PROPERTY_", "CFG_", "CONFIG_");
        this.reporter = reporter;
    }

    @SuppressWarnings("SameParameterValue")
    static Predicate<? super CharSequence> startsWithAny(String prefix1, String...others) {
        Set<String> set = Stream.concat(Stream.of(prefix1), Stream.of(others)).collect(Collectors.toSet());
        return name -> {
            String nameStr = name.toString();
            return set.stream().anyMatch(nameStr::startsWith);
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

    private boolean isActionableEnclosedElement(Element element) {
        Name name = element.getSimpleName();
        return element.getKind() == ElementKind.FIELD
                && element.getModifiers().contains(Modifier.STATIC)
                && element.getModifiers().contains(Modifier.FINAL)
                && element instanceof VariableElement
                && elementNamePredicate.test(name);
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        List<ConfigSetting> items = Collections.synchronizedList(new ArrayList<>());
        List<VariableElement> variableElements = environment.getIncludedElements().stream()
                .filter(element -> element.getKind() == ElementKind.CLASS)
                .filter(TypeElement.class::isInstance)
                .map(e -> (TypeElement) e)
                .flatMap(classElement -> classElement.getEnclosedElements().stream())
                .filter(VariableElement.class::isInstance)
                .map(VariableElement.class::cast)
                .collect(Collectors.toList());
        LinkResolver linkResolver = new CollectionLinkResolver(variableElements);
        variableElements.stream().filter(this::isActionableEnclosedElement)
                .forEach(enclosed -> {
                    log.log(defaultLevel, () -> String.format("enclosed: kind=%s; name=%s", enclosed.getKind(), enclosed.getSimpleName()));
                    DocCommentTree tree = environment.getDocTrees().getDocCommentTree(enclosed);
                    if (tree != null) {
                        Object constValue = enclosed.getConstantValue();
                        if (constValue != null) {
                            ConfigSetting.Builder b = prepareBuilder(enclosed, constValue, tree.getFullBody());
                            CollectingScanner visitor = new CollectingScanner(enclosed, b, linkResolver);
                            //noinspection RedundantCast
                            visitor.scan(tree, (Void) null);
                            ConfigSetting item = b.build();
                            items.add(item);
                        }
                    } else {
                        reporter.print(Diagnostic.Kind.NOTE, String.format("element does not have constant value: %s", enclosed.getSimpleName()));
                    }
                });
        boolean retval = produceOutput(items);
        return retval;
    }

    static String qualifySignature(String signature, Element element) {
        String fieldPrefix = getPrefix(ElementKind.FIELD);
        if (signature.startsWith(fieldPrefix)) {
            String parentSignature = constructSignature(element.getEnclosingElement());
            return parentSignature + signature;
        }
        return signature;
    }

    static String constructSignature(Element element) {
        List<Element> lineage = new ArrayList<>();
        do {
            lineage.add(element);
            element = element.getEnclosingElement();
        } while (element != null);
        Collections.reverse(lineage);
        StringBuilder sb = new StringBuilder();
        for (Element el : lineage) {
            if (!isSignaturePart(el)) {
                continue;
            }
            sb.append(getPrefix(el));
            Name name = el.getSimpleName();
            sb.append(name.toString());
        }
        return sb.toString();
    }

    private static boolean isSignaturePart(Element el) {
        switch (el.getKind()) {
            case PACKAGE:
            case CLASS:
            case METHOD:
            case FIELD:
                return true;
            default:
                return false;
        }

    }

    private static String getPrefix(Element el) {
        return getPrefix(el.getKind());
    }

    private static String getPrefix(ElementKind kind) {
        switch (kind) {
            case PACKAGE:
                return "";
            case CLASS:
                return ".";
            case METHOD:
            case FIELD:
                return "#";
            default:
                return "";
        }
    }

    private class CollectionLinkResolver  implements LinkResolver {

        private final Collection<VariableElement> elements;
        private final Map<VariableElement, String> signatureCache;

        public CollectionLinkResolver(Collection<VariableElement> elements) {
            this.elements = elements;
            signatureCache = new HashMap<>();
        }

        @Nullable
        @Override
        public VariableElement resolve(Element context, String signature) {
            requireNonNull(signature, "signature");
            return elements.stream().filter(element -> {
                String elSignature = getElementSignature(element);
                return signature.equals(elSignature);
            }).findFirst().orElse(null);
        }

        private String getElementSignature(VariableElement el) {
            return signatureCache.computeIfAbsent(el, ConfigDoclet::constructSignature);
        }
    }

    private ConfigSetting.Builder prepareBuilder(@SuppressWarnings("unused") VariableElement element, Object constValue, List<? extends DocTree> fullBody) {
        ConfigSetting.Builder b = ConfigSetting.builder();
        b.key(constValue.toString());
        String description = createDescriptionFromFullBody(fullBody);
        b.description(description);
        return b;
    }

    String createDescriptionFromFullBody(List<? extends DocTree> fullBody) {
        return joinText(fullBody, " ");
    }

    @SuppressWarnings("SameParameterValue")
    String joinText(List<? extends DocTree> fullBody, String delimiter) {
        return fullBody.stream()
                .filter(TextTree.class::isInstance)
                .map(tree -> (TextTree) tree)
                .map(TextTree::getBody)
                .collect(Collectors.joining(delimiter));
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
        String format = optionage.getOptionString(OPT_OUTPUT_FORMAT, OUTPUT_PROPERTIES);
        switch (format) {
            case OUTPUT_PROPERTIES:
                return new PropertiesOutputFormatter();
            case OUTPUT_JSON:
                reporter.print(Diagnostic.Kind.NOTE, String.format("java.class.path=%s", System.getProperty("java.class.path")));
                return new GsonOutputFormatter();
            default:
                throw new IllegalArgumentException("invalid output format");

        }
    }

    interface OutputFormatter {
        void format(List<ConfigSetting> items, PrintWriter out) throws IOException;
    }

    protected boolean produceOutput(List<ConfigSetting> items) {
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

    static final String OPT_OUTPUT_FORMAT = "-outputformat";

    static final String OUTPUT_PROPERTIES = "properties";
    static final String OUTPUT_JSON = "json";

    private interface LinkResolver {
        @Nullable
        VariableElement resolve(Element context, String signature);
    }

    private class CollectingScanner extends ActionableTagScanner<Void, Void> {

        @SuppressWarnings("unused")
        private final VariableElement element;
        private final ConfigSetting.Builder itemBuilder;
        private final LinkResolver linkResolver;

        public CollectingScanner(VariableElement element, ConfigSetting.Builder itemBuilder, LinkResolver linkResolver) {
            super(actionableTags);
            this.element = element;
            this.linkResolver = linkResolver;
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
            } else {
                log.warning(() -> String.format("unsupported tag %s", node.getTagName()));
            }
            //noinspection RedundantCast
            return (Void) null;
        }

        private void addExample(BlockTagTree node) {
            fudgeIt(itemBuilder::exampleValue, node);
        }

        private void addDescription(BlockTagTree node) {
            fudgeIt(itemBuilder::description, node);
        }

        @Nullable
        private VariableElement findVariableElementForSignature(String signature) {
            signature = qualifySignature(signature, element);
            return linkResolver.resolve(element, signature);
        }

        private void addDefault(BlockTagTree node) {
            if (node instanceof UnknownBlockTagTree) {
                List<? extends DocTree> content = ((UnknownBlockTagTree) node).getContent();
                if (!content.isEmpty() && content.get(0) instanceof LinkTree) {
                    LinkTree link = (LinkTree) content.get(0);
                    String signature = link.getReference().getSignature();
                    if (signature == null) {
                        reporter.print(Diagnostic.Kind.WARNING, "tried to resolve empty signature");
                    } else {
                        @Nullable VariableElement element = findVariableElementForSignature(signature);
                        if (element != null) {
                            Object constValue = element.getConstantValue();
                            String defaultValue = "";
                            if (constValue != null) {
                                defaultValue = constValue.toString();
                            }
                            itemBuilder.defaultValue(defaultValue);
                            return;
                        }
                    }
                }
                String defaultValue = joinText(content, " ");
                itemBuilder.defaultValue(defaultValue);
            } else {
                fudgeIt(itemBuilder::defaultValue, node);
            }
        }

        private void fudgeIt(Consumer<? super String> mangling, BlockTagTree node) {
            String stringified = node.toString();
            String expectedPrefix = "@" + node.getTagName();
            String outcome;
            if (stringified.startsWith(expectedPrefix)) {
                outcome = StringUtils.removeStart(stringified, expectedPrefix);
            } else {
                ToStringHelper h = new ToStringHelper(node.getClass());
                h.add("toString", node.toString());
                outcome = h.toString();

            }
            mangling.accept(outcome);
        }

    }

    @SuppressWarnings("UnusedReturnValue")
    private static class ToStringHelper {
        private final String itemClass;
        private final List<Object[]> properties;

        public ToStringHelper(Class<?> itemClass) {
            this(itemClass.getSimpleName());
        }

        public ToStringHelper(String itemClass) {
            this.itemClass = itemClass;
            properties = new ArrayList<>();
        }

        public ToStringHelper add(String name, Object value) {
            properties.add(new Object[]{name, value});
            return this;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder(itemClass.length() + 2 + properties.size() * 4);
            b.append(itemClass).append("{");
            properties.forEach(property -> {
                String name = (String) property[0];
                Object value = property[1];
                b.append(name).append('=').append(value);
            });
            b.append('}');
            return b.toString();
        }
    }


    protected Path resolveOutputPath() {
        String defaultValue = new File(System.getProperty("user.dir")).getAbsolutePath();
        String outputDirectory = optionage.getOptionString("-d", defaultValue);
        return new File(outputDirectory).toPath().resolve(getOutputFilename());
    }

    protected String getOutputFilename() {
        // TODO get filename from options
        return "cfg-doclet-output.txt";
    }

}
