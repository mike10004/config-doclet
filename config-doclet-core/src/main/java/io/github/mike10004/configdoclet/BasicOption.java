package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

class BasicOption implements Doclet.Option {

    private final List<String> names;
    private final Processor processor;
    private final String parameters;
    private final String description;
    private final int argCount;
    private final Kind kind;

    public BasicOption(List<String> names, Processor processor, String parameters, String description, int argCount, Kind kind) {
        this.processor = requireNonNull(processor, "processor");
        requireNonNull(names, "names");
        this.names = Collections.unmodifiableList(names);
        if (names.isEmpty()) {
            throw new IllegalArgumentException("names array must be nonempty");
        }
        if (names.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("all names must be non-null");
        }
        if (names.stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("all names must be nonempty");
        }
        this.parameters = parameters;
        this.description = requireNonNull(description, "description");
        this.argCount = argCount;
        if (argCount < 0) {
            throw new IllegalArgumentException("argcount >= 0 is required: " + argCount);
        }
        this.kind = requireNonNull(kind);
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        return processor.process(option, arguments);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public String getParameters() {
        return parameters;
    }

    @Override
    public int getArgumentCount() {
        return argCount;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
                .add("names", names)
                .add("description", StringUtils.abbreviate(description, 16))
                .add("argCount", argCount)
                .add("metavars", parameters)
                .toString();
    }

    @SuppressWarnings("SameParameterValue")
    private static String trimLeading(String original, String ugly) {
        while (original.startsWith(ugly)) {
            original = original.substring(ugly.length());
        }
        return original;
    }

    private static final String OPTION_INDICATOR = "-";

    public static boolean isConflicting(Doclet.Option me, Doclet.Option option) {
        for (String myName : me.getNames()) {
            myName = trimLeading(myName, OPTION_INDICATOR);
            for (String otherName : option.getNames()) {
                otherName = trimLeading(otherName, OPTION_INDICATOR);
                if (myName.equalsIgnoreCase(otherName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Interface for a service class that will handle the {@link Doclet.Option#process(String, List)} method.
     */
    public interface Processor {
        boolean process(String option, List<String> arguments);
    }

    /**
     * Builder of option instances.
     */
    public static class Builder {
        private final List<String> names;
        private final Processor processor;
        private String parameters;
        private String description;
        private int argCount;
        private Kind kind;

        private Builder(String name, Processor processor) {
            requireNonNull(name, "name");
            this.names = new ArrayList<>();
            this.names.add(name);
            this.processor = requireNonNull(processor, "processor");
            parameters = null;
            description = "set " + trimLeading(name, OPTION_INDICATOR);
            kind = Kind.STANDARD;
        }

        public Builder alias(String name) {
            requireNonNull(name, "name");
            if (name.isEmpty()) {
                throw new IllegalArgumentException("name must be nonempty");
            }
            this.names.add(name);
            return this;
        }

        public Builder aliases(Stream<String> names) {
            names.forEach(this::alias);
            return this;
        }

        public Builder description(String val) {
            this.description = val;
            return this;
        }

        public Builder parameters(String val) {
            this.parameters = val;
            return this;
        }

        public Builder argCount(int val) {
            this.argCount = val;
            return this;
        }

        public BasicOption build() {
            return new BasicOption(names, processor, parameters, description, argCount, kind);
        }

        /**
         * Sets argument count to one and assigns the parameters string.
         * @param metavar the parameters description, same as the arg to {@link #parameters(String)}
         * @return this builder instance
         */
        public Builder arg(String metavar) {
            argCount(1);
            parameters(metavar);
            return this;
        }
    }

    public static Builder builder(String name, Processor processor) {
        return new Builder(name, processor);
    }
}

