package io.github.mike10004.configdoclet;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

public class Tests {

    private static final Supplier<ImmutableMap<String, String>> configSupplier = Suppliers.memoize(() -> {
        Properties p = new Properties();
        try (InputStream in = Tests.class.getResourceAsStream("/test-config.properties")) {
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Maps.fromProperties(p);
    });

    public static ImmutableMap<String, String> config() {
        return configSupplier.get();
    }

    public static File getBuildDir() {
        return new File(config().get("project.build.directory"));
    }



}
