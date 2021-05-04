package com.mesut.backporter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageRenamer {
    static Map<String, String> map = new HashMap<>();

    public static void add(String from, String to) {
        map.put(from, to);
    }

    static String mapPkg(String pkg) {
        String[] all = pkg.split("\\.");
        return map.get(all[0]) + pkg.substring(all[0].length());
    }

    public void rename(String str) {
        Pattern pattern = Pattern.compile("package (.*);");
        Matcher matcher = pattern.matcher(str);
    }
}
