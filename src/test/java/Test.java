import com.mesut.backporter.Porter;

public class Test {
    @org.junit.Test
    public void name() {
        String src1 = "/media/mesut/SSD-DATA/IdeaProjects/jls/src/main/java";
        String src = src1;
        src = "/home/mesut/Desktop/IdeaProjects/backporter/src/test/resources";
        String dest = "/home/mesut/Desktop/port/src/main/java";

        Porter porter = new Porter(src, dest);
        porter.addClasspath(src1);
        porter.port();
    }
}
