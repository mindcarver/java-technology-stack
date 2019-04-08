package codecarver.chapter2.countDownLactch;

import java.util.concurrent.CountDownLatch;

/**
 * @ClassName CountDownLatchDemo
 * @Description TODO
 * @Author lenovo
 * @Date 2019/1/27 21:11
 **/
public class CountDownLatchDemo {
    public static void main(String args[])
            throws InterruptedException
    {
        CountDownLatch latch = new CountDownLatch(2);

        // 开启3个服务并启动
        Service first = new Service(1000, latch,
                "SERVICE-1");
        Service second = new Service(2000, latch,
                "SERVICE-2");
        Service third = new Service(3000, latch,
                "SERVICE-3");
        first.start();
        second.start();
        third.start();

        // 主线程等待服务完成
        latch.await();

        System.out.println(Thread.currentThread().getName() +
                " has finished");
    }
}

// 服务线程
class Service extends Thread
{
    // 服务执行时间
    private int delay;
    private CountDownLatch latch;

    public Service(int delay, CountDownLatch latch,
                  String name)
    {
        super(name);
        this.delay = delay;
        this.latch = latch;
    }

    @Override
    public void run()
    {
        try {
            Thread.sleep(delay);
            // 服务完成，调用countDown，倒计时减1
            latch.countDown();
            System.out.println(Thread.currentThread().getName()
                    + " finished");
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
