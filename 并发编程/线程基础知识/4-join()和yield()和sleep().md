# join()和yield()和sleep()

## 一：join()

很多时候,A线程的输出十分依赖B线程的输入，这个时候A线程就必须等待B线程执行完之后再根据线程B的执行结果进行输出。而JDK正提供了 *join()* 方法来实现这个功能。如下两个*join*方法：

```java
public final void join() throws InterruptedException {
    join(0);
}
public final synchronized void join(long millis)
    throws InterruptedException {}
```

第一个join方法表示无限等待，一直会阻塞**当前线程**，直到**目标线程**执行完毕。第二个方法给出了一个最大的等待时间，如果超过给定时间目标线程还在执行，那么当前线程则不管它，会继续执行下去。

**测试*join（）*方法：**

```java
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
```

**运行结果：**

> 1000

我们发现打印结果是1000，如果你不加join的话，main线程（当前线程）就不会被阻塞，可能直接就打印出0出来了。

**测试join(long millis)方法:**

```java
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
```

**运行结果：**

> 当前线程:main执行完，i=0
> 目标线程:Thread-0执行完，i=1000

我们发现，main线程1秒后输出：当前线程:main执行完，i=0，Thread-0线程3秒后输出：目标线程:Thread-0执行完，i=1000，也就是说，**当前线程只会等待目标线程执行1秒，当前线程就会继续执行**，**并且目标线程也会完成它剩下的，不会随着目标线程的结束而结束**。



案例：

```java

    /**
     * 并发弹出元素
     * @throws InterruptedException
     */
    @Test
    public void ConcurrencyPopElementsInList() throws InterruptedException {

        /**
         * 错误示范
         */
       /* Thread[] threads = new MyTask[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new MyTask();
            threads[i].start();
            threads[i].join();
        }*/

        /**
         * 正确方式
         * 上面的问题是，当循环的时候，他其实是当第一个循环，第一个循环就会join，
         * 然后调用线程挂起，其实for循环就是主线程的一部分，所以不会继续执行，要等到第一个循环
         * 完成，才会继续运行循环体，所以也就是为什么只能看见一个线程打印数据。
         */
       Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new MyTask();
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
       System.out.println("删除完成");
    }

     class MyTask  extends Thread{
        @Override
        public void run() {
           while (true){
               logger.info(Thread.currentThread().getName() +
                       "弹出元素：" + redisUtil.lRightPop("gxx"));
           }
        }
    }

```



------

## 二：yield()

从理论上讲，**“yiled”意味着放手，放弃，投降**。一个让步的线程告诉虚拟机它愿意让其他线程被安排在它的位置。这表明**它没有做太重要的事情**。请注意，*它只是一个提示*，但**并不保证**会产生任何影响。

*yield()*在 Thread 中的定义如下：

```java
public static native void yield();
```

- Yield也是一个静态方法和Native。
- Yield告诉当前正在执行的线程为**线程池中**具有**相同优先级的线程**提供机会。
- 每当一个线程调用java.lang.Thread.yield方法时，它就会向线程调度程序提示它已准备好暂停其执行。线程调度程序可以忽略此提示。
- 它只能使一个线程从Running State转为Runnable State，而不是处于wait或blocked状态。
- 如果任何线程执行yield方法，则线程调度程序检查是否存在与此线程具有相同或高优先级的线程。如果处理器找到任何具有更高或相同优先级的线程，则它将当前线程移动到Ready / Runnable状态并将处理器提供给其他线程，如果不是 ,当前线程将继续执行。

**yield（）方法示例用法:**

```java
public class YiledDemo {

    public static void main(String[]args)
    {
        MyThread t = new MyThread();
        t.start();

        for (int i=0; i<5; i++)
        {
            // 将控制权交给了子线程（Mythread）
            Thread.yield();
            // 当执行完子线程之后，main线程继续接手
            System.out.println(Thread.currentThread().getName()
                    + "正在控制");
        }
    }

    static class MyThread extends Thread
    {
        public void run()
        {
            for (int i=0; i<5 ; i++)
                System.out.println(Thread.currentThread().getName()
                        + "正在控制");
        }
    }
}
```

**运行结果：**

> main正在控制
> Thread-0正在控制
> Thread-0正在控制
> Thread-0正在控制
> Thread-0正在控制
> Thread-0正在控制
> main正在控制
> main正在控制
> main正在控制
> main正在控制

<font color="red">PS：不同的机器可能出现不同的结果，当main线程执行了Thread.yield()方法表示自己 会让出CPU，但不是指自己不执行了，它依然会抢占CPU资源，能不能分配就不知道了，所以一般可以对那些优先级低的线程在适当的时候调用此方法。</font>



------

## 三：sleep()

```java
 public static native void sleep(long millis) throws InterruptedException;
```

上面是*Thread.sleep()*方法的函数，它会让当前线程休眠若干时间，并且会在被中断的时候抛出这个异常。

其最简单的用法如下：

```java
public class ThreadDemo {
    private static int i = 0;
    public static class Mythread extends Thread{
        @Override
        public void run() {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                System.out.println("我在睡眠的时候被中断了");
            }
            for (; i < 10; i++);
            System.out.println("i的值是：" + i);
        }
    }

    public static void main(String[] args) {
        Mythread t1 = new Mythread();
        t1.start();
    }
}
```

我们在等待3秒之后，线程会输出："i的值是10"。

PS:如果`Thread.sleep()`方法因为中断而抛出异常，那么此时它会清除当前线程的中断标记，如果不做处理，那么将无法捕获这个中断（也就是说isInterrupted返回的是false），验证如下：

```java
public class ThreadDemo2 {
    public static class Mythread extends Thread{
        @Override
        public void run() {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                // 注释这段来验证
                Thread.currentThread().interrupt(); // 1
            }
            System.out.println(Thread.currentThread().isInterrupted());
        }
    }

    public static void main(String[] args) {
        Mythread t1 = new Mythread();
        t1.start();
        t1.interrupt();
    }
}
```

当我们注释掉代码1之后，输出的结果是false，如果加上，则为true ,结论成立。

------





