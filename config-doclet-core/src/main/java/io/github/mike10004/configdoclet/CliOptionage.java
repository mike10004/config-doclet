package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.StandardDoclet;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_APPEND_SETTINGS, processor)
                        .arg("<jsonfile>")
                        .description("append the settings parsed from json output of this doclet in specified file")
                        .build(),
                BasicOption.builder(ConfigDoclet.OPT_ASSIGNATION_HINT, processor)
                        .arg("<auto|always|never>")
                        .description("in properties output, specifies whether the value assignation in the output properties file is commented")
                        .build(),
        }));
    }

    @Override
    public Set<? extends Doclet.Option> getSupportedOptions() {
        return options;
    }

    public static CliOptionage standard() {
        return standard(x -> true);
    }

    public static CliOptionage standard(Predicate<? super Doclet.Option> supportPredicate) {
        Processage processage = Processage.fromMap(new HashMap<>());
        Set<Doclet.Option> options = createOptionSet(supportPredicate, processage);
        return new CliOptionage(options, processage);
    }

    private static Set<Doclet.Option> createOptionSet(Predicate<? super Doclet.Option> supportPredicate, Processage processage) {
        // TODO add option for element name predicate
        //noinspection RedundantStreamOptionalCall
        Set<? extends Doclet.Option> defaultOptions = new StandardDoclet().getSupportedOptions().stream()
                .filter(supportPredicate)
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
