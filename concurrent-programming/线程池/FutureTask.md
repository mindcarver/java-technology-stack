# Future和FutureTask

[TOC]

## 概述

我们都知道创建线程的最基本的两种方式:继承Thread 和 实现Runnable接口，但是这两种方式都无法返回结果。

但是从JDK1.5开始提供了Callable和Future，我们可以结合起来使用从而在任务执行完毕后拿到结果。这篇文章主要分析Runnable和Callable，Future和FutureTask，FutureTask的原理以及使用场景。

---

## Runnable和Callable

> Runnable概述

```java
@FunctionalInterface
public interface Runnable {
    public abstract void run();
}
```

Runnable是一个函数式接口，位于java.lang包下，可以看到其run方法没有返回值，所以无法获得执行任务后的结果，因此则有了Callable接口：

```java
/*
自JDK1.5开始
*/
@FunctionalInterface
public interface Callable<V> {
   	// 返回V 类型的结果，如果不能则抛出异常
    V call() throws Exception;
}
```

说说相同点吧：

1. 都是接口，且都必须要调用Thread.start启动线程

不同点：

1. 实现Callable接口的任务线程能返回执行结果；而实现Runnable接口的任务线程不能返回结果；
2. Callable接口的call()方法允许抛出异常；而Runnable接口的run()方法的异常只能在内部消化，不能继续上抛；

-----

## Future详解

Future是一个接口，其定义如下：

```java
public interface Future<V> {
    /*
    1. 如果任务已经取消，或者任务已经完成或者其他一些原因不能取消则返回false
    2. 如果取消任务成功，那么任务则不会开始，如果任务已经开始，那么则会根据参数是否去中断执行此任务的线程
    3. 调用此方法后，后续再调用isdone的话总是返回ture
    4. 调用此方法后，如果返回的是ture,那么后续的调用都会返回ture
    5. 设置mayInterruptIfRunning为ture的话，表示不会中断正在执行的任务
    */
    boolean cancel(boolean mayInterruptIfRunning);

    // 如果此任务在完成之前被取消，则返回ture
    boolean isCancelled();

    /**
    1. 若次任务完成，返回true
    2. 也有可能是因为正常终止或者正常取消返回了ture
     */
    boolean isDone();

    /**
     1. 阻塞等待计算完成，然后返回结果
     2. 如果被取消了任务，则抛出CancellationException
     3. 如果任务出现异常，抛出ExecutionException
     4. 如果当前线程被中断了,抛出InterruptedException
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     1. 在给定的时间内阻塞等待计算完成
     2. timeout 超时等待时间
     3. TimeUnit 超时等待时间的单位
     */
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```

总结Future的功能：

1. 可判断任务是否完成
2. 可取消任务，并决定是否可以取消正在执行的任务
3. 可以阻塞获取任务返回结果
4. 可以在给定时间内阻塞返回结果

最后需要注意Future只是一个接口，并且我们经常使用的是其实现类FutureTask

### Future结合Callable的使用示例

统计多个任务的执行结果

```java
public class CountReportTaskByFuture implements Callable<Integer>{

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
```



----

## FutureTask详解

FutureTask是一个实现类：

```java
public class FutureTask<V> implements RunnableFuture<V> {}
-----------------------------------------------------------
public interface RunnableFuture<V> extends Runnable, Future<V> {
    // 将此Future设置为其计算结果除非已被取消
    void run();
}
```

从继承关系中不难看出FutureTask实现了RunnableFuture接口，而RunnableFuture接口实现了Runnable和Future接口，所以它`既可以作为Runnable被线程执行`，`又可以作为Future得到Callable的返回值`。

### FutureTask执行多任务计算

场景：统计多个任务返回结果

```java
public class CountReportTaskByFutureTask implements Callable<Integer>{

    private static int count = 100;
    private static int totalCount = 0;
    @Override
    public Integer call() throws Exception {
        Thread.sleep(3_000);
        System.out.println(Thread.currentThread().getName() + "执行完子任务");
        return count;
    }
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        long time1=System.currentTimeMillis();
        ArrayList<FutureTask<Integer>> result = new ArrayList<>();
        ExecutorService cachedThreadPool =  Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++) {
            // 创建FutureTask对象
            FutureTask<Integer> task = new FutureTask(new CountReportTaskByFutureTask());
            // 保存任务对象
            result.add((task));
            // 提交任务到线程池中
            cachedThreadPool.submit(task);
        }
        System.out.println("任务提交完毕，继续执行主线程");

        // 获取异步计算结果
        for (int i = 0; i < result.size(); i++) {
            totalCount += result.get(i).get();
        }

        System.out.println("最终结果:" + totalCount);
        long time2=System.currentTimeMillis();
        System.out.println("当前程序耗时："+(time2-time1)+"ms");
    }
}
```

## FutureTask实现原理

主要从以下几方面内容概述：

1. 核心成员变量
2. 内部状态转换
3. 核心方法解析

###1：核心成员变量

- private volatile int state  : 表示对象状态，volatile关键字保证了内存可见性，并且定义了7中不同的线程执行状态
  - private static final int NEW          = 0; //任务新建和执行中
  - private static final int COMPLETING   = 1; //任务将要执行完毕
  - private static final int NORMAL       = 2; //任务正常执行结束
  - private static final int EXCEPTIONAL  = 3; //任务异常
  - private static final int CANCELLED    = 4; //任务取消
  - private static final int INTERRUPTING = 5; //任务即将被中断
  - private static final int INTERRUPTED  = 6;//任务已中断

- Callable<V> callable:被提交的任务
- Object outcome:任务执行结果或者任务异常
- volatile Thread runner:执行任务的线程
- volatile WaitNode waiters：等待节点，关联等待线程
- long stateOffset:state字段的内存偏移量
- long runnerOffset：runner字段的内存偏移量
- long waitersOffset：waiters字段的内存偏移量

### 2：内部状态转换

FutureTask中使用state表示任务状态，state值变更的由CAS操作保证原子性。

FutureTask对象初始化时，在构造器中把state置为为NEW，之后状态的变更依据具体执行情况来定。

FutureTask的状态流转过程，可以出现以下四种情况：

1. 任务正常执行并返回。 NEW -> COMPLETING -> NORMAL
2. 执行中出现异常。NEW -> COMPLETING -> EXCEPTIONAL
3. 任务执行过程中被取消，并且不响应中断。NEW -> CANCELLED
4. 任务执行过程中被取消，并且响应中断。 NEW -> INTERRUPTING -> INTERRUPTED　　

### 3：核心方法解析

> Run运行原理

```java
public void run() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran)
                    set(result);
            }
        } finally {
            // 在状态确定之前 runner必须为null 
            // 不允许并发调用run方法
            runner = null;
            // 在state状态值为0 后必须重新读取
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }
```

此方法主要做了一下几件事：

1. 检查当前任务状态是否为NEW以及runner是否已赋值。这一步是防止任务被取消。
2. 校验任务状态并执行业务逻辑通过c.call()
3. 若在执行业务逻辑中遇到异常，则将异常对象赋值给outcome同时再次更新state值。
4. 若执行业务逻辑正常，则将结果赋值给outcome,并更新state值，可自行从堆栈测试。

-------

> get()方法剖析

get方法如下：

```java
public V get() throws InterruptedException, ExecutionException {
    int s = state;
    // 当 s<= COMPLETING时，表明任务仍然在执行且没有被取消。
    if (s <= COMPLETING)
        s = awaitDone(false, 0L);
    return report(s);
}
```

不难看出get()方法首先获取任务执行状态，如果任务正在执行且还没有被取消，那么就执行awaitDone方法：

```java
 private int awaitDone(boolean timed, long nanos)
    throws InterruptedException {
     // 任务截止时间
    final long deadline = timed ? System.nanoTime() + nanos : 0L;
    WaitNode q = null;
    boolean queued = false;
     // 自旋操作
    for (;;) {
        if (Thread.interrupted()) {
            //线程中断则移除等待线程,并抛出异常
            removeWaiter(q);
            throw new InterruptedException();
        }

        int s = state;
        if (s > COMPLETING) {
             // 任务可能已经完成或者被取消了
            if (q != null)
                q.thread = null;
            return s;
        }
        else if (s == COMPLETING) 
             // 可能任务线程被阻塞了,主线程让出CPU，防止超时
            Thread.yield();
        else if (q == null)
            // 等待线程节点为空,则初始化新节点并关联当前线程
            q = new WaitNode();
        else if (!queued)
            queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                 q.next = waiters, q);
        else if (timed) {
            nanos = deadline - System.nanoTime();
             //已经超时的话,移除等待节点
            if (nanos <= 0L) {
                removeWaiter(q);
                return state;
            }
             // 未超时,将当前线程挂起指定时间
            LockSupport.parkNanos(this, nanos);
        }
        else
            // timed=false时会走到这里,挂起当前线程
            LockSupport.park(this);
    }
}
```

waitDone大概做了以下几件事：

1. 根据deadLine确认是否超时
2. 自旋进行以下操作：
   - 判断线程是否中断，若中断则移除线程
   - 判断任务状态，若任务完成或取消则直接返回；若任务被阻塞，则直接让出CPU；若等待节点为空，则初始化节点并关联当前任务线程；若已经超时，则移除等待节点；若未超时，则挂起当前线程到执行时间

最终通过report方法，返回结果，如果状态值s==NORMAL,则返回outcome所得到的值，上面讲到的outcome,如果s >= CANCELLED，则直接抛出CancellationException，除此之外抛出ExecutionException。

```java
 private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL)
            return (V)x;
        if (s >= CANCELLED)
            throw new CancellationException();
        throw new ExecutionException((Throwable)x);
    }
```

--------

> cancel()详解

```java
public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW &&
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        try {    // in case call to interrupt throws exception
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null)
                        t.interrupt();
                } finally { // final state
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

```

cancel方法主要做了以下几件事：

1. 判断任务当前执行状态，如果任务状态不为NEW，则说明任务或者已经执行完成，或者执行异常，不能被取消，直接返回false表示执行失败。
2.  判断需要中断任务执行线程，则
   - 把任务状态从NEW转化到INTERRUPTING。这是个中间状态。
   - 中断任务执行线程。
   - 修改任务状态为INTERRUPTED。这个转换过程对应finishCompletion。
3. 如果不需要中断任务执行线程，直接把任务状态从NEW转化为CANCELLED。如果转化失败则返回false表示取消失败。这个转换过程对应finishCompletion。
4. 调用finishCompletion()。

```java
 private void finishCompletion() {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null;) {
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null)
                        break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        done();

        callable = null;        // to reduce footprint
    }
```

这个方法的实现比较简单，依次遍历waiters链表，唤醒节点中的线程，然后把callable置空。

被唤醒的线程会各自从awaitDone()方法中的LockSupport.park()阻塞中返回，然后会进行新一轮的循环。在新一轮的循环中会返回执行结果(或者更确切的说是返回任务的状态)。

----



>  参考：
>
> 《并发编程的艺术》
>
> https://www.cnblogs.com/dolphin0520/p/3949310.html
>
> https://juejin.im/post/5a3242ef6fb9a045076fb16d
>
> https://juejin.im/post/5a3242ef6fb9a045076fb16d
>
> https://www.cnblogs.com/linghu-java/p/8991824.html

