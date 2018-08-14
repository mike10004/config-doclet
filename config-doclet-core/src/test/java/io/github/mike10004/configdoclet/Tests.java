package io.github.mike10004.configdoclet;

import io.github.mike10004.configdoclet.tests.TestConfig;

public class Tests {

    private Tests() {}

    public static TestConfig config() {
        return TestConfig.getInstance(Tests.class);
    }

}
