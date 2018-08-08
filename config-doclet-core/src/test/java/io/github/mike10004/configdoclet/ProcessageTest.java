package io.github.mike10004.configdoclet;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class ProcessageTest {

    @Test
    public void accept_gnuWithEqualsAndValue() {
        CliOptionage.Processage p = CliOptionage.Processage.fromMap(new HashMap<>());
        boolean ret = p.accept("--output-format=json", Collections.singletonList("json"));
        assertTrue(ret);
        List<String> params = p.get("--output-format");
        assertNotNull("params", params);
        assertEquals("params values", Collections.singletonList("json"), params);
    }
}