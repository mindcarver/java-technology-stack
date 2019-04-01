package codecarver.chapter16;

import java.util.concurrent.locks.ReentrantLock;

/**
 * reenterLock constructor fair
 */
public class FairLock implements Runnable {

    public static ReentrantLock fairLock = new ReentrantLock(true);

    @Override
    public void run() {
        while (true) {
            try {
                fairLock.lock();
                // unfairLock.lock();
                System.out.println(Thread.currentThread().getName() + "println str");
            } finally {
                fairLock.unlock();
                // unfairLock.unlock();
            }
        }
    }


    public static void main(String args[]) {
        FairLock r1 = new FairLock();
        Thread thread1 = new Thread(r1, "Thread_t1");
        Thread thread2 = new Thread(r1, "Thread_t2");
        Thread thread3 = new Thread(r1, "Thread_t3");

        thread1.start();
        thread2.start();
        thread3.start();
    }

}
