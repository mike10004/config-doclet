package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.StandardDoclet;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

class CliOptionage implements Optionage {

    // immutable
    private final Set<Doclet.Option> options;

    // mutable
    private final Processage processage;

    private CliOptionage(Set<Doclet.Option> options, Processage processage) {
        this.options = Collections.unmodifiableSet(requireNonNull(options));
        this.processage = requireNonNull(processage);
    }

    private <T> T getOptionValue(String name, Function<List<String>, T> parser, T defaultValue) {
        Doclet.Option target = this.options.stream().filter(option -> {
            return option.getNames().contains(name);
        }).findFirst().orElseThrow(() -> new IllegalArgumentException("no option by name " + name));
        List<String> allNames = target.getNames();
        for (String key : this.processage.keySet()) {
            if (allNames.contains(key)) {
                List<String> args = this.processage.get(key);
                return parser.apply(args);
            }
        }
        return defaultValue;
    }

    @Override
    public String getOptionString(String name, String defaultValue) {
        return getOptionValue(name, list -> list.get(0), defaultValue);
    }

    @SuppressWarnings("unused")
    protected boolean isSupported(Doclet.Option option) {
        return true;
    }

    private static Set<Doclet.Option> deconflict(Set<? extends Doclet.Option> superset, Set<? extends Doclet.Option> preferred) {
        Set<Doclet.Option> combo = new HashSet<>(preferred);
        for (Doclet.Option disliked : superset) {
            for (Doclet.Option preferredOne : preferred) {
                if (!BasicOption.isConflicting(preferredOne, disliked)) {
                    combo.add(disliked);
                }
            }
        }
        return combo;
    }

    private static Set<? extends Doclet.Option> getInternalOptions(Processage processage) {
        BasicOption.Processor processor = processage.processor();
        //noinspection RedundantArrayCreation
        return new HashSet<>(Arrays.asList(new Doclet.Option[]{
                BasicOption.builder(ConfigDoclet.OPT_OUTPUT_FORMAT, processor)
                        .alias("--output-format")
                        .arg("<type>")
                        .description("set config help output format (either 'properties' or 'json')")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_FIELD_NAME_REGEX, processor)
                        .arg("<regex>")
                        .description("restrict documentable fields to those whose name matches a regex")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_OUTPUT_FILENAME, processor)
                        .arg("<filename>")
                        .description("set output filename")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_FIELD_NAME_PATTERN, processor)
                        .arg("<patterns>")
                        .description("restrict documentable fields to those whose name matches a wildcard pattern (using '*' and '?'); delimit multiple patterns with commas or whitespace")
                        .build()
        }));
    }

    @Override
    public Set<? extends Doclet.Option> getSupportedOptions() {
        return options;
    }

    static boolean isSupportedHere(Doclet.Option option) {
        return true;
    }

    public static CliOptionage standard() {
        Processage processage = Processage.fromMap(new HashMap<>());
        Set<Doclet.Option> options = createOptionSet(processage);
        return new CliOptionage(options, processage);
    }

    private static Set<Doclet.Option> createOptionSet(Processage processage) {
        // TODO add option for element name predicate
        //noinspection RedundantStreamOptionalCall
        Set<? extends Doclet.Option> defaultOptions = new StandardDoclet().getSupportedOptions().stream()
                .filter(CliOptionage::isSupportedHere)
                .map(option -> wrap(option, processage))
                .collect(Collectors.toSet());
        Set<? extends Doclet.Option> internalOptions = getInternalOptions(processage);
        return (deconflict(defaultOptions, internalOptions));
    }

    private static BasicOption wrap(Doclet.Option opt, Processage processage) {
        BasicOption.Processor p = (option, arguments) -> {
            boolean retval = opt.process(option, Collections.unmodifiableList(arguments));
            boolean retval2 = processage.accept(option, arguments);
            return retval && retval2;
        };
        return BasicOption.builder(opt.getNames().get(0), p)
                .aliases(opt.getNames().subList(1, opt.getNames().size()).stream())
                .description(opt.getDescription())
                .parameters(opt.getParameters())
                .argCount(opt.getArgumentCount())
                .build();
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
