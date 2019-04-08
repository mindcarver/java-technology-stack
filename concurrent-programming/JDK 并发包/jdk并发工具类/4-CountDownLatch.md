# CountDownLatch

## 一：CountDownLatch介绍

CountDownLatch 是一个多线程控制工具类，用来控制线程等待，它可以让某个线程等待直到倒计时结束，再执行。

## 二：案例

API：
```java
public CountDownLatch (int count)
```

> 模拟一个场景，现在有3个服务，我们必须等待3个服务全部完成之后才能调用主线程

```java
public class CountDownLatchDemo {
    public static void main(String args[])
            throws InterruptedException
    {
        CountDownLatch latch = new CountDownLatch(3);

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
```

说明：通过  CountDownLatch latch = new CountDownLatch(3)来生成countDownLatch实例，3表示需要3个线程完成任务。通过 latch.await()告诉main线程必须等待我的所有线程全部执行完才可以执行main线程。当我们执行完一个线程任务的时候，调用latch.countDown()可以告知计时器减1，当计时器为0的时候，才会执行main线程，所以要注意计时器的个数是否和线程数目匹配。

## 三：countDownLatch使用以及注意点

场景：
当线程像main一样，使用CountDownLatch 时，需要等待一个或多个线程完成，然后才开始进行处理，就可以使用countDownLatch,在Java 中使用CountDownLatch的经典示例是使用服务体系结构的任何服务器端核心Java应用程序，其中多个服务由多个线程提供，并且应用程序无法在所有服务成功启动之前开始处理.

注意点：
1.   一旦count达到零，不能再用CountDownLatch。
2.   主线程通过调用CountDownLatch.await（）方法等待Latch，而其他线程调用CountDownLatch.countDown（）以通知它们已完成。
3. 计数器必须和要执行的线程数匹配，如果计数器大于了要执行的线程数目，那么count最后不会为0，调用的主线程也不会继续执行，主线程将会一直处于阻塞状态。

----

