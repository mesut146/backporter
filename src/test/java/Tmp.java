import java.util.function.IntFunction;

public class Tmp {
}

class a {

    public a(int x) {
        System.out.println("cons " + x);
    }

    static boolean isEven(int a) {
        return a % 2 == 0;
    }

    public int sum(int a, int b) {
        return a + b;
    }

    public void sum(int a, int b, int c) {
        System.out.println(a + b + c);
    }

    public void ins() {
        i1 ob = this::sum;
        System.out.println(ob.s(5, 6));
    }

    public void ins2() {
        i3 ob = this::sum;
        ob.a(1, 2, 3);
    }

    void cons() {
        i4 o = a::new;
        o = new i4() {
            public a get(int x) {
                return new a(x);
            }
        };

        o.get(5);
    }

    void consArr() {
        IntFunction<a[]> arr = a[]::new;
    }

    void sttc() {
        i2 o = a::isEven;
        System.out.println(o.a(5));
    }

    interface i1 {
        int s(int a, int b);
    }

    interface i3 {
        void a(int a, int b, int c);
    }

    interface i2 {
        boolean a(int x);
    }

    interface i4 {
        a get(int x);
    }

    interface i5 {

    }
}
