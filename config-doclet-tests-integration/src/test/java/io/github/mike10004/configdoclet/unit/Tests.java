package io.github.mike10004.configdoclet.unit;

public class Tests {

    private Tests() {}

    public static TestConfig config() {
        return TestConfig.getInstance(Tests.class);
    }
}
