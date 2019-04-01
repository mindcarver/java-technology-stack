package codecarver.jdkConcurrentPackage.reentrantlock.case1;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @ClassName Test
 * @Description TODO
 * @Author lenovo
 * @Date 2019/1/18 17:31
 **/
public class Test implements Runnable {
    public static  ReentrantLock lock = new ReentrantLock();
    public static int i = 0;

    @Override
    public void run() {
        for (int j = 0; j < 1000000; j++) {
            lock.lock();

            try {
                i++;
            } finally {
                // 手动释放锁
                lock.unlock();
                lock.unlock();

            }

        }
    }

    public static void main(String args[]) throws InterruptedException {
        Test reenterLock = new Test();
        Thread t1 = new Thread(reenterLock);
        Thread t2 = new Thread(reenterLock);

        t1.start();
        t2.start();

        // 阻塞main线程，等到t1和t2全部执行完
        t1.join();
        t2.join();

        System.out.println(i);
    }
}
