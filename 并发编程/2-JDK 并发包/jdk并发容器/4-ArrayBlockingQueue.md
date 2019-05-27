# ArrayBlockingQueue

[TOC]

## 一：概述

**ArrayBlockingQueue**类是由数组支持的`有界阻塞`队列。通过有界，它意味着`队列的大小是固定的`。创建后，`无法更改容量`。尝试将元素放入完整队列将导致操作阻塞。同样，也会阻止从空队列中获取元素的尝试。最初可以通过将容量作为ArrayBlockingQueue的构造函数中的参数传递来实现ArrayBlockingQueue的绑定。此队列命令元素**FIFO（先进先出）**。新插入的元素始终插入队列的尾部，队列检索操作获取队列头部的元素

## 二：ArrayBlockingQueue详解

### 1.构造函数

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">构造函数
</blockquote>

```java
// 使用给定（固定）容量和默认访问策略创建ArrayBlockingQueue。
public ArrayBlockingQueue(int capacity) {
}

// 使用给定（固定）容量和指定的访问策略创建ArrayBlockingQueue。如果fair为真，那么在插入或删除时阻塞的线程的队列访问将按FIFO顺序处理; 如果为false，则未指定访问顺序。
public ArrayBlockingQueue(int capacity, boolean fair) {
    // 初始容量必须大于0
        if (capacity <= 0)
            throw new IllegalArgumentException();
        // 初始化数组
        this.items = new Object[capacity];
        // 初始化可重入锁
        lock = new ReentrantLock(fair);
        // 初始化等待条件
        notEmpty = lock.newCondition();
        notFull =  lock.newCondition();
}

//创建一个具有给定（固定）容量的ArrayBlockingQueue，指定的访问策略，最初包含给定集合的元素，以集合迭代器的遍历顺序添加。如果fair为真，那么在插入或删除时阻塞的线程的队列访问将按FIFO顺序处理; 如果为false，则未指定访问顺序。
public ArrayBlockingQueue(int capacity, boolean fair,
                          Collection<? extends E> c) {
    // 调用两个参数的构造函数
        this(capacity, fair);
        // 可重入锁
        final ReentrantLock lock = this.lock;
        // 上锁
        lock.lock(); // Lock only for visibility, not mutual exclusion
        try {
            int i = 0;
            try {
                for (E e : c) { // 遍历集合
                    // 检查元素是否为空
                    checkNotNull(e);
                    // 存入ArrayBlockingQueue中
                    items[i++] = e;
                }
            } catch (ArrayIndexOutOfBoundsException ex) { // 当初始化容量小于传入集合的大小时，会抛出异常
                throw new IllegalArgumentException();
            }
            // 元素数量
            count = i;
            // 初始化存元素的索引
            putIndex = (i == capacity) ? 0 : i;
        } finally {
            // 释放锁
            lock.unlock();
        }
}
```

-----

### 2.类基本属性

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">类基本属性
</blockquote>

```java
public class ArrayBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    // 存放实际元素的数组
    final Object[] items;
    // 取元素索引
    int takeIndex;
    // 获取元素索引
    int putIndex;
    // 队列中的项
    int count;
    // 可重入锁
    final ReentrantLock lock;
    // 等待获取条件
    private final Condition notEmpty;
    // 等待存放条件
    private final Condition notFull;
    // 迭代器
    transient Itrs itrs = null;
}
```

------

### 3.put函数解析

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">put函数解析
</blockquote>

```java
   public void put(E e) throws InterruptedException {
        checkNotNull(e);
        // 获取重入锁 
        final ReentrantLock lock = this.lock;
       /* 1.如果当前线程未被中断，则获取锁。  
          2.如果该锁没有被另一个线程保持，则获取该锁并立即返回，将锁的保持计数设置为 1。
          3.如果当前线程已经保持此锁，则将保持计数加 1，并且该方法立即返回。 
          4.如果锁被另一个线程保持，则出于线程调度目的，禁用当前线程，并且在发生以下两种情况之一以
前，该线程将一直处于休眠状态： 
     		1）锁由当前线程获得；或者 
    	    2）其他某个线程中断当前线程。 
		  5.如果当前线程获得该锁，则将锁保持计数设置为 1。 
   如果当前线程： 
       		1）在进入此方法时已经设置了该线程的中断状态；或者 
       		2）在等待获取锁的同时被中断。 
   则抛出 InterruptedException，并且清除当前线程的已中断状态。
   */
       lock.lockInterruptibly();
        try {
            // // 判断元素是否已满
            while (count == items.length)
                // // 若满，则等待
                notFull.await();
            // // 入队列
            enqueue(e);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
```

put函数用于存放元素，当线程被中断时会抛出异常，如果队列已满则会一直阻塞等待，当队列有空余位置，则enqueue：

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">enqueue源码
</blockquote>

```java
private void enqueue(E x) {
        // assert lock.getHoldCount() == 1;
        // assert items[putIndex] == null;
    	// 获取数组
        final Object[] items = this.items;
        // 放入元素
        items[putIndex] = x;
    	// 如果放入后存元素的索引等于数组长度（表示已满）
        if (++putIndex == items.length)
            // 重置索引为0
            putIndex = 0;
        // 元素数量加1
        count++;
        // 唤醒在notEmpty(等待获取)条件上等待的线程
        notEmpty.signal();
    }
```

-----

### 4.offer函数解析

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">offer函数解析
</blockquote>

```java
public boolean offer(E e) {
    checkNotNull(e);
    // 可重入锁
    final ReentrantLock lock = this.lock;
    // 获取锁
    lock.lock();
    try {
        //元素个数等于数组长度，则返回
        if (count == items.length)
            return false;
        else {
            //入队
            enqueue(e);
            return true;
        }
    } finally {
        // 释放锁
        lock.unlock();
    }
}
```

offer函数也用于存放元素，在调用ArrayBlockingQueue的add方法时，会间接的调用到offer函数（super.add()方法里面调用的就是offer方法），offer函数添加元素`不会抛出异常`，当底层Object数组已满时，则返回false，否则，会调用enqueue函数(这部分逻辑同put函数)，将元素存入底层Object数组。并唤醒等待notEmpty条件的线程。

----

### 5.take函数解析

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">take函数解析
</blockquote>

```java
public E take() throws InterruptedException {
    // 可重入锁
    final ReentrantLock lock = this.lock;
    // 当前线程未被中断，则获取锁，中断会抛出异常 同put函数
    lock.lockInterruptibly();
    try {
        // 元素数量为0，即Object数组为空
        while (count == 0)
            // 等待，直到有元素
            notEmpty.await();
        // 出队列
        return dequeue();
    } finally {
        // 释放锁
        lock.unlock();
    }
}
```

take函数用于从ArrayBlockingQueue中获取一个元素，其与put函数相对应，在当前线程被中断时会抛出异常，并且当队列为空时，会阻塞一直等待，直到队列有元素。出队过程主要由 Dequeue 函数完成：

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">Dequeue 函数解析
</blockquote>

```java
private E dequeue() {
        // assert lock.getHoldCount() == 1;
        // assert items[takeIndex] != null;
        final Object[] items = this.items;
        @SuppressWarnings("unchecked")
    	// 根据takeIndex获取元素
        E x = (E) items[takeIndex];
    	// 取出元素之后，将此索引的值赋值为null
        items[takeIndex] = null;
        if (++takeIndex == items.length)
            // 重新赋值取值索引
            takeIndex = 0;
        // 元素个数减1
        count--;
        if (itrs != null)
            itrs.elementDequeued();
    	// 唤醒在notFull条件上等待的线程
        notFull.signal();
        return x;
    }
```

-----

### 6.poll函数

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">poll 函数解析
</blockquote>

```java
public E poll() {
    // 重入锁
    final ReentrantLock lock = this.lock;
    // 获取锁
    lock.lock();
    try {
        // 若元素个数为0则返回null，否则，调用dequeue，出队列
        return (count == 0) ? null : dequeue();
    } finally {
        lock.unlock();
    }
}
```

poll函数用于获取元素，其与offer函数相对应，不会抛出异常，当元素个数为0是，返回null，否则，调用dequeue函数，并唤醒等待notFull条件的线程。并返回。

-----

### 7.clear函数

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">clear 函数解析
</blockquote>

```java
public void clear() {
    // 数组
    final Object[] items = this.items;
    // 重入锁
    final ReentrantLock lock = this.lock;
    // 获取锁
    lock.lock();
    try {
        // 元素的个数
        int k = count;
        if (k > 0) {// 元素个数大于0
            // 存元素索引
            final int putIndex = this.putIndex;
            // 取元素索引
            int i = takeIndex;
            do {
                // 取元素同时将索引赋值为null
                items[i] = null;
                if (++i == items.length)
                    i = 0;
            } while (i != putIndex);
            takeIndex = putIndex;
            count = 0;
            if (itrs != null)
                itrs.queueIsEmpty();
            for (; k > 0 && lock.hasWaiters(notFull); k--)
                notFull.signal();
        }
    } finally {
        lock.unlock();
    }
}
```

## 三：简单使用

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">① add方法 
</blockquote>


```java
public class ArrayBlockingQueueDemo1 {
    public static void main(String[] args) {
        // define capacity of ArrayBlockingQueue
        int capacity = 3;

        // create object of ArrayBlockingQueue
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(capacity);

        // Add element to ArrayBlockingQueue
        queue.add(1);

        System.out.println("After adding 1");
        System.out.println(queue);

        queue.add(2);
        queue.add(3);
        queue.add(4);
        System.out.println("After adding four element");
        System.out.println(queue);
    }
}
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">当添加元素少于等于3个时候，结果为：<br>After adding 1
[1]<br>
After adding four element<br>
[1, 2, 3]<br>当添加元素大于3个，运行结果异常：Exception in thread "main" java.lang.IllegalStateException: Queue full (实际调用的是AbstractQueue里的add方法)
</blockquote>
-----

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">② put方法 
</blockquote>

```java
public class ArrayBlockingQueueDemo2 {
    public static void main(String[] args)  {
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
        try {
            queue.put(1);
            queue.put(2);
            queue.put(3);
            queue.put(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("queue contains " + queue);
    }
}
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">当添加元素大于3时候，会阻塞当前线程，直到有空余的元素
</blockquote>
-------

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">③ take方法 
</blockquote>

```java
public class ArrayBlockingQueueDemo3 {
    public static void main(String[] args) throws InterruptedException {
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        System.out.println("After addding numbers Queue: " +queue);

        int head=queue.take();
        System.out.println("Head of queue removed is " +head);
        System.out.print("After removing head,Queue: ");
        System.out.println(queue);

        head = queue.take();

        System.out.println("Head of queue removed is " + head);
        System.out.print("After removing head Queue: ");
        System.out.println(queue);
    }
}
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">
After addding numbers Queue: [1, 2, 3]<br>
Head of queue removed is 1<br>
After removing head,Queue: [2, 3]<br>
Head of queue removed is 2<br>
After removing head Queue: [3]<br>
</blockquote>
-------

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">④ drainTo方法 
</blockquote>

```java
public class ArrayBlockingQueueDemo4 {
    public static void main(String[] args) throws InterruptedException {
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue(3);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        System.out.println("Before drainTo Operation");
        System.out.println("queue = " + queue);

        ArrayList<Integer> list = new ArrayList();

        queue.drainTo(list);
        System.out.println("After drainTo Operation");
        System.out.println("queue = " + queue);
        System.out.println("collection = " + list);
    }
}
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">
Before drainTo Operation<br>
queue = [1, 2, 3]<br>
After drainTo Operation<br>
queue = []<br>
collection = [1, 2, 3]<br>
</blockquote>
------

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">④ poll方法 
</blockquote>

```java
public class ArrayBlockingQueueDemo5 {
    public static void main(String[] args) throws InterruptedException {
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(5);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        System.out.println("Queue Contains" + queue);
        System.out.println("Removing From head: " + queue.poll());
        System.out.println("Queue Contains" + queue);
        System.out.println("Removing From head: " + queue.poll());
        System.out.println("Queue Contains" + queue);
        System.out.println("Removing From head: " + queue.poll());
        System.out.println("Queue Contains" + queue);
        System.out.println("Removing From head: " + queue.poll());
        System.out.println("Queue Contains" + queue);
    }
}
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">
Queue Contains[1, 2, 3]<br>
Removing From head: 1<br>
Queue Contains[2, 3]<br>
Removing From head: 2<br>
Queue Contains[3]<br>
Removing From head: 3<br>
Queue Contains[]<br>
Removing From head: null<br>
Queue Contains[]<br>
</blockquote>
----------

## 

