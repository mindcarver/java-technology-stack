package codecarver.chapter3.RejectionDemo;

import java.util.concurrent.*;

public class MyTask implements Runnable {

    @Override
    public void run() {
        System.out.println(System.currentTimeMillis() + ":Thread ID:" + Thread.currentThread().getId());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main(String args[]) throws InterruptedException {
        MyTask myTask = new MyTask();
        //拒绝策略1：将抛出 RejectedExecutionException.
//        RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();
        // 策略2：ThreadPoolExecutor.CallerRunsPolicy
//        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        // 策略3:ThreadPoolExecutor.DiscardOldestPolicy();
//        RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardOldestPolicy();

        // 策略4： ThreadPoolExecutor.DiscardPolicy();
        RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardPolicy();

        ExecutorService executorService = new ThreadPoolExecutor(5, 5, 0L,
                TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(10), Executors.defaultThreadFactory()
                , handler);

        for (int i = 0; i < 100; i++) {
            executorService.submit(myTask);
            Thread.sleep(10);
        }
    }
}
