# LockSupport

## 一：概念

LockSupport 是一个非常方便实用的线程阻塞工具，它可以在线程内任意位置让线程阻塞。

和 Thread.suspend()相比，它弥补了由于 resume()发生，导致线程无法继续执行的情况。

和 Object.wait()相比，它不需要先获得某个对象的锁，也不会抛出 InterruptedException 异常。

LockSupport 的静态方法 park()可以阻塞当前线程，类似的还有 parkNanos()、parkUntil()等方法。它们实现了一个限时等待。

![image-20190404082539352](https://ws2.sinaimg.cn/large/006tKfTcgy1g1qanbyxjej31qu0f215e.jpg)

---

## 二：LockSupport支持定时阻塞

```java
public static Object object = new Object();
    static TestThread t1 = new TestThread("线程1");
    static TestThread t2 = new TestThread("线程2");
    public static class TestThread extends Thread{
        public TestThread(String name) {
            super.setName(name);
        }
        @Override
        public void run() {
            synchronized (object) {
                System.out.println(getName()+" 占用。。");
//                Thread.currentThread().suspend();
                LockSupport.park();
                System.out.println(Thread.currentThread().getName()+" 执行结束！");
            }
        }
    }
    public static void main(String[] args) throws InterruptedException {
        t1.start();
        Thread.sleep(200);
        t2.start();
//        t1.resume();
        LockSupport.unpark(t1);
        LockSupport.unpark(t2);
//        t2.resume();
        t1.join();
        t2.join();
    }
```

运行结果：

> 线程1 占用。。
> 线程1 执行结束！
> 线程2 占用。。
> 线程2 执行结束！

可将 suspend()和 resume()改为 park()与 unpark()看看变化。

我们无法保证 unpark()方法发生在 park()之后，但是多次执行代码，发现始终都可以正常结束，不会因为两方法的顺序导致线程永久性挂起。这是因为，其使用类似信号量的机制。它为每个线程准备了一个许可，如果许可可用，那么 park()方法立即返回，并消费这个许可（将许可变为不可用）；如果许可不可用，则阻塞。而 unpark()方法，则是使一个许可变为可用。

上述特点使得：即使 unpark()操作发生在 park()之前，它也可以使下一次得 park()操作立即返回。这是上述实例代码顺利结束的原因。

同时，park()挂起的线程不会像 suspend()那样线程状态为 RUNNABLE，park()会明确给出 WAITING 状态，并标注由 park()引起，如下图所示：

![image-20190404090114623](https://ws2.sinaimg.cn/large/006tKfTcly1g1qbnzqhpvj30ym052gp6.jpg)

这个标注，使得分析问题时更加方便，可以使用 park(Object)方法，为当前线程设置一个阻塞对象，这个阻塞对象会出现在线程 Dump 中。

将实例中 13 行改为：

```java
LockSupport.park(this);
```

jstack 输出为：

![image-20190404090232963](https://ws2.sinaimg.cn/large/006tKfTcly1g1qbpda6buj30z205mgq6.jpg)

除了有定时阻塞的功能，LockSupport.park()方法还能支持中断影响。但是和其他接受中断的函数不一样，LockSupport.part()方法不会抛出InterruptException异常。它只会默默返回。但是我们可以从Thread.interrupted()等方法中获得中断标记。

## 三：Locksupport支持中断

```java
public class LockSupportIntDemo {
    public static Object u = new Object();

    static ChangeObjectThread t1 = new ChangeObjectThread("t1");
    static ChangeObjectThread t2 = new ChangeObjectThread("t2");
    public static class ChangeObjectThread extends Thread{
        public ChangeObjectThread(String name){
            super.setName(name);
        }

        @Override
        public void run() {
            synchronized (u){
                System.out.println("in " + getName());
                LockSupport.park();
                if(Thread.interrupted()){
                    System.out.println(getName()+" 被中断了");
                }
            }

            System.out.println(getName()+"执行结束");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        t1.start();
        Thread.sleep(100);
        t2.start();
        t1.interrupt();
        LockSupport.unpark(t2);
    }
}

```

运行结果：

> in t1
> t1 被中断了
> t1执行结束
> in t2
> t2执行结束

上述代码第29行中断了处于park方法状态的t1.之后，t1可以马上响应这个中断，并且返回。t1返回后在外面等待的t2才可以进入临界区。并最终由Locksupport.unpark(t2)操作使其运行结束。

---

> 参考:
>
> 高并发程序设计

https://blog.csdn.net/u013851082/article/details/70242395

