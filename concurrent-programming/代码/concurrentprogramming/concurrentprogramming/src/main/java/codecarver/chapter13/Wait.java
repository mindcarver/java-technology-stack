package codecarver.chapter13;

// 理论应该打印开始执行-> 线程将要阻塞-> 开始执行-> 线程将要阻塞-> 线程退出
// 然而不是 ，而是开始执行-> 线程将要阻塞-> 线程退出
// 虽然阻塞之后会必须重新抢锁，但是线程会从它之前运行的位置开始执行
public class Wait {
    private static final Object LOCK = new Object();
    private static void work(){
        synchronized (LOCK){
            System.out.println("开始执行");
            try {
                System.out.println("线程将要阻塞");
                LOCK.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程退出");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Thread(){
            @Override
            public void run() {
                work();
            }
        }.start();
        Thread.sleep(1_000);
        synchronized (LOCK){
            LOCK.notify();
        }

    }
}
