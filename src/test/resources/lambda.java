import java.util.function.Function;

class lambda {

    public static void main(String[] args) {
        Function<Integer, Integer> inc = (Integer a) -> a + 1;
        assert inc.apply(5) == 6;

        asd((Integer a) -> a + 1);
    }

    static void asd(Function<Integer, Integer> f) {
        assert f.apply(5) == 6;
    }

}