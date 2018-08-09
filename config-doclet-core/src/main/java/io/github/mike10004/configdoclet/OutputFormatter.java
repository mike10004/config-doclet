package io.github.mike10004.configdoclet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

interface OutputFormatter {
    void format(List<ConfigSetting> items, PrintWriter out) throws IOException;
}
