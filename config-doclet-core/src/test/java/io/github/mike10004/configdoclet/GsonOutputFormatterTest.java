package io.github.mike10004.configdoclet;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class GsonOutputFormatterTest {

    @Test
    public void toJson() throws Exception {
        Map<String, Integer> m = ImmutableMap.of("a", 1, "b", 2);
        String json = GsonOutputFormatter.toJson(m);
        String expected = new GsonBuilder().setPrettyPrinting().create().toJson(m);
        assertEquals("json", expected, json);
    }
}