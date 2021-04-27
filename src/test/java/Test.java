import com.mesut.backporter.Porter;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

public class Test {

    @org.junit.Test
    public void port() {
        String src1 = "/media/mesut/SSD-DATA/IdeaProjects/jls/src/main/java";
        String src = src1;
        //src = "/home/mesut/Desktop/IdeaProjects/backporter/src/test/resources";
        String dest = "/home/mesut/Desktop/port/src/main/java";

        Porter porter = new Porter(src, dest);
        porter.addClasspath(src1);
        porter.addClasspath("/media/mesut/SSD-DATA/IdeaProjects/jls/dist/classpath/gson-2.8.5.jar");
        porter.addClasspath("/media/mesut/SSD-DATA/IdeaProjects/jls/dist/classpath/protobuf-java-3.9.1.jar");
        porter.port();
    }

    @org.junit.Test
    public void consRef() {
        List<w> l = Arrays.asList(new w(5), new w(8));
        IntFunction<w[]> f = w[]::new;
        f = new IntFunction<w[]>() {
            @Override
            public w[] apply(int i) {
                return new w[i];
            }
        };
        w[] arr = l.toArray(f);


        System.out.println(Arrays.toString(arr));
    }

    @org.junit.Test
    public void stRef() {

    }

    static class w {
        int a;

        w(int i) {
            a = i;
            System.out.println("cons " + i);
        }

        @Override
        public String toString() {
            return "w{" + a + "}";
        }

    }

}

