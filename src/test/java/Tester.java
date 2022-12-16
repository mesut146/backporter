import com.mesut.backporter.Porter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Tester {

    String dir = new File(".").getAbsolutePath();


    void call(File dir, File out) throws IOException {
        out.mkdirs();
        try (var it = Files.walk(dir.toPath())) {
            var files = it
                    .map(Path::toString)
                    .filter(s -> s.endsWith(".java"))
                    .collect(Collectors.joining(" "));
            var b = new ProcessBuilder(String.format("javac -d %s %s", out, files).split(" "));
            b.redirectErrorStream(true);
            var proc = b.start();
            var str = new String(proc.getInputStream().readAllBytes());
            System.out.println(str);
        }
    }

    @org.junit.Test
    public void internal() throws IOException {
        File src = new File(dir, "src/test/resources");
        File dest = new File(dir, "testOut");
        Porter porter = new Porter(src, dest);
        porter.port();
        call(dest, new File(dir, "testClasses"));
    }

    @org.junit.Test
    public void port() {
        String src;
        String dest;

        src = "/home/mesut/Desktop/IdeaProjects/javac13/src/main/java";
        dest = "/home/mesut/Desktop/IdeaProjects/javac13/conv";
//        PackageRenamer.add("java", "java0");
//        PackageRenamer.add("javax", "javax0");
//        PackageRenamer.add("com", "com0");
//        PackageRenamer.add("jdk", "jdk0");
//        PackageRenamer.add("sun", "sun0");

        Porter porter = new Porter(new File(src), new File(dest));
        porter.port();
    }


}

