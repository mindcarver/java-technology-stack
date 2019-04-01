package codecarver.chapter5;

import java.util.concurrent.locks.ReentrantLock;

public class ReeterLock implements Runnable {
    public static ReentrantLock lock = new ReentrantLock();
    public static int i = 0;

    public void run() {
        for (int j = 0; j < 10000; j++) {
            lock.lock();
            try{
                i++;
            }finally {
                lock.unlock();
            }

        }
    }

    public static void main(String[] args) throws InterruptedException {
        ReeterLock t = new ReeterLock();
        Thread t1 = new Thread(t);
        Thread t2 = new Thread(t);
        t1.start();
        t2.start();
        Thread.sleep(2000);
        /*t1.join();
        t2.join();*/
        System.out.println(i);
    }
}
