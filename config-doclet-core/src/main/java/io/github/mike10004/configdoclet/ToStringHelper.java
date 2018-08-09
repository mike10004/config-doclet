package io.github.mike10004.configdoclet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author the Guava authors
 */
@SuppressWarnings("UnusedReturnValue")
class ToStringHelper {

    private final String itemClass;
    private final List<Object[]> properties;

    public ToStringHelper(Object theObject) {
        this(theObject == null ? "null" : theObject.getClass().getName());
    }

    public ToStringHelper(Class<?> itemClass) {
        this(itemClass == null ? "null" : itemClass.getSimpleName());
    }

    public ToStringHelper(String itemClass) {
        this.itemClass = itemClass;
        properties = new ArrayList<>();
    }

    public ToStringHelper add(String name, Object value) {
        properties.add(new Object[]{name, value});
        return this;
    }

    @Override
    public String toString() {
        Object[][] properties = this.properties.toArray(new Object[0][]);
        StringBuilder b = new StringBuilder(itemClass.length() + 2 + properties.length * 4);
        b.append(itemClass).append("{");
        for (int i = 0; i < properties.length; i++) {
            if (i > 0) {
                b.append(",");
            }
            Object[] property = properties[i];
            String name = (String) property[0];
            Object value = property[1];
            b.append(name);
            b.append("=");
            b.append(value);
        }
        b.append('}');
        return b.toString();
    }
}
