package io.github.mike10004.configdoclet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static java.util.Objects.requireNonNull;

class GsonOutputFormatter implements OutputFormatter {

    private final Gson gson;

    public GsonOutputFormatter() {
        this(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create());
    }

    public GsonOutputFormatter(Gson gson) {
        this.gson = requireNonNull(gson);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void format(List<ConfigSetting> items, PrintWriter out) throws IOException {
        gson.toJson(items, out);
    }

}
