package codecarver.chapter1.c3.case3;

/**
 * 测试join
 */
public class JoinClient {
    public static int i=0;
    public static class AddThread extends Thread{
        @Override
        public void run() {
            for (; i < 1000; i++);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AddThread t = new AddThread();
        t.start();
        // main 线程要执行最后一句输出必须等到t线程执行完毕
        t.join();
        System.out.println(i);
    }
}
