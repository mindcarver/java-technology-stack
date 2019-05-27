package codecarver.chapter2.reentrantlock.case3;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


public class TimeLock implements Runnable {
    public static ReentrantLock lock = new ReentrantLock();

    @Override
    public void run() {
        try {
            if (lock.tryLock(2, TimeUnit.SECONDS)) {
                System.out.println(Thread.currentThread().getName()+":成功获取锁");
                // 让第一个进来的线程再睡4秒，那么第二个线程则需要等至少4秒
                // 第二个线程必然失败
                Thread.sleep(4000);
            } else {
                System.out.println(Thread.currentThread().getName()+"：获取锁失败");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public static void main(String args[]) {
        TimeLock timeLock = new TimeLock();
        Thread t1 = new Thread(timeLock,"Thread-1");
        Thread t2 = new Thread(timeLock,"Thread-2");

        t1.start();
        t2.start();
    }
}
