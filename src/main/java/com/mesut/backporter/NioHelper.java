package com.mesut.backporter;

public class NioHelper {

    static String pkg = "utils";

    public static String replace(String str) {
        str = str.replaceAll("import java\\.nio\\.file\\.Path;", "import " + pkg + ".Path;");
        str = str.replaceAll("import java\\.nio\\.file\\.Paths;", "import " + pkg + ".Paths;");
        str = str.replaceAll("import java\\.nio\\.file\\.Files;", "import " + pkg + ".Files;");
        return str;
    }


}
