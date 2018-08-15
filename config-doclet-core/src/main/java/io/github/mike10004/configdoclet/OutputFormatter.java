package io.github.mike10004.configdoclet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

interface OutputFormatter {

    void format(List<ConfigSetting> items, PrintWriter out) throws IOException;

    interface Factory {

        boolean isSpecifiedByFormatCode(String formatOptionParameterValue);

        OutputFormatter produce(Optionage optionage);

        static Factory forCode(String code, Function<? super Optionage, OutputFormatter> transform) {
            requireNonNull(code);
            requireNonNull(transform);
            return new Factory() {
                @Override
                public boolean isSpecifiedByFormatCode(String formatOptionParameterValue) {
                    return code.equalsIgnoreCase(formatOptionParameterValue);
                }

                @Override
                public OutputFormatter produce(Optionage optionage) {
                    return transform.apply(optionage);
                }
            };
        }
    }
}
