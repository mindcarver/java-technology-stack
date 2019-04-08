package codecarver.chapter2.reentrantlock.case3;

import java.util.concurrent.locks.ReentrantLock;

public class TryLockSolveDeadLock implements Runnable {
    public static ReentrantLock lock1 = new ReentrantLock();
    public static ReentrantLock lock2 = new ReentrantLock();
    boolean flag;

    public TryLockSolveDeadLock(boolean flag) {
        this.flag = flag;
    }

    @Override
    public void run() {
        if (flag) {
            while (true) {
                if (lock1.tryLock()) {
                    try {
                        Thread.sleep(100);
                        if (lock2.tryLock()) {
                            try {
                                System.out.println(Thread.currentThread().getName() + ":进入锁2，完成任务");
                                return;
                            } finally {
                                lock2.unlock();
                            }
                        }
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    finally {
                        lock1.unlock();
                    }
                }
            }
        } else {
            while (true) {
                if (lock2.tryLock()) {
                    try {
                        Thread.sleep(100);
                        if (lock1.tryLock()) {
                            try {
                                System.out.println(Thread.currentThread().getName() + ":进入锁1，完成任务");
                                return;
                            } finally {
                                lock1.unlock();
                            }
                        }
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    finally {
                        lock2.unlock();
                    }
                }
            }

        }
    }

    public static void main(String args[]) {
        TryLockSolveDeadLock r1 = new TryLockSolveDeadLock(true);
        TryLockSolveDeadLock r2 = new TryLockSolveDeadLock(false);
        Thread thread1 = new Thread(r1,"Thread-1");
        Thread thread2 = new Thread(r2, "Thread-2");

        thread1.start();
        thread2.start();
    }

}
