package codecarver.chapter3.scheduledThreadPoolExecutor;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName ScheduledThreadPoolExecutor
 * @Description TODO
 * @Author lenovo
 * @Date 2019/2/8 19:25
 **/
public class ScheduledThreadPoolExecutor {
    public static void main(String[] args) {
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(10);
        //如果前面的任务没有完成，则调度也不会启动
        //每隔一秒执行输出
        ses.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3_000);
                    System.out.println(new Date(System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
}
