package io.github.mike10004.configdoclet;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import jdk.javadoc.doclet.Doclet;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class OptionageTest {

    @Test
    public void compose() {
        Function<String, String> sysprops = Functions.forMap(
                ImmutableMap.<String, String>builder()
                        .put("configdoclet.footer", "a")
                        .put("configdoclet.header", "b")
                        .put("configdoclet.booloption1", "")
                        .put("configdoclet.booloption4", "")
                        .build()
        , null);
        Optionage secondary = new PropertyOptionage(sysprops, "configdoclet.");
        Map<String, List<String>> priority = ImmutableMap.<String, List<String>>builder()
                .put("-header", singletonList("c"))
                .put("-docencoding", singletonList("d"))
                .put("-booloption2", emptyList())
                .put("-booloption4", emptyList())
                .build();
        Optionage composed = Optionage.compose(new PredefinedOptionage(priority), secondary);
        assertGetEquals(composed, "defined in both", "-header", singletonList("c"));
        assertGetEquals(composed, "defined only in priority", "-docencoding", singletonList("d"));
        assertGetEquals(composed, "defined only in secondary", "-footer", singletonList("a"));
        assertGetEquals(composed, "defined in neither", "-d", null);
        assertBoolEquals(composed, "boolean in secondary", "-booloption1", true);
        assertBoolEquals(composed, "boolean in priority", "-booloption2", true);
        assertBoolEquals(composed, "boolean in neither", "-booloption3", false);
        assertBoolEquals(composed, "boolean in both", "-booloption4", true);
    }

    private static void assertBoolEquals(Optionage opt, String message, String name, boolean expected) {
        assertEquals(message, expected, opt.isPresent(name));
    }

    private static void assertGetEquals(Optionage opt, String message, String name, @Nullable List<String> expected) {
        List<String> actual = opt.getOptionStrings(name);
        assertEquals(message, expected, actual);
    }

    private static class PredefinedOptionage implements Optionage {

        private final Map<String, List<String>> content;

        public PredefinedOptionage(Map<String, List<String>> content) {
            this.content = content;
        }

        @Override
        public Set<? extends Doclet.Option> getSupportedOptions() {
            throw new UnsupportedOperationException("not supported in this unit test");
        }

        @Nullable
        @Override
        public List<String> getOptionStrings(String name) {
            return content.get(name);
        }
    }
}