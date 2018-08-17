package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Class that emulates command line arguments using system properties.
 * Note that the way system properties are parsed and made available means
 * that all properties are defined with exactly one value (even if that value
 * is empty). This affects the interpretation of properties as command line
 * options because options that do not accept parameter values do indeed
 * appear to have a single empty string parameter value. This should
 * not have any practical effects, because the way you query a boolean option
 * returns the same response regardless of parameter value.
 */
class PropertyOptionage implements Optionage {

    private static final String DEFAULT_SYSPROP_PREFIX = "configdoclet.";

    private final Function<String, String> sysprops;
    private final String prefix;

    PropertyOptionage(Function<String, String> sysprops, String prefix) {
        this.sysprops = requireNonNull(sysprops);
        this.prefix = requireNonNull(prefix);
    }

    public static PropertyOptionage system() {
        return new PropertyOptionage(System::getProperty, DEFAULT_SYSPROP_PREFIX);
    }

    @Override
    public Set<? extends Doclet.Option> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Nullable
    private String normalizedGet(String name) {
        String propName = toSyspropName(name);
        String value = sysprops.apply(propName);
        return value;
    }

    @Override
    @Nullable
    public List<String> getOptionStrings(String name) {
        String value = normalizedGet(name);
        if (value != null) {
            return Collections.singletonList(value);
        }
        return null;
    }

    String toSyspropName(String optionName) {
        String suffix = Stringage.trimLeadingFrom(optionName, '-');
        return prefix + suffix;
    }

    @Override
    public boolean isPresent(String name) {
        return normalizedGet(name) != null;
    }
}
