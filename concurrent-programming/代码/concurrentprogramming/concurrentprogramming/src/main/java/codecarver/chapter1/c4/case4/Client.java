package codecarver.chapter1.c4.case4;

public class Client {
    public static void main(String[] args) {
        new Thread("T1") {
            @Override
            public void run() {
                SynchronizedStaticDemo.m1();
            }
        }.start();

        new Thread("T2") {
            @Override
            public void run() {
                SynchronizedStaticDemo.m2();
            }
        }.start();

    }
}
