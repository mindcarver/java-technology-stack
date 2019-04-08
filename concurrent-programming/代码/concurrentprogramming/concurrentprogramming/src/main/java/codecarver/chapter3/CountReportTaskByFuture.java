package codecarver.chapter3;

import java.util.ArrayList;
import java.util.concurrent.*;

public class CountReportTaskByFuture implements Callable<Integer> {
    private static int count = 100;
    private static int totalCount = 0;
    @Override
    public Integer call() throws Exception {
        Thread.sleep(3_000);
        return count;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        long time1=System.currentTimeMillis();
        // 保存任务集合
        ArrayList<Future<Integer>> result = new ArrayList<>();
        // 创建任务
        Callable<Integer> task = new CountReportTaskByFuture();

        ExecutorService cachedThreadPool =  Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++) {
            // 提交任务
            Future future =  cachedThreadPool.submit(task);
            result.add(future);
        }
        for (int i = 0; i < result.size(); i++) {
            totalCount += result.get(i).get();
        }

        System.out.println("最终结果:" + totalCount);
        long time2=System.currentTimeMillis();
        System.out.println("当前程序耗时："+(time2-time1)+"ms");
    }
}
