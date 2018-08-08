package io.github.mike10004.configdoclet;

import io.github.mike10004.configdoclet.unit.TestConfig;

public class Tests {

    private Tests() {}

    public static TestConfig config() {
        return TestConfig.getInstance(Tests.class);
    }

}
