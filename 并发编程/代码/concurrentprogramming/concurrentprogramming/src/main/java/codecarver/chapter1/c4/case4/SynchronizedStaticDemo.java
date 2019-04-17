package codecarver.chapter1.c4.case4;

public class SynchronizedStaticDemo {
    static {
        synchronized (SynchronizedStaticDemo.class){
            System.out.println("静态代码块" + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static void m1() {
        System.out.println("m1 " + Thread.currentThread().getName());
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void m2() {
        synchronized (SynchronizedStaticDemo.class){
            System.out.println("m2 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
