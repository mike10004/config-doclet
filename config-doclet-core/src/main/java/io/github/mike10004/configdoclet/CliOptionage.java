package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class CliOptionage implements Optionage {

    // immutable
    private final Set<? extends Doclet.Option> options;

    // mutable
    private final Processage processage;

    private CliOptionage(Set<? extends Doclet.Option> options, Processage processage) {
        this.options = Collections.unmodifiableSet(requireNonNull(options));
        this.processage = requireNonNull(processage);
    }

    @Nullable
    @Override
    public List<String> getOptionStrings(String name) {
        Doclet.Option target = this.options.stream().filter(option -> {
            return option.getNames().contains(name);
        }).findFirst().orElseThrow(() -> new IllegalArgumentException("not a supported option: " + name));
        List<String> allNames = target.getNames();
        for (String key : this.processage.keySet()) {
            if (allNames.contains(key)) {
                List<String> args = this.processage.get(key);
                if (args != null) {
                    return args;
                }
            }
        }
        return null;
    }

    private static Doclet.Option dummyOption(String name, int numArgs, String...otherNames) {
        return dummyOption(Doclet.Option.Kind.STANDARD, name, numArgs, otherNames);
    }

    @SuppressWarnings("SameParameterValue")
    private static Doclet.Option dummyExtendedOption(String name, int numArgs, String...otherNames) {
        return dummyOption(Doclet.Option.Kind.EXTENDED, name, numArgs, otherNames);
    }

    private static Doclet.Option doclintOptions() {
        return new Doclet.Option() {
            @Override
            public int getArgumentCount() {
                return 0;
            }

            @Override
            public String getDescription() {
                return "not used by this doclet";
            }

            @Override
            public Kind getKind() {
                return Kind.EXTENDED;
            }

            @Override
            public List<String> getNames() {
                return Arrays.asList(
                        "-Xdoclint:none", 
                        "-Xdoclint:all", 
                        "-Xdoclint:accessibility", 
                        "-Xdoclint:html", 
                        "-Xdoclint:missing", 
                        "-Xdoclint:reference", 
                        "-Xdoclint:syntax",
                        "-Xdoclint"
                );
            }

            @Override
            public String getParameters() {
                return "";
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                return true;
            }
        };
    }

    private static Doclet.Option dummyOption(Doclet.Option.Kind kind, String name, int numArgs, String...otherNames) {
        BasicOption.Processor processor = (x, y) -> true;
        return BasicOption.builder(name, processor)
                .argCount(numArgs)
                .kind(kind)
                .aliases(Arrays.stream(otherNames))
                .build();
    }

    private static Set<? extends Doclet.Option> getInternalOptions(Processage processage) {
        BasicOption.Processor processor = processage.processor();
        //noinspection RedundantArrayCreation
        return new HashSet<>(Arrays.asList(new Doclet.Option[]{
                BasicOption.builder(ConfigDoclet.OPT_OUTPUT_DIRECTORY, processor)
                        .alias(ConfigDoclet.OPT_OUTPUT_DIRECTORY_ALIAS)
                        .arg("<dirname>")
                        .description("set output directory; see also " + ConfigDoclet.OPT_OUTPUT_FILENAME)
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_OUTPUT_FORMAT, processor)
                        .autoAlias()
                        .arg("<type>")
                        .description("set config help output format (either 'properties' or 'json')")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_FIELD_NAME_REGEX, processor)
                        .autoAlias()
                        .arg("<regex>")
                        .description("restrict documentable fields to those whose name matches a regex")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_OUTPUT_FILENAME, processor)
                        .autoAlias()
                        .arg("<filename>")
                        .description("set output filename")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_FIELD_NAME_PATTERN, processor)
                        .autoAlias()
                        .arg("<patterns>")
                        .description("restrict documentable fields to those whose name matches a wildcard pattern (using '*' and '?'); delimit multiple patterns with commas or whitespace")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_APPEND_SETTINGS, processor)
                        .autoAlias()
                        .arg("<jsonfile>")
                        .description("append the settings parsed from json output of prior execution of this doclet")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_ASSIGNATION_HINT, processor)
                        .autoAlias()
                        .arg("<auto|always|never>")
                        .description("in properties output, specifies whether the value assignation in the output properties file is commented")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_FOOTER, processor)
                        .autoAlias()
                        .arg("<text|fileurl>")
                        .description("set header")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_HEADER, processor)
                        .autoAlias()
                        .arg("<text|fileurl>")
                        .description("set header")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_DOCENCODING, processor)
                        .autoAlias()
                        .arg("<charset>")
                        .description("set output charset")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_TEST_MODE, processor)
                        .autoAlias()
                        .arg(ConfigDoclet.TestMode.describeChoices())
                        .description("specify test mode (for testing purposes)")
                        .build(),
                dummyOption("-charset", 1),
                dummyOption("-author", 0),
                dummyOption("-noindex", 0),
                dummyOption("-splitindex", 0),
                dummyOption("-nonavbar", 0),
                dummyOption("-use", 0),
                dummyOption("-version", 0),
                dummyOption("-notree", 0),
                dummyOption("-top", 1),
                dummyOption("-excludedocfilessubdir", 1),
                dummyOption("-doctitle", 1),
                dummyOption("-windowtitle", 1),
                dummyOption("-bottom", 1),
                dummyOption("-linkoffline", 2),
                doclintOptions(),
                dummyExtendedOption("-Xdocrootparent", 1),
        }));
    }

    @Override
    public Set<? extends Doclet.Option> getSupportedOptions() {
        return options;
    }

    public static CliOptionage standard() {
        Processage processage = Processage.fromMap(new HashMap<>());
        Set<? extends Doclet.Option> options = getInternalOptions(processage);
        return new CliOptionage(options, processage);
    }

    interface Processage {

        boolean accept(String option, List<String> args);

        Set<String> keySet();

        List<String> get(String option);

        static Processage fromMap(Map<String, List<String>> map) {
            return new Processage() {
                @Override
                public boolean accept(String option, List<String> args) {
                    if (option.startsWith("--")) {
                        option = option.split("=", 2)[0];
                    }
                    map.put(option, args);
                    return true;
                }

                @Override
                public Set<String> keySet() {
                    return map.keySet();
                }

                @Override
                public List<String> get(String option) {
                    return map.get(option);
                }
            };
        }

        default BasicOption.Processor processor() {
            return this::accept;
        }
    }
}
