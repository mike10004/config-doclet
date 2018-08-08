package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

class BasicOption implements Doclet.Option, Comparable<BasicOption> {

    private final String[] names;
    private final Processor processor;
    private final String parameters;
    private final String description;
    private final int argCount;

//    public BasicOption(String name, int argCount) {
//        this.names = name.trim().split("\\s+");
//        String desc =
//        if (desc.isEmpty()) {
//            this.description = "<MISSING KEY>";
//            this.parameters = "<MISSING KEY>";
//        } else {
//            this.description = desc;
//            this.parameters = getOptionsMessage(resources, keyBase + ".parameters");
//        }
//        this.argCount = argCount;
//    }


    public BasicOption(String[] names, Processor processor, String parameters, String description, int argCount) {
        this.processor = requireNonNull(processor, "processor");
        this.names = names;
        requireNonNull(names, "names");
        if (names.length < 1) {
            throw new IllegalArgumentException("names array must be nonempty");
        }
        this.parameters = parameters;
        this.description = requireNonNull(description, "description");
        this.argCount = argCount;
        if (argCount < 0) {
            throw new IllegalArgumentException("argcount >= 0 is required: " + argCount);
        }
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
    public Doclet.Option.Kind getKind() {
        return Doclet.Option.Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return Arrays.asList(names);
    }

    @Override
    public String getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return Arrays.toString(names);
    }

    @Override
    public int getArgumentCount() {
        return argCount;
    }

    @SuppressWarnings("SameParameterValue")
    private static String trimLeading(String original, String ugly) {
        while (original.startsWith(ugly)) {
            original = original.substring(ugly.length());
        }
        return original;
    }

    public static boolean isConflicting(Doclet.Option me, Doclet.Option option) {
        for (String myName : me.getNames()) {
            myName = trimLeading(myName, "-");
            for (String otherName : option.getNames()) {
                otherName = trimLeading(otherName, "-");
                if (myName.equalsIgnoreCase(otherName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int compareTo(BasicOption that) {
        return this.getNames().get(0).compareTo(that.getNames().get(0));
    }

    public interface Processor {
        boolean process(String option, List<String> arguments);
    }

    public static class Builder {
        private final List<String> names;
        private final Processor processor;
        private String parameters;
        private String description;
        private int argCount;

        public Builder(String name, Processor processor) {
            requireNonNull(name, "name");
            this.names = new ArrayList<>();
            this.names.add(name);
            this.processor = requireNonNull(processor, "processor");
            parameters = null;
            description = "set " + name;
        }

        public Builder alias(String name) {
            requireNonNull(name);
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
            String[] namesArray = names.toArray(new String[0]);
            return new BasicOption(namesArray, processor, parameters, description, argCount);
        }

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

