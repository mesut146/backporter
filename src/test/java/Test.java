import com.mesut.backporter.Porter;

public class Test {
    @org.junit.Test
    public void name() {
        String src = "/media/mesut/SSD-DATA/IdeaProjects/jls/src/main/java";
        String dest = "/home/mesut/Desktop/port";

        Porter porter = new Porter(src, dest);
        porter.port();
    }
}
