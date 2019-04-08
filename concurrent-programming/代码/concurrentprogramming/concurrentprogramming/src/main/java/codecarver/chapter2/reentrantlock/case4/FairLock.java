package codecarver.chapter2.reentrantlock.case4;

import java.util.concurrent.locks.ReentrantLock;

public class FairLock implements Runnable {

    public static ReentrantLock fairLock = new ReentrantLock(true);
    @Override
    public void run() {
        while (true) {
            try {
                fairLock.lock();
                System.out.println(Thread.currentThread().getName() + "得到锁");
            } finally {
                fairLock.unlock();
            }
        }
    }

    public static void main(String args[]) {
        FairLock r1 = new FairLock();
        Thread thread1 = new Thread(r1, "Thread_t1");
        Thread thread2 = new Thread(r1, "Thread_t2");
        /*Thread thread3 = new Thread(r1, "Thread_t3");
*/
        thread1.start();
        thread2.start();
        /*thread3.start();*/
    }

}
