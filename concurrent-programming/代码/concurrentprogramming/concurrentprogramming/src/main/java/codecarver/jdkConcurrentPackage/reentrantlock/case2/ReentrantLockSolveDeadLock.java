package codecarver.jdkConcurrentPackage.reentrantlock.case2;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @ClassName DeadLock
 * @Description TODO
 * @Author lenovo
 * @Date 2019/1/19 13:54
 **/
public class ReentrantLockSolveDeadLock {
    public static void main(String[] args) {
        DeadLockThread deadLock1=new DeadLockThread(true);
        DeadLockThread deadLock2=new DeadLockThread(false);
        Thread t1=new Thread(deadLock1,"Thread-1");
        Thread t2=new Thread(deadLock2,"Thread-2");
        t1.start();
        t2.start();
        t1.interrupt();
    }

}

class DeadLockThread implements Runnable{
    boolean flag;
    final static ReentrantLock LOCK1 = new ReentrantLock();
    final static ReentrantLock LOCK2 = new ReentrantLock();

    public DeadLockThread(boolean flag){
        this.flag=flag;
    }

    public void run() {
        try {
            if(flag){
                LOCK1.lockInterruptibly();
                Thread.sleep(100);
                LOCK2.lockInterruptibly();
                System.out.println("我没有死锁-1");
            }
            else{
                LOCK2.lockInterruptibly();
                Thread.sleep(100);
                LOCK1.lockInterruptibly();
                //此处类似，保持o2的锁，申请o1的锁
                System.out.println("我没有死锁-2");
            }
        }catch (InterruptedException e){
            System.out.println(Thread.currentThread().getName()+":进入了catch块，因为被中断了");
        }finally {
            if (LOCK1.isHeldByCurrentThread()){
                LOCK1.unlock();
            }
            if (LOCK2.isHeldByCurrentThread()){
                LOCK2.unlock();
            }
            System.out.println(Thread.currentThread().getName()+":线程退出");
        }

    }

}
