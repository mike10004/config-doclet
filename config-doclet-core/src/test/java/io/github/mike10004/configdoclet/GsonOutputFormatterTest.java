package io.github.mike10004.configdoclet;

import com.google.gson.Gson;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GsonOutputFormatterTest {

    @Test
    public void format() throws Exception {
        List<ConfigSetting> settings = List.of(
                ConfigSetting.builder("a").exampleValue("b").build(),
                ConfigSetting.builder("c").description("d").build()
        );
        StringWriter sw = new StringWriter();
        try (PrintWriter out = new PrintWriter(sw)) {
            new GsonOutputFormatter().format(settings, out);
        }
        ConfigSetting[] deserialized = new Gson().fromJson(sw.toString(), ConfigSetting[].class);
        assertEquals("deserialized", settings, Arrays.asList(deserialized));
    }
}