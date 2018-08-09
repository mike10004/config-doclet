package io.github.mike10004.configdoclet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

class PropertiesOutputFormatter implements OutputFormatter {

    private static final String COMMENT_CHAR = "#";

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
        String value = "";
        if (item.defaultValue != null) {
            value = item.defaultValue;
        } else if (!item.exampleValues.isEmpty()) {
            value = item.exampleValues.get(0).value;
        }
        return value;
    }

    @SuppressWarnings("RedundantThrows")
    void format(ConfigSetting item, PrintWriter out) throws IOException {
        if (item.description != null) {
            out.format("%s %s%n", COMMENT_CHAR, item.description);
        }
        String value = getAssignedValue(item);
        out.format("%s%s = %s%n", COMMENT_CHAR, item.key, value);
    }
}
