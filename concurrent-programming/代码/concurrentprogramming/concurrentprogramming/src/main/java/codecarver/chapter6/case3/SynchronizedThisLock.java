package codecarver.chapter6.case3;

public class SynchronizedThisLock {
    public static void main(String[] args) {

        ThisLock thisLock = new ThisLock();
        new Thread("Thread-T1") {
            @Override
            public void run() {
                thisLock.m1();
            }
        }.start();

        new Thread("Thread-T2") {
            @Override
            public void run() {
                thisLock.m2();
            }
        }.start();


    }
}

class ThisLock {
    public synchronized void m1() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void m2() {
        synchronized (this) {
            try {
                System.out.println(Thread.currentThread().getName());
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

