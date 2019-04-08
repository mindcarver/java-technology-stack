package codecarver.chapter1.c4.case5;


public class SynchronizedObjectLock {

    private final Object LOCK = new Object();

    public  void m1() {
        synchronized (LOCK){
            System.out.println("m1 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
    public void m2() {
        synchronized (LOCK){
            System.out.println("m2 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
