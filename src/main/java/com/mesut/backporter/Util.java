package com.mesut.backporter;

public class Util {
    public static String trimPrefix(String str, String prefix) {
        if (str.startsWith(prefix)) {
            return str.substring(prefix.length());
        }
        return str;
    }


}
