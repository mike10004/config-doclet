package io.github.mike10004.configdoclet;

import com.google.common.io.CharSource;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class PropertiesOutputFormatterTest {

    @Test
    public void multilineDescription() throws Exception {
        ConfigSetting setting = ConfigSetting.builder("app.multilineDescription")
                .description("This description\n has multiple\n lines")
                .defaultValue("A\tcomplex\nvalue")
                .build();
        StringWriter sw = new StringWriter();
        PropertiesOutputFormatter formatter = new PropertiesOutputFormatter();
        formatter.format(setting, new PrintWriter(sw));
        String result = sw.toString();
        System.out.println(result);
        List<String> lines = CharSource.wrap(result).readLines();
        List<String> violations = new ArrayList<>();
        lines.forEach(line -> {
            if (!line.trim().isEmpty()) {
                if (!line.startsWith(StringEscaping.getPropertyCommentPrefix())) {
                    violations.add(line);
                }
            }
        });
        assertEquals("expect all lines to start with comment char", Collections.emptyList(), violations);
    }

    @Test
    public void doNotRepeatExampleWhenNoDefaultIsSpecified() throws Exception {
        ByteBucket bucket = new ByteBucket(1024);
        ConfigSetting setting = ConfigSetting.builder("app.something")
                .description("Something")
                .exampleValue("1")
                .exampleValue("2")
                .build();
        new PropertiesOutputFormatter().format(setting, bucket.printWriter(UTF_8));
        String output = bucket.dump(UTF_8);
        System.out.println(output);
        List<String> lines = CharSource.wrap(output).readLines();
        long exampleCount = lines.stream().filter(line -> line.contains("Example: ")).count();
        assertEquals("num examples", 1, exampleCount);
    }

}