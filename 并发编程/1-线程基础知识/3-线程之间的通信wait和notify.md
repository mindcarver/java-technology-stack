# 等待wait()和通知notify()、notifyAll()

<blockquote style=" border-left-color:yellow;color:white;background-color:black;width:30%">简单分析
</blockquote>

线程之间用来通信的两个方法：`wait()等待`和`notify()通知`.它们是object类里的方法。如下：

| 方法名称        | 描述                                                         |
| --------------- | ------------------------------------------------------------ |
| notify()        | 通知一个在对象上等待的线程，使其从wait()方法返回，而返回的前提是该线程获取到了对象的锁。 |
| notifyAll()     | 通知所有等待在该对象上的线程。                               |
| wait()          | 调用该方法的线程由RUNNING进入WAITING状态，只有等待另外线程的通知或被终端才会返回，需要注意，**调用wait()方法后，会释放对象的锁。** |
| wait(long)      | 超时等待一段时间，这里的参数时间是毫秒，也就是等待长达n毫秒，如果没有通知就超时返回。 |
| wait(long, int) | 对于超时时间更细粒度的控制，可以达到纳秒。                   |

**我们进入到Object源码里去看下wait():**

```java
 public final void wait() throws InterruptedException {
        wait(0);
    }
 public final native void notify();
```

可以发现wait（）方法里调用了wait(0)，参数表示超时时间，为0的话就是一直等待，知道当前线程被通知之后才会结束等待。可以发现notify是一个本地方法。

`wait():`当在一个`对象实例`上调用wait（）方法后，`当前对象实例所在的线程`就会在这个对象实例上等待。直白的讲就是线程A里面调用了obj.wait(),那么线程A就不会继续执行了，转为`等待状态`,直到其他线程B调用了obj.notify(),或者A线程再调用obj.notify(),所以很明显这两个方法可以实现线程之间的通信。

下面是一个很简单的等待通知的例子：

```java
public class SimpleWait {
    final static Object object = new Object();
    public static class T1 extends Thread{
        @Override
        public void run() {
            synchronized (object){
            System.out.println("T1线程开启");
            try {
                System.out.println("T1线程进入等待");
                object.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("T1线程继续执行");
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("T1线程结束");
        }
    }
    }

    public static class T2 extends Thread{
        @Override
        public void run() {
            synchronized (object){
                System.out.println("T2开启，通知一个线程");
                object.notify();
                try {
                    // 虽然通知了（这里指的是等待队列的任意线程）
                    // 但是模拟一下T2线程还需要经历2秒左右才能处理完
                    // 也就是说2秒后才会释放锁
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("T2结束");
            }
        }
    }
    public static void main(String[] args) {
        Thread t1 = new T1();
        Thread t2 = new T2();
        t1.start();
        t2.start();
    }
}

```

上面的代码大概是这个意思：开启两个线程T1和T2，T1先拿到锁之后，进入等待（释放锁），此时T2拿到锁，通知等待队列里的任意线程（这里就一个T1），之后执行完T2线程的剩下动作，T2释放锁，T1继续从等待的地方执行。从而完成T1和T2之间的线程通信。

----

用一个图大概的说明下wait()、notify()的工作流程：

![1547819436454](http://gxx-resource.oss-cn-hangzhou.aliyuncs.com/img/concurrencyprogramming/1547819436454.png)

1. T1线程拿到锁之后，当调用wait()或者join()方法，将进入等待状态，同时`释放锁`

![1547819804954](http://gxx-resource.oss-cn-hangzhou.aliyuncs.com/img/concurrencyprogramming/1547819804954.png)

2. T2抢到锁之后，处于运行状态，当调用notify方法，假设这里只有T1等待中，那么T1从等待状态进入到阻塞状态，因为它还没有得到锁，也就意味着T2虽然通知了T1，但是还没有执行完它剩下的程序。当T2执行完，T1顺利拿到锁之后，进入到运行状态，接着之前等待状态那边继续执行。

![1547820616782](http://gxx-resource.oss-cn-hangzhou.aliyuncs.com/img/concurrencyprogramming/1547820616782.png)

3. 除了notify()方法，还有一个notifyAll方法，它的用法跟notify基本类似，但是他不像notify`随机的通知一个线程`,它是`通知所有线程`,拿到锁的那个线程将会继续处于运行状态

----

注意点：

1. Object.wait()、notify()、notifyAll()必须包含在synchronized语句中，它们必须获得目标对象的监视器
2. Object.wait()方法和Thread.sleep()方法都可以让线程等待若干时间，wait()方法在等待的时候会释放锁，但是Thread.sleep不会释放任何资源

---

