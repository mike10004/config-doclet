package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Interface that provides methods to parse and query a command line.
 */
public interface Optionage {

    /**
     * Gets the set of supported options.
     * @return the set of supported options
     * @see Doclet#getSupportedOptions()
     */
    Set<? extends Doclet.Option> getSupportedOptions();

    /**
     * Gets a single parameter value for the given option or returns a default value. Use this
     * method only for options that take exactly one parameter value, because only the first
     * value will be returned for options accepting multiple parameter values. For options
     * not accepting parameter values, this method will always return the default value.
     * To check whether an option not accepting parameter values is present, use
     * {@link #isPresent(String)}.
     *
     * @param name the option name
     * @param defaultValue the default value; if null, this method may return null
     * @return the parameter value for the given option, or
     */
    default String getOptionString(String name, String defaultValue) {
        List<String> paramValues = getOptionStrings(name);
        if (paramValues != null && !paramValues.isEmpty()) {
            return paramValues.get(0);
        }
        return defaultValue;
    }

    /**
     * Gets the list of parameter values specified for the given option if it is present.
     * @param name the option name
     * @return the list of parameter values specified for the given option, or null if the option was not present
     */
    @Nullable
    List<String> getOptionStrings(String name);

    /**
     * Checks whether this option is present, meaning it was present on the command line that was parsed.
     * @param name the option name
     * @return true iff option is present
     */
    default boolean isPresent(String name) {
        return getOptionStrings(name) != null;
    }

    static Optionage compose(Optionage priority, Optionage...lessers) {
        List<Optionage> optionages = Stream.concat(Stream.of(priority), Stream.of(lessers)).collect(Collectors.toList());
        return new Optionage() {
            @Override
            public Set<? extends Doclet.Option> getSupportedOptions() {
                return optionages.stream()
                        .flatMap(optionage -> optionage.getSupportedOptions().stream())
                        .collect(Collectors.toSet());
            }

            @Nullable
            @Override
            public List<String> getOptionStrings(String name) {
                for (Optionage optionage : optionages) {
                    @Nullable List<String> paramValues = optionage.getOptionStrings(name);
                    if (paramValues != null) {
                        return paramValues;
                    }
                }
                return null;
            }

            @Override
            public String getOptionString(String name, String defaultValue) {
                for (Optionage optionage : optionages) {
                    String value = optionage.getOptionString(name, null);
                    if (value != null) {
                        return value;
                    }
                }
                return defaultValue;
            }

            @Override
            public boolean isPresent(String name) {
                return optionages.stream().anyMatch(optionage -> optionage.isPresent(name));
            }
        };
    }
}
