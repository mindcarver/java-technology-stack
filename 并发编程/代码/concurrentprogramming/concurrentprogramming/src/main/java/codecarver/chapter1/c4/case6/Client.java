package codecarver.chapter1.c4.case6;

public class Client {
    public static void main(String[] args) {
        ThreeLockDemo threeLockDemo = new ThreeLockDemo();
        new Thread("T1") {
            @Override
            public void run() {
                threeLockDemo.m1();
            }
        }.start();

        new Thread("T2") {
            @Override
            public void run() {
                threeLockDemo.m2();
            }
        }.start();

        new Thread("T3") {
            @Override
            public void run() {
                threeLockDemo.m3();
            }
        }.start();
    }
}
