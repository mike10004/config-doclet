package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author the Guava authors
 */
@SuppressWarnings("UnusedReturnValue")
class ToStringHelper {

    private final String objectClass;
    private final List<Object[]> properties;

    public ToStringHelper(Object theObject) {
        this(getSimpleClassname(theObject));
    }

    public ToStringHelper(Class<?> objectClass) {
        this(getSimpleClassname(objectClass));
    }

    public ToStringHelper(String objectClass) {
        this.objectClass = objectClass;
        properties = new ArrayList<>();
    }

    public ToStringHelper add(String name, Object value) {
        properties.add(new Object[]{name, value});
        return this;
    }

    private static String getSimpleClassname(Object object) {
        if (object == null) {
            return "null";
        }
        return getSimpleClassname(object.getClass());
    }

    private static boolean contains(char[] chars, char ch) {
        for (char aChar : chars) {
            if (aChar == ch) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllCharsFromArray(String str, char[] chars) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!contains(chars, ch)) {
                return false;
            }
        }
        return true;
    }

    private static final char[] UNINFORMATIVE_CHARS = {'0', '1', '2', '3', '4' , '5', '6', '7', '8', '9', '$'};

    private static String getSimpleClassname(@Nullable Class<?> clazz) {
        if (clazz == null) {
            return "null";
        }
        String simple = clazz.getSimpleName();
        if (simple.isEmpty() || isAllCharsFromArray(simple, UNINFORMATIVE_CHARS)) {
            return clazz.getName();
        }
        return simple;
    }

    @Override
    public String toString() {
        Object[][] properties = this.properties.toArray(new Object[0][]);
        StringBuilder b = new StringBuilder(objectClass.length() + 2 + properties.length * 4);
        b.append(objectClass).append("{");
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
