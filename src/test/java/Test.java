import com.mesut.backporter.PackageRenamer;
import com.mesut.backporter.Porter;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

public class Test {

    @org.junit.Test
    public void port() {
        String src;
        String dest;
        //src = "/media/mesut/SSD-DATA/IdeaProjects/jls/src/main/java";

        src = "/home/mesut/Desktop/IdeaProjects/javac13/src/main/java";
        dest = "/home/mesut/Desktop/IdeaProjects/javac13/conv";
        PackageRenamer.add("java", "java0");
        PackageRenamer.add("javax", "javax0");
        PackageRenamer.add("com", "com0");
        PackageRenamer.add("jdk", "jdk0");
        PackageRenamer.add("sun", "sun0");

        //src = "/home/mesut/Desktop/IdeaProjects/backporter/src/test/resources";
        //dest = "/home/mesut/Desktop/port/src/main/java";


        Porter porter = new Porter(src, dest);
        //porter.addClasspath("/media/mesut/SSD-DATA/IdeaProjects/jls/dist/classpath/gson-2.8.5.jar");
        //porter.addClasspath("/media/mesut/SSD-DATA/IdeaProjects/jls/dist/classpath/protobuf-java-3.9.1.jar");
        porter.port();
    }


}

