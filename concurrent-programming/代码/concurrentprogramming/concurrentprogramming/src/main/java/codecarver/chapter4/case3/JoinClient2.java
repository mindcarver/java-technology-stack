package codecarver.chapter4.case3;

/**
 * 测试join（long mills）
 */
public class JoinClient2 {
    public static int i=0;
    public static class AddThread extends Thread{
        @Override
        public void run() {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (; i < 1000; i++);
            System.out.println("目标线程:" + Thread.currentThread().getName() + "执行完，i=" + i);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AddThread t = new AddThread();
        t.start();
        // 测试当前线程只会等待目标线程执行1秒，当前线程就会继续执行
        t.join(1_000);
        System.out.println("当前线程:" + Thread.currentThread().getName() + "执行完，i=" + i);
    }
}
