package io.github.mike10004.configdoclet;

import jdk.javadoc.doclet.Doclet;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CliOptionageTest {

    @Test
    public void standard_noConflicts() {
        List<? extends Doclet.Option> options = new ArrayList<>(CliOptionage.standard().getSupportedOptions());
        List<Pair<Doclet.Option, Doclet.Option>> conflicts = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Doclet.Option opt1 = options.get(i);
            for (int j = i + 1; j < options.size(); j++) {
                Doclet.Option opt2 = options.get(j);
                if (BasicOption.isConflicting(opt1, opt2)) {
                    conflicts.add(Pair.of(opt1, opt2));
                }
            }
        }
        conflicts.forEach(conflict -> {
            Doclet.Option a = conflict.getLeft(), b = conflict.getRight();
            System.out.format("%s (%s) clashes with %s (%s)%n", a.getNames(), a.getDescription(), b.getNames(), b.getDescription());
        });
        assertEquals("conflicts", Collections.emptyList(), conflicts);
    }

}