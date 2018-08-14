package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static java.util.Objects.requireNonNull;

class PropertiesOutputFormatter implements OutputFormatter {

    public enum AssignationMode {
        auto,
        always,
        never;

        public static final AssignationMode DEFAULT = auto;

        public static AssignationMode parse(@Nullable String token) {
            if (token == null || token.isEmpty()) {
                return DEFAULT;
            }
            return valueOf(token.toLowerCase());
        }

        public boolean includeAssignment(String unescapedValue) {
            // TODO in auto mode, decide whether assignation is to be commented based on the unescaped value, e.g. comment only for empty values
            switch (this) {
                case always:
                    return true;
                case auto:
                case never:
                    return false;
                default:
                    throw new IllegalStateException("not handled: " + this);
            }
        }
    }

    private final String header;
    private final String bottom;
    private final AssignationMode assignationMode;

    PropertiesOutputFormatter() {
        this("", "", AssignationMode.DEFAULT);
    }

    public PropertiesOutputFormatter(String header, String bottom, AssignationMode assignationMode) {
        this.header = requireNonNull(header);
        this.bottom = requireNonNull(bottom);
        this.assignationMode = requireNonNull(assignationMode);
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
        return String.format(" Example: %s", example.value); // TODO include example value description
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
        if (assignationMode.includeAssignment(unescapedValue)) {
            out.format("%s = %s%n", key, escapedValue);
        } else {
            StringEscaping.writePropertyComment(String.format("%s = %s", key, escapedValue), out);
        }
    }

}
