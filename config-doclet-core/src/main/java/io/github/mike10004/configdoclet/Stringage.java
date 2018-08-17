package io.github.mike10004.configdoclet;

class Stringage {

    private Stringage() {}

    public static String trimLeadingFrom(String original, String ugly) {
        while (original.startsWith(ugly)) {
            original = original.substring(ugly.length());
        }
        return original;
    }

    static String trimLeadingFrom(String str, char ch) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ch) {
                return str.substring(i);
            }
        }
        return "";
    }
}
