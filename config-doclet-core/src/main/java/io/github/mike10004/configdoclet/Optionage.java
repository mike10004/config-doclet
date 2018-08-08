package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Optionage {

    Set<? extends Doclet.Option> getSupportedOptions();

    String getOptionString(String name, String defaultValue);

    static Optionage compose(Optionage priority, Optionage...lessers) {
        List<Optionage> optionages = Stream.concat(Stream.of(priority), Stream.of(lessers)).collect(Collectors.toList());
        return new Optionage() {
            @Override
            public Set<? extends Doclet.Option> getSupportedOptions() {
                return optionages.stream()
                        .flatMap(optionage -> optionage.getSupportedOptions().stream())
                        .collect(Collectors.toSet());
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
        };
    }
}
