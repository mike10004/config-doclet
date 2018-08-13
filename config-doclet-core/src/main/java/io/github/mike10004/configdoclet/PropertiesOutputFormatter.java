package io.github.mike10004.configdoclet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

class PropertiesOutputFormatter implements OutputFormatter {

    public PropertiesOutputFormatter() {
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void format(List<ConfigSetting> items, PrintWriter out) throws IOException {
        for (ConfigSetting item : items) {
            format(item, out);
            out.println();
        }
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
            for (ConfigSetting.ExampleValue example : setting.exampleValues) {
                String exampleStr = formatExample(example);
                StringEscaping.writePropertyComment(exampleStr, out);
            }
        }
        String value = getAssignedValue(setting);
        String key = StringEscaping.escapePropertyKey(setting.key);
        value = StringEscaping.escapePropertyValue(value);
        StringEscaping.writePropertyComment(String.format("%s = %s", key, value), out);
    }

}
