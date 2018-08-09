package io.github.mike10004.configdoclet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class ConfigSetting {

    public final String key;
    public final String description;
    public final String defaultValue;
    public final List<ExampleValue> exampleValues;

    private ConfigSetting(Builder builder) {
        key = requireNonNull(builder.key, "key");
        description = builder.description;
        defaultValue = builder.defaultValue;
        exampleValues = Collections.unmodifiableList(requireNonNull(builder.exampleValues));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigSetting)) return false;
        ConfigSetting modelItem = (ConfigSetting) o;
        return Objects.equals(key, modelItem.key) &&
                Objects.equals(description, modelItem.description) &&
                Objects.equals(defaultValue, modelItem.defaultValue) &&
                Objects.equals(exampleValues, modelItem.exampleValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, description, defaultValue, exampleValues);
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {

        private String key;
        private String description;
        private String defaultValue;

        private final List<ExampleValue> exampleValues;

        private Builder() {
            exampleValues = new ArrayList<>();
        }

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder description(String val) {
            description = val;
            return this;
        }

        public Builder defaultValue(String val) {
            defaultValue = val;
            return this;
        }

        public Builder exampleValue(String val) {
            return exampleValue(new ExampleValue(val, null));
        }

        public Builder exampleValue(ExampleValue val) {
            exampleValues.add(val);
            return this;
        }

        public ConfigSetting build() {
            return new ConfigSetting(this);
        }
    }

    static final class ExampleValue {
        public final String value;
        public final String description;

        public ExampleValue(String value, String description) {
            this.value = requireNonNull(value);
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExampleValue)) return false;
            ExampleValue that = (ExampleValue) o;
            return Objects.equals(value, that.value) &&
                    Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {

            return Objects.hash(value, description);
        }
    }

    @Override
    public String toString() {
        return "ConfigSetting{" +
                "key='" + key + '\'' +
                ", description='" + description + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", exampleValues.size=" + exampleValues.size() +
                '}';
    }

    public static Comparator<ConfigSetting> comparatorByKey() {
        return Comparator.comparing(setting -> setting.key);
    }
}
