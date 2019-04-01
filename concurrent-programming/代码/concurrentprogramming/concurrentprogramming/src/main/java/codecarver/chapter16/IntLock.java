package codecarver.chapter16;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 重入锁配合中断响应解决死锁问题
 */
public class IntLock implements Runnable {
    public static ReentrantLock lock1 = new ReentrantLock();
    public static ReentrantLock lock2 = new ReentrantLock();
    int lock;

    public IntLock(int lock) {
        this.lock = lock;
    }

    @Override
    public void run() {
        try {
            if (lock == 1) {
                lock1.lockInterruptibly();
                Thread.sleep(500);
                lock2.lockInterruptibly();
            } else {
                lock2.lockInterruptibly();
                Thread.sleep(500);
                lock1.lockInterruptibly();
            }
            System.out.println(Thread.currentThread().getId() + "线程成功执行try里全部内容");
        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getId() + "线程直接从这里出来，不会继续try块的内容了");
            e.printStackTrace();
        } finally {
            if (lock1.isHeldByCurrentThread()) {
                lock1.unlock();
            }
            if (lock2.isHeldByCurrentThread()) {
                lock2.unlock();
            }
            System.out.println(Thread.currentThread().getId() + "线程退出");
        }

    }


    public static void main(String args[]) throws InterruptedException {
        IntLock r1 = new IntLock(1);
        IntLock r2 = new IntLock(2);

        Thread thread1 = new Thread(r1);
        Thread thread2 = new Thread(r2);

        thread1.start();
        thread2.start();

        Thread.sleep(1000);
        // 中断t2
        thread2.interrupt();

    }
}
