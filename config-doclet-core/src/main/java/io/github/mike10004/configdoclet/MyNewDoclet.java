package io.github.mike10004.configdoclet;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.StandardDoclet;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class MyNewDoclet implements Doclet {

    private final Level defaultLevel = Level.FINER;

    private static final Logger log = Logger.getLogger(MyNewDoclet.class.getName());

    private static final String TAG_CFG_DESCRIPTION = "cfg.description";
    private static final String TAG_CFG_EXAMPLE = "cfg.example";
    private static final String TAG_DEFAULT_VALUE = "cfg.default";

    private final Set<String> actionableTags;
    private Predicate<? super Name> elementNamePredicate;
    private Reporter reporter;
    private final Set<Doclet.Option> options;

    public MyNewDoclet() {
        actionableTags = new HashSet<>();
        options = new HashSet<>();
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        actionableTags.add(TAG_CFG_DESCRIPTION);
        actionableTags.add(TAG_CFG_EXAMPLE);
        elementNamePredicate = x -> true;
        this.reporter = reporter;
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
        List<ModelItem> items = Collections.synchronizedList(new ArrayList<>());
        environment.getIncludedElements().stream()
                .filter(element -> element.getKind() == ElementKind.CLASS)
                .filter(TypeElement.class::isInstance)
                .map(e -> (TypeElement) e).forEach(clsEl -> {
            clsEl.getEnclosedElements().stream().filter(this::isActionableEnclosedElement).forEach(enclosed -> {
                DocCommentTree tree = environment.getDocTrees().getDocCommentTree(enclosed);
                log.log(defaultLevel, () -> String.format("enclosed: kind=%s; name=%s", enclosed.getKind(), enclosed.getSimpleName()));
                Object constValue = ((VariableElement)enclosed).getConstantValue();
                if (constValue != null) {
                    ModelItem.Builder b = ModelItem.builder();
                    b.configPropertyName(constValue.toString());
                    CollectingVisitor visitor = new CollectingVisitor((VariableElement) enclosed, b);
                    //noinspection RedundantCast
                    visitor.scan(tree, (Void) null);
                    ModelItem item = b.build();
                    items.add(item);
                } else {
                    reporter.print(Diagnostic.Kind.NOTE, String.format("element does not have constant value: %s", enclosed.getSimpleName()));
                }
            });
        });
        boolean retval = produceOutput(items);
        return retval;
    }

    protected Charset getOutputCharset() {
        // TODO get charset from options
        return StandardCharsets.UTF_8;
    }

    private String getCommentChar() {
        // TODO get comment char from options
        return "#";
    }

    protected Function<? super ModelItem, String> getItemFormatter() {
        final String COMMENT_CHAR = getCommentChar();
        return item -> {
            String description = item.description == null ? "" : item.description;
            String value = "";
            if (item.defaultValue != null) {
                value = item.defaultValue;
            } else if (!item.exampleValues.isEmpty()) {
                value = item.exampleValues.get(0).value;
            }
            return String.format("%s %s%n%s %s = %s%n", COMMENT_CHAR, description, COMMENT_CHAR, item.configPropertyName, value);
        };
    }

    protected boolean produceOutput(List<ModelItem> items) {
        File outputFile = resolveOutputPath().toFile();
        Function<? super ModelItem, String> formatter = getItemFormatter();
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), getOutputCharset()))) {
            for (ModelItem item : items) {
                String content = formatter.apply(item);
                out.println(content);
            }
        } catch (IOException e) {
            reporter.print(Diagnostic.Kind.ERROR, "failed to write to output file " + outputFile);
            return false;
        }
        return true;
    }

    private class CollectingVisitor extends MyDocTreeVisitor<Void, Void> {

        @SuppressWarnings("unused")
        private final VariableElement element;
        private final ModelItem.Builder itemBuilder;

        public CollectingVisitor(VariableElement element, ModelItem.Builder itemBuilder) {
            super(actionableTags);
            this.element = element;
            this.itemBuilder = itemBuilder;
        }

        @Override
        protected Void processActionableTag(BlockTagTree node, Void nothing) {
            String tagName = node.getTagName();
            if (TAG_CFG_DESCRIPTION.equals(tagName)) {
                addDescription(node);
            } else if (TAG_CFG_EXAMPLE.equals(tagName)) {
                addExample(node);
            } else if (TAG_DEFAULT_VALUE.equals(tagName)) {
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

        private void addDefault(BlockTagTree node) {
            fudgeIt(itemBuilder::defaultValue, node);
        }

        private void fudgeIt(Consumer<? super String> mangling, BlockTagTree node) {
            ToStringHelper h = new ToStringHelper(node.getClass());
            h.add("toString", node.toString());
            mangling.accept(h.toString());
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

    public static class ExampleValue {
        public final String value;
        public final String description;

        public ExampleValue(String value, String description) {
            this.value = requireNonNull(value);
            this.description = description;
        }
    }

    public static class ModelItem {
        public final String configPropertyName;
        public final String description;
        public final String defaultValue;
        public final List<ExampleValue> exampleValues;

        private ModelItem(Builder builder) {
            configPropertyName = builder.configPropertyName;
            description = builder.description;
            defaultValue = builder.defaultValue;
            exampleValues = Collections.unmodifiableList(builder.exampleValues);
        }

        public static Builder builder() {
            return new Builder();
        }

        @SuppressWarnings("UnusedReturnValue")
        public static final class Builder {

            private String configPropertyName;
            private String description;
            private String defaultValue;

            private final List<ExampleValue> exampleValues;

            private Builder() {
                exampleValues = new ArrayList<>();
            }

            public Builder configPropertyName(String val) {
                configPropertyName = val;
                return this;
            }

            public Builder description(String val) {
                description = val;
                return this;
            }

            public Builder defaultValue(String val) {
                defaultValue = val;
                return this;
            }

            public Builder exampleValue(String val) {
                return exampleValue(new ExampleValue(val, null));
            }

            public Builder exampleValue(ExampleValue val) {
                exampleValues.add(val);
                return this;
            }

            public ModelItem build() {
                return new ModelItem(this);
            }
        }
    }

    @SuppressWarnings("unused")
    protected boolean isSupported(Doclet.Option option) {
        return true;
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        // TODO add option for element name predicate
        //noinspection RedundantStreamOptionalCall
        new StandardDoclet().getSupportedOptions().stream()
                .filter(this::isSupported)
                .forEach(this.options::add);
        return options;
    }

    protected Path resolveOutputPath() {
        String outputDirectory = this.options.stream().filter(option -> {
            return option.getNames().contains("-d");
        }).map(Option::getParameters)
          .findFirst().orElseGet(() -> new File(System.getProperty("user.dir")).getAbsolutePath());
        return new File(outputDirectory).toPath().resolve(getOutputFilename());
    }

    protected String getOutputFilename() {
        // TODO get filename from options
        return "cfg-doclet-output.txt";
    }
}
