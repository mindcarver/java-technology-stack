package codecarver.chapter2.LockSupport.case1;

import java.util.concurrent.locks.LockSupport;

public class LockSupportDemo {

    public static Object object = new Object();
    static TestThread t1 = new TestThread("线程1");
    static TestThread t2 = new TestThread("线程2");
    public static class TestThread extends Thread{
        public TestThread(String name) {
            super.setName(name);
        }
        @Override
        public void run() {
            synchronized (object) {
                System.out.println(getName()+" 占用。。");
//                Thread.currentThread().suspend();
                LockSupport.park();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName()+" 执行结束！");
            }
        }
    }
    public static void main(String[] args) throws InterruptedException {
        t1.start();

        t2.start();
//        t1.resume();
        LockSupport.unpark(t1);
        LockSupport.unpark(t2);
//        t2.resume();
        t1.join();
        t2.join();
    }
}
