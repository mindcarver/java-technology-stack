# LinkedBlockingDequeue

[TOC]

## 一：简介

　　Deque是一个双端队列，deque(双端队列) 是 "Double Ended Queue" 的缩写。因此，双端队列是一个你可以从任意一端插入或者抽取元素的队列。实现了在队列头和队列尾的高效插入和移除。
　　BlockingDeque 类是一个双端队列，在不能够插入元素时，它将阻塞住试图插入元素的线程；在不能够抽取元素时，它将阻塞住试图抽取的线程。
　　正如阻塞队列使用与生产者-消费者模式，双端队列同样适用于另一种相关模式，即**工作密取**。在生产者-消费者设计中，所有消费者有一个共享的工作队列，而在工作密取设计中，每个消费者都有各自的双端队列。如果一个消费者完成了自己双端队列中的全部工作，那么它可以从其它消费者双端队列末尾秘密地获取工作。密取工作模式比传统的生产者-消费者模式具有更高的可伸缩性，这是因为工作者线程不会在单个共享的任务队列上发生竞争。在大多数时候，它们都只是访问自己的双端队列，从而极大地减少了竞争。当工作者线程需要访问另一个队列时，它会从队列的尾部而不是头部获取工作，因此进一步降低了队列上的竞争程度。

**LinkedBlockingDeque**是双向链表实现的双向并发阻塞队列。该阻塞队列同时支持FIFO和FILO两种操作方式，即可以从队列的头和尾同时操作(插入/删除)；并且，该阻塞队列是支持线程安全。
此外，LinkedBlockingDeque还是可选容量的(防止过度膨胀)，即可以指定队列的容量。如果不指定，默认容量大小等于Integer.MAX_VALUE。

## 二：类的继承

```java
public class LinkedBlockingDeque<E>
    extends AbstractQueue<E>
    implements BlockingDeque<E>, java.io.Serializable {}
```

## 三：数据结构

LinkedBlockingDeque的底层数据结构是一个双端队列，该队列使用链表实现，其源码和结构图如下：

```java
/** 双向链表节点 */  
static final class Node<E> {  
  /** 
     * 元素值 
     */  
  E item;  

  /** 
     * 节点前驱 
     * 1.指向前驱；2.指向this，说明前驱是尾节点，看unlinklast；3.指向null说明没有前驱 
     */  
  Node<E> prev;  

  /** 
     * 节点后继 
     * 1.指向后继；2.指向this,说明后继是头结点，看unlinkfirst；3.指向null说明没有后继 
     */  
  Node<E> next;  

  Node(E x) {  
    item = x;  
  }  
}
```



![image-20190407160357171](https://ws1.sinaimg.cn/large/006tNc79ly1g1u4qr9ngmj31ew0fwaav.jpg)

## 四：源码分析

### 1. 属性

```java
//队列的头节点
transient Node<E> first;
//队列的尾节点
transient Node<E> last;

//队列中元素的个数
private transient int count;

//队列中元素的最大个数
private final int capacity;

//锁
final ReentrantLock lock = new ReentrantLock();

//队列为空时，阻塞take线程的条件队列
private final Condition notEmpty = lock.newCondition();

//队列满时，阻塞put线程的条件队列
private final Condition notFull = lock.newCondition();
```

LinkedBlockingDeque内部只有一把锁以及该锁上关联的两个条件，所以可以推断同一时刻只有一个线程可以在队头或者队尾执行入队或出队操作。可以发现这点和LinkedBlockingQueue不同，LinkedBlockingQueue可以同时有两个线程在两端执行操作。

### 2.构造函数

```java
public LinkedBlockingDeque() {
  this(Integer.MAX_VALUE);
}


public LinkedBlockingDeque(int capacity) {
  if (capacity <= 0) throw new IllegalArgumentException();
  this.capacity = capacity;
}

public LinkedBlockingDeque(Collection<? extends E> c) {
  this(Integer.MAX_VALUE);
  final ReentrantLock lock = this.lock;
  lock.lock(); // Never contended, but necessary for visibility
  try {
    for (E e : c) {
      if (e == null)
        throw new NullPointerException();
      if (!linkLast(new Node<E>(e)))
        throw new IllegalStateException("Deque full");
    }
  } finally {
    lock.unlock();
  }
}

```

可以看到这三个构造方法的结构和LinkedBlockingQueue是相同的。 但是LinkedBlockingQueue是存在一个哨兵节点维持头节点的，而LinkedBlockingDeque中是没有的。

### 3.入队

#### add(E e)

将指定的元素插入到此队列的尾部，在成功时返回 true，如果此队列已满，则抛出 IllegalStateException

```java
public boolean add(E e) {
    addLast(e);
    return true;
}
```

```java
public void addLast(E e) {
    if (!offerLast(e))
        throw new IllegalStateException("Deque full");
}
```

```java
public boolean offerLast(E e) {
    //入队元素不能为空
    if (e == null) throw new NullPointerException();
    Node<E> node = new Node<E>(e);
    final ReentrantLock lock = this.lock;
    //操作队列，加锁
    lock.lock();
    try {
        return linkLast(node);
    } finally {
        //释放锁
        lock.unlock();
    }
}
```

```java
/**
 * Links node as last element, or returns false if full.
 */
private boolean linkLast(Node<E> node) {
    //超过最大容量了，操作失败
    if (count >= capacity)
        return false;
    Node<E> l = last;
    node.prev = l;
    last = node;
    //如果队头尾空，赋值给队头
    if (first == null)
        first = node;
    else
        l.next = node; //连接至队列尾
    ++count; //元素数量增加
    notEmpty.signal();//队列非空条件满足，唤醒阻塞在队列空上的一个线程
    return true;
}
```

####offer(E e) 

将指定的元素插入到此队列的尾部，在成功时返回 true，如果此队列已满，则抛出 IllegalStateException.
```java
public boolean offer(E e) {
    return offerLast(e);
}
```

```java
public boolean offerLast(E e) {
     if (e == null) throw new NullPointerException();
     Node<E> node = new Node<E>(e);
     final ReentrantLock lock = this.lock;
     //操作队列，加锁
     lock.lock();
     try {
         return linkLast(node);
     } finally {
         lock.unlock();
     }
 }
```

#### offer(E e, long timeout, TimeUnit unit) 

将指定的元素插入此队列的尾部，如果该队列已满，则在到达指定的等待时间之前等待（如果发生中断，则抛出中断异常）

```java
public boolean offer(E e, long timeout, TimeUnit unit)
    throws InterruptedException {
    return offerLast(e, timeout, unit);
}
```

```java
public boolean offerLast(E e, long timeout, TimeUnit unit)
    throws InterruptedException {
    if (e == null) throw new NullPointerException();
    Node<E> node = new Node<E>(e);
    //纳秒数
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    //可中断的 加锁
    lock.lockInterruptibly();
    try {
        //如果添加节点至队列末尾失败，则队列满了
        while (!linkLast(node)) {
            if (nanos <= 0) //如果超时时间到了，则返回false
                return false;
            nanos = notFull.awaitNanos(nanos);// 阻塞等待，等待nanos指定的纳秒数
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```

#### **put(E e)** 

将指定的元素插入此队列的尾部，如果该队列已满，则阻塞等待。

```java
public void put(E e) throws InterruptedException {
    putLast(e);
}
```

```java
public void putLast(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    Node<E> node = new Node<E>(e);
    final ReentrantLock lock = this.lock;
    //操作队列，加锁
    lock.lock();
    try {
        //如果添加到队列末尾失败，则说明队列已经满了
        while (!linkLast(node))
            notFull.await(); //notFull条件不满足，阻塞在该条件上
    } finally {
        //释放锁
        lock.unlock();
    }
}
```

#### push(E e) 

将元素添加至队列头部

```java
public void push(E e) {
    addFirst(e);
}	
```

```java
public void addFirst(E e) {
    if (!offerFirst(e))
        throw new IllegalStateException("Deque full");
}
```

```java
public boolean offerFirst(E e) {
    if (e == null) throw new NullPointerException();
    Node<E> node = new Node<E>(e);
    final ReentrantLock lock = this.lock;
    //加锁
    lock.lock();
    try {
        return linkFirst(node);
    } finally {
        lock.unlock();
    }
}
```

```java
//将元素添加到队列头部
private boolean linkFirst(Node<E> node) {
    // assert lock.isHeldByCurrentThread();
    if (count >= capacity)
        return false;
    Node<E> f = first;
    node.next = f; //设置后继
    first = node;
    if (last == null)
        last = node;
    else
        f.prev = node; //设置前驱
    ++count;
    notEmpty.signal(); //notEmpty 条件满足，唤醒阻塞在队列为空条件上的一个线程
    return true;
}
```

----

### 4.出队

#### poll() 

获取并移除此队列的头，如果此队列为空，则返回 null

```java
public E poll() {
    return pollFirst();
}
```

```java
public E pollFirst() {
    final ReentrantLock lock = this.lock;
    //加锁
    lock.lock();
    try {
        return unlinkFirst(); //移除并返回对头
    } finally {
        lock.unlock();
    }
}
```

```java
/**
 * Removes and returns first element, or null if empty.
 */
private E unlinkFirst() {
    // assert lock.isHeldByCurrentThread();
    Node<E> f = first;
    if (f == null)
        return null; //队列为空，返回null
    Node<E> n = f.next;
    E item = f.item;
    f.item = null;
    f.next = f; // help GC
    first = n;
    if (n == null) //出队后，队列为空
        last = null;
    else
        n.prev = null; //清空新head的前驱指针
    --count;
    notFull.signal(); //现在队列notFull条件满足，唤醒阻塞在队列满了上的一个线程
    return item;
}
```

#### poll(long timeout, TimeUnit unit) 

获取并移除此队列的头部，在指定的等待时间前等待（如果发生中断，则抛出中断异常）

```java
public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    return pollFirst(timeout, unit);
}
```

```java
public E pollFirst(long timeout, TimeUnit unit)
    throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    //可中断的加锁
    lock.lockInterruptibly();
    try {
        E x;
        //如果移除节点失败，说明队列为空
        while ( (x = unlinkFirst()) == null) {
            if (nanos <= 0) //超时时间到，返回null
                return null;
            nanos = notEmpty.awaitNanos(nanos); //在notEmpty 条件上 阻塞等待nanos 纳秒
        }
        return x;
    } finally {
        lock.unlock();
    }
}
```

#### take()

获取并移除此队列的头部，在元素变得可用之前一直等待

```java
public E take() throws InterruptedException {
    return takeFirst();
}
```

```java
public E takeFirst() throws InterruptedException {
   final ReentrantLock lock = this.lock;
   lock.lock();
   try {
       E x;
       //移除元素失败，说明队列为空
       while ( (x = unlinkFirst()) == null)
           notEmpty.await();//在notEmpty 条件阻塞等待
       return x;
   } finally {
       lock.unlock();
   }
}
```

#### remove

移除并返回队头，如果队列为空，则会抛出NoSuchElementException异常

````java
public E remove() {
    return removeFirst();
}
````

```java
public E removeFirst() {
    E x = pollFirst();
    if (x == null) throw new NoSuchElementException();
    return x;
}
```

#### pop()

移除并返回队头元素

```java
public E pop() {
    return removeFirst();
}
```

####takeLast()

取队尾元素，如果队列为空，则阻塞等待，直到队列不空，或者发生中断异常

```java
public E takeLast() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        E x;
        //如果队列为空，则等待
        while ( (x = unlinkLast()) == null)
            notEmpty.await(); //在notEmpty 条件上阻塞等待
        return x;
    } finally {
        lock.unlock();
    }
}
```

#### pollLast() 

取出队列末尾元素，如果队列为空，则返回null

```java
public E takeLast() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        E x;
        //如果队列为空，则等待
        while ( (x = unlinkLast()) == null)
            notEmpty.await(); //在notEmpty 条件上阻塞等待
        return x;
    } finally {
        lock.unlock();
    }
}
```

###5.序列化

队列头尾指针都是被transient关键字修饰，因此必须自己手动来实现序列化工作。

```java
public E takeLast() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        E x;
        //如果队列为空，则等待
        while ( (x = unlinkLast()) == null)
            notEmpty.await(); //在notEmpty 条件上阻塞等待
        return x;
    } finally {
        lock.unlock();
    }
}
```

---

## 五：总结

LinkedBlockingDeque 内部使用了可重入锁（线程安全），不像SynchronousQueue使用的循环cas，因此很简单，出队和入队使用的是同一个锁，但是两头都可以操作队列，相对于单端队列可以减少一半的竞争。 
LinkedBlockingDeque 同其他阻塞队列一样，不能存储null值元素。

LinkedBlockingDeque 和LinkedBlockingQueue 实际上是差不多的，因此可以结合着来看 
