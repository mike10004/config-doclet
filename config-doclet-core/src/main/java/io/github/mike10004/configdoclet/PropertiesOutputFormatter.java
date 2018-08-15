package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Reporter;

import javax.annotation.Nullable;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

class PropertiesOutputFormatter implements OutputFormatter {

    public enum AssignationHint {
        auto,
        always,
        never;

        public static final AssignationHint DEFAULT = auto;

        public static AssignationHint parse(@Nullable String token) {
            if (token == null || token.isEmpty()) {
                return DEFAULT;
            }
            return valueOf(token.toLowerCase());
        }

        public boolean includeAssignment(String unescapedValue) {
            // TODO in auto mode, decide whether assignation is to be commented based on the unescaped value;
            //      e.g. include unless value is empty
            switch (this) {
                case auto:
                    return false;
                case always:
                    return true;
                case never:
                    return false;
                default:
                    throw new IllegalStateException("not handled: " + this);
            }
        }
    }

    private final String header;
    private final String bottom;
    private final AssignationHint assignationHint;

    PropertiesOutputFormatter() {
        this("", "", AssignationHint.DEFAULT);
    }

    public PropertiesOutputFormatter(String header, String bottom, AssignationHint assignationHint) {
        this.header = requireNonNull(header);
        this.bottom = requireNonNull(bottom);
        this.assignationHint = requireNonNull(assignationHint);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void format(List<ConfigSetting> items, PrintWriter out) throws IOException {
        StringEscaping.writePropertyComment(header, out);
        for (ConfigSetting item : items) {
            format(item, out);
            out.println();
        }
        StringEscaping.writePropertyComment(bottom, out);
    }

    String getAssignedValue(ConfigSetting item) {
        String value;
        if (item.defaultValue != null) {
            value = item.defaultValue;
        } else if (!item.exampleValues.isEmpty()) {
            value = item.exampleValues.get(0).value;
        } else {
            value = "";
        }
        return value;
    }

    @SuppressWarnings("SameParameterValue")
    private static String trimLeadingFrom(String str, char ch) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ch) {
                return str.substring(i);
            }
        }
        return "";
    }

    protected String formatExample(ConfigSetting.ExampleValue example) {
        if (example.description == null || example.description.trim().isEmpty()) {
            return String.format(" Example: %s", example.value);
        } else {
            return String.format(" Example: %s (%s)", example.value, example.description);
        }
    }

    @SuppressWarnings("RedundantThrows")
    void format(ConfigSetting setting, PrintWriter out) throws IOException {
        if (setting.description != null) {
            String desc = " " + trimLeadingFrom(setting.description, ' ');
            StringEscaping.writePropertyComment(desc, out);
        }
        if (!setting.exampleValues.isEmpty()) {
            for (int i = 0; i < setting.exampleValues.size(); i++) {
                ConfigSetting.ExampleValue example = setting.exampleValues.get(i);
                if (i > 0 || setting.defaultValue != null) {
                    String exampleStr = formatExample(example);
                    StringEscaping.writePropertyComment(exampleStr, out);
                }
            }
        }
        String unescapedValue = getAssignedValue(setting);
        String key = StringEscaping.escapePropertyKey(setting.key);
        String escapedValue = StringEscaping.escapePropertyValue(unescapedValue);
        if (assignationHint.includeAssignment(unescapedValue)) {
            out.format("%s = %s%n", key, escapedValue);
        } else {
            StringEscaping.writePropertyComment(String.format("%s = %s", key, escapedValue), out);
        }
    }

    public static Factory factory(Reporter reporter) {
        return new MyFactory(reporter);
    }

    private static class MyFactory implements Factory {

        private static final Logger log = Logger.getLogger(MyFactory.class.getName());

        private final Reporter reporter;

        private MyFactory(Reporter reporter) {
            this.reporter = reporter;
        }

        @Override
        public boolean isSpecifiedByFormatCode(String formatOptionParameterValue) {
            return ConfigDoclet.OUTPUT_FORMAT_PROPERTIES.equalsIgnoreCase(formatOptionParameterValue);
        }

        @Override
        public OutputFormatter produce(Optionage optionage) {
            return new PropertiesOutputFormatter(readHeader(optionage), readFooter(optionage), getAssignationHint(optionage));
        }

        private PropertiesOutputFormatter.AssignationHint getAssignationHint(Optionage optionage) {
            String token = optionage.getOptionString(ConfigDoclet.OPT_ASSIGNATION_HINT, null);
            return PropertiesOutputFormatter.AssignationHint.parse(token);
        }

        private String readHeader(Optionage optionage) {
            @Nullable String headerSpecification = optionage.getOptionString(ConfigDoclet.OPT_HEADER, null);
            return readBookend(optionage, headerSpecification);
        }

        private String readFooter(Optionage optionage) {
            @Nullable String bottomSpecification = optionage.getOptionString(ConfigDoclet.OPT_FOOTER, null);
            return readBookend(optionage, bottomSpecification);
        }

        static final String FILE_URL_INDICATOR = "file:";

        // TODO support option to specify -header or -bottom charset
        private Charset getBookendCharset(Optionage optionage) {
            return Charset.defaultCharset();
        }

        private String readBookend(Optionage optionage, @Nullable String specification) {
            String content = "";
            if (specification != null) {
                if (specification.startsWith(FILE_URL_INDICATOR)) {
                    try {
                        File sourceFile = new File(new URI(specification));
                        byte[] bytes = java.nio.file.Files.readAllBytes(sourceFile.toPath());
                        content = new String(bytes, getBookendCharset(optionage));
                    } catch (IOException | URISyntaxException e) {
                        log.log(Level.WARNING, e, () -> "failed to read from source file " + specification);
                        reporter.print(Diagnostic.Kind.WARNING, "failed to read from header/bottom source file " + specification);
                    }
                } else {
                    content = specification;
                }
            }
            return content;
        }

    }
}
