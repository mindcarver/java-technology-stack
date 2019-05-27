package codecarver.chapter1.c4.case5;

public class Client {
    public static void main(String[] args) {
        SynchronizedObjectLock lock = new SynchronizedObjectLock();
        new Thread("T1") {
            @Override
            public void run() {
                lock.m1();
            }
        }.start();

        new Thread("T2") {
            @Override
            public void run() {
                lock.m2();
            }
        }.start();
    }
}

