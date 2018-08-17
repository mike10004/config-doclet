package io.github.mike10004.configdoclet;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Collections;
import java.util.function.Function;

import static org.junit.Assert.*;

public class PropertyOptionageTest {

    @Test
    public void getOptionString() {
        Function<String, String> sysprops = Functions.forMap(
                ImmutableMap.<String, String>builder()
                        .put("foo", "bar")
                        .put("hello.foo", "baz")
                        .put("hello.gaw", "")
                .build()
                , null);
        PropertyOptionage opt = new PropertyOptionage(sysprops, "hello.");
        assertEquals("getOptionStrings present option", Collections.singletonList("baz"), opt.getOptionStrings("-foo"));
        assertNull("getOptionStrings absent option", opt.getOptionStrings("-gee"));
        assertEquals("getOptionStrings present empty-value option", Collections.singletonList(""), opt.getOptionStrings("-gaw"));
        assertTrue("isPresent present option", opt.isPresent("-gaw"));
        assertFalse("isPresent absent option", opt.isPresent("-hen"));
    }

}