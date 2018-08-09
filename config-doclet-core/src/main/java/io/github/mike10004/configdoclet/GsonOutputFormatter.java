package io.github.mike10004.configdoclet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

class GsonOutputFormatter implements OutputFormatter {

    @Override
    public void format(List<ConfigSetting> items, PrintWriter out) throws IOException {
        try {
            String json = toJson(items);
            out.println(json);
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }

    static String toJson(Object object) throws ReflectiveOperationException {
        Class<?> builderClass = Class.forName("com.google.gson.GsonBuilder");
        Object builder = Reflections.newInstanceNoArgs(builderClass);
        builder = Reflections.invokeNoArgs(builder, "setPrettyPrinting");
        Object gson = Reflections.invokeNoArgs(builder, "create");
        String json = Reflections.invokeOneArg(gson, "toJson", Object.class, object);
        return json;
    }
}
