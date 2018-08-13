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

    @SuppressWarnings("RedundantThrows")
    void format(ConfigSetting item, PrintWriter out) throws IOException {
        if (item.description != null) {
            StringEscaping.writePropertyComment(item.description, out);
        }
        String value = getAssignedValue(item);
        String key = StringEscaping.escapePropertyKey(item.key);
        value = StringEscaping.escapePropertyValue(value);
        StringEscaping.writePropertyComment(String.format("%s = %s", key, value), out);
    }

}
