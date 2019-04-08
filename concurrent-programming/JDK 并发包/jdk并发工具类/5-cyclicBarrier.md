# cyclicBarrier

## 一：cyclicBarrier简介

cyclicBarrier是一种多线程并发控制工具，与countDownLatch非常类似，也可以实现线程间计数等待，但在功能上比countDownLatch更为强大。

cyclicBarrier用于使线程彼此等待，当不同的线程分别处理计算中的一部分服务，并且当所有线程都完成执行的时候，使用它，也就是说当多个线程执行不同的子任务并且需要组合这些子任务的输出来形成最终的输出，可以使用cyclibarrier。完成执行后，线程调用await()方法并且等待其他线程到达屏障。一旦所有的线程到达，cyclicBarrier就会为线程提供方法。

基本使用：

CyclicBarrier默认的构造方法是CyclicBarrier(int parties)，其参数表示屏障拦截的线程数量，每个线程使用await()方法告诉CyclicBarrier我已经到达了屏障，然后当前线程被阻塞。

```java
CyclicBarrier Barrier = new CyclicBarrier（parties）;
```

之后每个线程都会进行计算，完成自己的执行之后，会调用await()方法：

```java
public void run (){
    // 线程执行操作
    Barrier.await();
}
```

其原理图如下：



CyclicBarrier的另一个构造函数CyclicBarrier(int parties, Runnable barrierAction)，用于线程到达屏障时，优先执行barrierAction，方便处理更复杂的业务场景。

> ```
> //当所有线程到达障碍时要执行的操作;
> CyclicBarrier Barrier = new CyclicBarrier（numberOfThreads，action）;
> ```
>

CyclicBarrier常用方法：

> ```java
> // 返回设置屏障外的线程数
> public int getParties（）
> 
> // 将屏障外的线程数重置
> // 如果还有其他线程正在屏障等待，则会抛出BrokenBarrierException
> public void reset（）
> 
> // 表示当前线程到达栅栏,Parties数目-1
> public int await（）throw InterruptedException，BrokenBarrierException
> 
> ```

## 二：案例

模拟一种情景，当所有人都到齐后再执行任务：

```java
public class CyclicBarrierDemo {
    // 自定义工作线程
    private static class Worker extends Thread {
        private CyclicBarrier cyclicBarrier;

        public Worker(CyclicBarrier cyclicBarrier) {
            this.cyclicBarrier = cyclicBarrier;
        }

        @Override
        public void run() {
            super.run();

            try {
                System.out.println(Thread.currentThread().getName() + "开始等待其他线程");
                cyclicBarrier.await();
                System.out.println(Thread.currentThread().getName() + "开始执行");
                // 工作线程开始处理，这里用Thread.sleep()来模拟业务处理
                Thread.sleep(5_000);
                System.out.println(Thread.currentThread().getName() + "执行完毕");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3);

        for (int i = 0; i < 3; i++) {
            Worker worker = new Worker(cyclicBarrier);
            worker.start();
        }
    }
}
```

> 运行结果：
>
> Thread-2开始等待其他线程
> Thread-0开始等待其他线程
> Thread-1开始等待其他线程
> Thread-1开始执行
> Thread-0开始执行
> Thread-2开始执行
> Thread-0执行完毕
> Thread-2执行完毕
> Thread-1执行完毕

barrier.await()表示某线程已经到达栅栏，当所有线程都到达栅栏外后，会开始执行每个线程的方法。

## 三：CyclicBarrier和CountDownLatch的区别

1. CountDownLatch的计数器只能使用一次，而CyclicBarrier的计数器可以使用reset()方法重置，可以使用多次，所以CyclicBarrier能够处理更为复杂的场景；
2. CyclicBarrier还提供了一些其他有用的方法，比如getNumberWaiting()方法可以获得CyclicBarrier阻塞的线程数量，isBroken()方法用来了解阻塞的线程是否被中断；
3. CountDownLatch允许一个或多个线程等待一组事件的产生，而CyclicBarrier用于等待其他线程运行到栅栏位置。



----

> 参考：
>
> https://blog.csdn.net/hanchao5272/article/details/79779639

