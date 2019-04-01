package codecarver.chapter13;

/**
 * 1.所有的对象都会有一个wait set,用来存放调用了该对象wait方法之后进入block状态线程
 * 2.线程被notify之后，不一定立即得到执行
 * 3.线程从wait set中被唤醒顺序不一定是FIFO.(根据实验效果来判定)
 * 4.线程被唤醒后，必须重新获取锁
 */
public class WaitSet {
    private static final Object LOCK = new Object();
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(){
                @Override
                public void run() {
                    synchronized (LOCK){
                        try {
                            System.out.println(Thread.currentThread().getName() + "即将等待");
                            LOCK.wait();
                            System.out.println(Thread.currentThread().getName() + "锁被释放");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            t.start();
        }
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("------------------------------");
        for (int i = 0; i < 10; i++) {
            synchronized (LOCK){
                LOCK.notify();
            }
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
