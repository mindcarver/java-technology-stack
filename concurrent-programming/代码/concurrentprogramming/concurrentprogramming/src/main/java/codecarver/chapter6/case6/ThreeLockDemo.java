package codecarver.chapter6.case6;

public class ThreeLockDemo {

    private final Object LOCK = new Object();

    public void m1() {
        synchronized (this){
            System.out.println("m1 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static void m2() {
        synchronized (ThreeLockDemo.class){
            System.out.println("m2 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public  void m3() {
        synchronized (LOCK){
            System.out.println("m3 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
