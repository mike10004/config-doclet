package io.github.mike10004.configdoclet.tests;

import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class TestConfig {

    private static final LoadingCache<URL, TestConfig> cache = CacheBuilder.newBuilder()
            .build(new CacheLoader<>() {
                @Override
                public TestConfig load(@SuppressWarnings("NullableProblems") URL resource) {
                    return new TestConfig(Resources.asCharSource(resource, StandardCharsets.UTF_8));
                }
            });

    private final Supplier<ImmutableMap<String, String>> configSupplier;

    private TestConfig(CharSource testConfigSource) {
        requireNonNull(testConfigSource);
        configSupplier = Suppliers.memoize(() -> {
            Properties p = new Properties();
            try (Reader in = testConfigSource.openStream()) {
                p.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return Maps.fromProperties(p);
        });
    }

    private static final String DEFAULT_RESOURCE_PATH = "/test-config.properties";

    public static TestConfig getInstance(Class<?> yourProjectClass) {
        return getInstance(yourProjectClass, DEFAULT_RESOURCE_PATH);
    }

    public static TestConfig getInstance(Class<?> yourProjectClass, String resourcePath) {
        URL resource = yourProjectClass.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalArgumentException("not found: classpath:" + resourcePath);
        }
        return cache.getUnchecked(resource);
    }

    public ImmutableMap<String, String> get() {
        return configSupplier.get();
    }

    public File getBuildDir() {
        return new File(get("project.build.directory"));
    }

    public String get(String propertyName) {
        return get().get(propertyName);
    }

}
