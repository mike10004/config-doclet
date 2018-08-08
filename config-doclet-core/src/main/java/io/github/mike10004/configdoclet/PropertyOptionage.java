package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

class PropertyOptionage implements Optionage {

    private static final String DEFAULT_SYSPROP_PREFIX = "configdoclet.";
    private static final String DASH = "-";

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

    @Override
    public String getOptionString(String name, String defaultValue) {
        String propName = toSyspropName(name);
        String value = sysprops.apply(propName);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    String toSyspropName(String optionName) {
        String suffix = trimLeadingFrom(optionName, DASH);
        return prefix + suffix;
    }

    @SuppressWarnings("SameParameterValue")
    private static String trimLeadingFrom(String name, String illegal) {
        while (name.startsWith(illegal)) {
            name = name.substring(illegal.length());
        }
        return name;
    }
}
