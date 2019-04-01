package codecarver.chapter4.case2;

public class SimpleWait {
    final static Object object = new Object();
    public static class T1 extends Thread{
        @Override
        public void run() {
            synchronized (object){
            System.out.println("T1线程开启");
            try {
                System.out.println("T1线程进入等待");
                object.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("T1线程继续执行");
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("T1线程结束");
        }
    }
    }

    public static class T2 extends Thread{
        @Override
        public void run() {
            synchronized (object){
                System.out.println("T2开启，通知一个线程");
                object.notify();
                try {
                    // 虽然通知了（这里指的是等待队列的任意线程）
                    // 但是模拟一下T2线程还需要经历2秒左右才能处理完
                    // 也就是说2秒后才会释放锁
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("T2结束");
            }
        }
    }

    public static void main(String[] args) {
        Thread t1 = new T1();
        Thread t2 = new T2();
        t1.start();
        t2.start();


    }
}
