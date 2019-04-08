# LinkedBlockingQueue

## 一：概述

上篇我们介绍了[ArrayBlockingQueue]的相关方法的原理，这一篇我们来学习一下LinkedBlockingQueue。在Collection中，大家都用过ArrayList和LinkedList。ArrayList和ArrayBlockingQueue一样，内部基于数组来存放元素，而LinkedBlockingQueue则和LinkedList一样，内部基于链表来存放元素,下面正式分析。

## 二：LinkedBlockingQueue详解

先来看看LinkedBlockingQueue的继承关系：

```java
public class LinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {}
```

### 1.构造函数

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">构造函数
</blockquote>

```java
// 无参构造函数 this调用链
public LinkedBlockingQueue() {
    // 不传，默认容量最大 
    this(Integer.MAX_VALUE);
}
// 传入默认容量
public LinkedBlockingQueue(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException();
    this.capacity = capacity;
    // 初始化一个头结点和尾节点
    last = head = new Node<E>(null);
}
// 传入Collection
public LinkedBlockingQueue(Collection<? extends E> c) {
    // 默认容量最大值
    this(Integer.MAX_VALUE);
    // 设置putLock
    final ReentrantLock putLock = this.putLock;
    putLock.lock(); // 不会产生竞争，是为了保证可见性
    try {
        int n = 0;
        for (E e : c) {
            if (e == null)
                throw new NullPointerException();
            if (n == capacity)
                throw new IllegalStateException("Queue full");
            // 入队操作 
            enqueue(new Node<E>(e));
            ++n; 
        }
        // 设置队列个数
        count.set(n);
    } finally {
        putLock.unlock();
    }
}
```

-----

### 2.类基本属性

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">类基本属性
</blockquote>

```java
public class LinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    // 版本序列号
    private static final long serialVersionUID = -6903933977591709194L;
    // 容量
    private final int capacity;
    // 元素的个数
    private final AtomicInteger count = new AtomicInteger();
    // 头结点
    transient Node<E> head;
    // 尾结点
    private transient Node<E> last;
    // 取元素锁
    private final ReentrantLock takeLock = new ReentrantLock();
    // 非空条件
    private final Condition notEmpty = takeLock.newCondition();
    // 存元素锁
    private final ReentrantLock putLock = new ReentrantLock();
    // 非满条件
    private final Condition notFull = putLock.newCondition();
}
```

注意：LinkedBlockingQueue包含了读、写重入锁（与ArrayBlockingQueue不同，ArrayBlockingQueue只包含了一把重入锁），读写操作进行了分离，并且不同的锁有不同的Condition条件（与ArrayBlockingQueue不同，ArrayBlockingQueue是一把重入锁的两个条件）。

------

### 3.put函数解析

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">put函数解析
</blockquote>

```java
public void put(E e) throws InterruptedException {
        // 值不为空
        if (e == null) throw new NullPointerException();
       
        int c = -1;
        // 新生结点
        Node<E> node = new Node<E>(e);
        // 存元素锁
        final ReentrantLock putLock = this.putLock;
        // 元素个数
        final AtomicInteger count = this.count;
        // 如果当前线程未被中断，则获取锁
        putLock.lockInterruptibly();
        try {
           
            while (count.get() == capacity) { // 元素个数到达指定容量
                // 在notFull条件上进行等待
                notFull.await();
            }
            // 入队列
            enqueue(node);
            // 更新元素个数，返回的是以前的元素个数
            c = count.getAndIncrement();
            if (c + 1 < capacity) // 元素个数是否小于容量
                // 唤醒在notFull条件上等待的某个线程
                notFull.signal();
        } finally {
            // 释放锁
            putLock.unlock();
        }
        if (c == 0) // 元素个数为0，表示已有take线程在notEmpty条件上进入了等待，则需要唤醒在notEmpty条件上等待的线程
            signalNotEmpty();
    }
```

说明：put函数用于存放元素，其流程如下:

1. 判断元素是否为null，如果是的话就直接抛出异常，否则进入2
2. 获取存元素锁，并上锁，若当前线程被中断，则抛出异常，否则，进入步骤3
3. 判断当前队列中的元素个数是否已经达到指定容量，若是，则在notFull条件上进行等待，否则进入步骤4
4. 将新生结点入队列，更新队列元素个数，若元素个数小于指定容量，则唤醒在notFull条件上等待的线程，表示可以继续存放元素。进入步骤5
5. 释放锁，判断结点入队列之前的元素个数是否为0，若是，则唤醒在notEmpty条件上等待的线程（表示队列中没有元素，取元素线程被阻塞了）。

put函数中会调用到enqueue函数和signalNotEmpty函数，enqueue函数源码如下　

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">enqueue源码
</blockquote>

```java
private void enqueue(Node<E> node) {
        // assert putLock.isHeldByCurrentThread();
        // assert last.next == null;
        // 更新尾结点域
        last = last.next = node;
    }
```

可以看到，enqueue函数只是更新了尾节点。signalNotEmpty函数源码如下　：

```java
private void signalNotEmpty() {
        // 取元素锁
        final ReentrantLock takeLock = this.takeLock;
        // 获取锁
        takeLock.lock();
        try {
            // 唤醒在notEmpty条件上等待的某个线程
            notEmpty.signal();
        } finally {
            // 释放锁
            takeLock.unlock();
        }
    }
```

说明：signalNotEmpty函数用于唤醒在notEmpty条件上等待的线程，其首先获取取元素锁，然后上锁，然后唤醒在notEmpty条件上等待的线程，最后释放取元素锁。

-----

### 4.offer函数解析

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">offer函数解析
</blockquote>

```java
public boolean offer(E e) {
        // 确保元素不为null
        if (e == null) throw new NullPointerException();
        // 获取计数器
        final AtomicInteger count = this.count;
        if (count.get() == capacity) // 元素个数到达指定容量
            // 返回
            return false;
        // 
        int c = -1;
        // 新生结点
        Node<E> node = new Node<E>(e);
        // 存元素锁
        final ReentrantLock putLock = this.putLock;
        // 获取锁
        putLock.lock();
        try {
            if (count.get() < capacity) { // 元素个数小于指定容量
                // 入队列
                enqueue(node);
                // 更新元素个数，返回的是以前的元素个数
                c = count.getAndIncrement();
                if (c + 1 < capacity) // 元素个数是否小于容量
                    // 唤醒在notFull条件上等待的某个线程
                    notFull.signal();
            }
        } finally {
            // 释放锁
            putLock.unlock();
        }
        if (c == 0) // 元素个数为0，则唤醒在notEmpty条件上等待的某个线程
            signalNotEmpty();
        return c >= 0;
    }
```

offer函数也用于存放元素，offer函数添加元素不会抛出异常（其他的域put函数类似）。

----

### 5.take函数解析

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">take函数解析
</blockquote>

```java
public E take() throws InterruptedException {
        E x;
        int c = -1;
        // 获取计数器
        final AtomicInteger count = this.count;
        // 获取取元素锁
        final ReentrantLock takeLock = this.takeLock;
        // 如果当前线程未被中断，则获取锁
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) { // 元素个数为0
                // 在notEmpty条件上等待
                notEmpty.await();
            }
            // 出队列
            x = dequeue();
            // 更新元素个数，返回的是以前的元素个数
            c = count.getAndDecrement();
            if (c > 1) // 元素个数大于1，则唤醒在notEmpty上等待的某个线程
                notEmpty.signal();
        } finally {
            // 释放锁
            takeLock.unlock();
        }
        if (c == capacity) // 元素个数到达指定容量
            // 唤醒在notFull条件上等待的某个线程
            signalNotFull();
        // 返回
        return x;
    }
```

说明：take函数用于获取一个元素，其与put函数相对应，其流程如下。

　　1.  获取取元素锁，并上锁，如果当前线程被中断，则抛出异常，否则，进入步骤2

　　2.  判断当前队列中的元素个数是否为0，若是，则在notEmpty条件上进行等待，否则，进入步骤3

　　3.  出队列，更新队列元素个数，若元素个数大于1，则唤醒在notEmpty条件上等待的线程，表示可以继续取元素。进入步骤4

　　4.  释放锁，判断结点出队列之前的元素个数是否为指定容量，若是，则唤醒在notFull条件上等待的线程（表示队列已满，存元素线程被阻塞了）。

　　take函数调用到了dequeue函数和signalNotFull函数，dequeue函数源码如下　:　

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">Dequeue 函数解析
</blockquote>

```java
private E dequeue() {
        // assert takeLock.isHeldByCurrentThread();
        // assert head.item == null;
        // 头结点
        Node<E> h = head;
        // 第一个结点
        Node<E> first = h.next;
        // 头结点的next域为自身
        h.next = h; // help GC
        // 更新头结点
        head = first;
        // 返回头结点的元素
        E x = first.item;
        // 头结点的item域赋值为null
        first.item = null;
        // 返回结点元素
        return x;
    }
```

说明：dequeue函数的作用是将头结点更新为之前头结点的下一个结点，并且将更新后的头结点的item域设置为null。signalNotFull函数的源码如下:

```java
private void signalNotFull() {
        // 存元素锁
        final ReentrantLock putLock = this.putLock;
        // 获取锁
        putLock.lock();
        try {
            // 唤醒在notFull条件上等待的某个线程
            notFull.signal();
        } finally {
            // 释放锁
            putLock.unlock();
        }
    }
```

说明：signalNotFull函数用于唤醒在notFull条件上等待的某个线程，其首先获取存元素锁，然后上锁，然后唤醒在notFull条件上等待的线程，最后释放存元素锁。

-----

### 6.poll函数

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">poll 函数解析
</blockquote>

```java
public E poll() {
        // 获取计数器
        final AtomicInteger count = this.count;
        if (count.get() == 0) // 元素个数为0
            return null;
        // 
        E x = null;
        int c = -1;
        // 取元素锁
        final ReentrantLock takeLock = this.takeLock;
        // 获取锁
        takeLock.lock();
        try {
            if (count.get() > 0) { // 元素个数大于0
                // 出队列
                x = dequeue();
                // 更新元素个数，返回的是以前的元素个数
                c = count.getAndDecrement();
                if (c > 1) // 元素个数大于1
                    // 唤醒在notEmpty条件上等待的某个线程
                    notEmpty.signal();
            }
        } finally {
            // 释放锁
            takeLock.unlock();
        }
        if (c == capacity) // 元素大小达到指定容量
            // 唤醒在notFull条件上等待的某个线程
            signalNotFull();
        // 返回元素
        return x;
    }
```

　　说明：poll函数也用于存放元素，poll函数添加元素不会抛出异常（其他的与take函数类似）。

-----

### 7.remove函数

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">remove 函数解析
</blockquote>


```java
public boolean remove(Object o) {
        // 元素为null，返回false
        if (o == null) return false;
        // 获取存元素锁和取元素锁（不允许存或取元素）
        fullyLock();
        try {
            for (Node<E> trail = head, p = trail.next;
                 p != null;
                 trail = p, p = p.next) { // 遍历整个链表
                if (o.equals(p.item)) { // 结点的值与指定值相等
                    // 断开结点
                    unlink(p, trail);
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }
```

说明：remove函数的流程如下

　　① 获取读、写锁（防止此时继续出、入队列）。进入步骤②

　　② 遍历链表，寻找指定元素，若找到，则将该结点从链表中断开，有利于被GC，进入步骤③

　　③ 释放读、写锁（可以继续出、入队列）。步骤②中找到指定元素则返回true，否则，返回false。

　　其中，remove函数会调用unlink函数，其源码如下　:

```java
void unlink(Node<E> p, Node<E> trail) {
        // assert isFullyLocked();
        // p.next is not changed, to allow iterators that are
        // traversing p to maintain their weak-consistency guarantee.
        // 结点的item域赋值为null
        p.item = null;
        // 断开p结点
        trail.next = p.next;
        if (last == p) // 尾节点为p结点
            // 重新赋值尾节点
            last = trail;
        if (count.getAndDecrement() == capacity) // 更新元素个数，返回的是以前的元素个数，若结点个数到达指定容量
            // 唤醒在notFull条件上等待的某个线程
            notFull.signal();
    }
```

说明：unlink函数用于将指定结点从链表中断开，并且更新队列元素个数，并且判断若之前队列元素的个数达到了指定容量，则会唤醒在notFull条件上等待的某个线程。

## 三：简单使用

由于LinkedBlockingQueue实现是线程安全的，实现了先进先出等特性，是作为生产者消费者的首选，LinkedBlockingQueue 可以指定容量，也可以不指定，不指定的话，默认最大是Integer.MAX_VALUE，其中主要用到put和take方法，put方法在队列满的时候会阻塞直到有队列成员被消费，take方法在队列空的时候会阻塞，直到有队列成员被放进来。

```java
public class LinkedBlockingQueueDemo {
    // 定义装苹果的篮子
    public class Basket {
        // 篮子，能够容纳3个苹果
        BlockingQueue<String> basket = new LinkedBlockingQueue<String>(3);

        // 生产苹果，放入篮子
        public void produce() throws InterruptedException {
            // put方法放入一个苹果，若basket满了，等到basket有位置
            basket.put("An apple");
        }

        // 消费苹果，从篮子中取走
        public String consume() throws InterruptedException {
            // take方法取出一个苹果，若basket为空，等到basket有苹果为止(获取并移除此队列的头部)
            return basket.take();
        }
    }

    // 定义苹果生产者
    class Producer implements Runnable {
        private String instance;
        private Basket basket;

        public Producer(String instance, Basket basket) {
            this.instance = instance;
            this.basket = basket;
        }

        public void run() {
            try {
                while (true) {
                    // 生产苹果
                    System.out.println("生产者准备生产苹果：" + instance);
                    basket.produce();
                    System.out.println("!生产者生产苹果完毕：" + instance);
                    // 休眠300ms
                    Thread.sleep(300);
                }
            } catch (InterruptedException ex) {
                System.out.println("Producer Interrupted");
            }
        }
    }

    // 定义苹果消费者
    class Consumer implements Runnable {
        private String instance;
        private Basket basket;

        public Consumer(String instance, Basket basket) {
            this.instance = instance;
            this.basket = basket;
        }

        public void run() {
            try {
                while (true) {
                    // 消费苹果
                    System.out.println("消费者准备消费苹果：" + instance);
                    System.out.println(basket.consume());
                    System.out.println("!消费者消费苹果完毕：" + instance);
                    // 休眠1000ms
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ex) {
                System.out.println("Consumer Interrupted");
            }
        }
    }

    public static void main(String[] args) {
        LinkedBlockingQueueDemo test = new LinkedBlockingQueueDemo();

        // 建立一个装苹果的篮子
        Basket basket = test.new Basket();

        ExecutorService service = Executors.newCachedThreadPool();
        Producer producer = test.new Producer("生产者001", basket);
        Producer producer2 = test.new Producer("生产者002", basket);
        Consumer consumer = test.new Consumer("消费者001", basket);
        service.submit(producer);
        service.submit(producer2);
        service.submit(consumer);
        // 程序运行5s后，所有任务停止
        try {
            Thread.sleep(1000 * 5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        service.shutdownNow();
    }
}
```



运行结果：

> 生产者准备生产苹果：生产者001
> 消费者准备消费苹果：消费者001
> !生产者生产苹果完毕：生产者001
> An apple
> 生产者准备生产苹果：生产者002
> !消费者消费苹果完毕：消费者001
> !生产者生产苹果完毕：生产者002
> 生产者准备生产苹果：生产者001
> 生产者准备生产苹果：生产者002
> !生产者生产苹果完毕：生产者002
> !生产者生产苹果完毕：生产者001
> 生产者准备生产苹果：生产者001
> 生产者准备生产苹果：生产者002
> 消费者准备消费苹果：消费者001
> An apple
> !消费者消费苹果完毕：消费者001
> !生产者生产苹果完毕：生产者001
> 生产者准备生产苹果：生产者001
> 消费者准备消费苹果：消费者001
> An apple
> !消费者消费苹果完毕：消费者001
> !生产者生产苹果完毕：生产者002
> 生产者准备生产苹果：生产者002
> 消费者准备消费苹果：消费者001
> An apple
> !消费者消费苹果完毕：消费者001
> !生产者生产苹果完毕：生产者001
> 生产者准备生产苹果：生产者001
> 消费者准备消费苹果：消费者001
> An apple
> !生产者生产苹果完毕：生产者002
> !消费者消费苹果完毕：消费者001
> 生产者准备生产苹果：生产者002
> Consumer Interrupted
> Producer Interrupted
> Producer Interrupted


> 参考：
>
> https://blog.csdn.net/javazejian/article/details/77410889	
>
> https://www.cnblogs.com/leesf456/p/5539071.html
>
> <https://blog.csdn.net/dfskhgalshgkajghljgh/article/details/51363543>
>
> 《并发编程的艺术》



