package codecarver.chapter18.threadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 固定线程池大小
 */
public class FixedThreadPoolDemo {
    // 任务
    public static class MyTask implements Runnable {
        @Override
        public void run() {
            System.out.println(System.currentTimeMillis() + "Thread ID:" + Thread.currentThread().getId());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String args[]) {
        MyTask myTask = new MyTask();
        // 任务的执行
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++) {
            executorService.execute(myTask);
        }
    }
}
