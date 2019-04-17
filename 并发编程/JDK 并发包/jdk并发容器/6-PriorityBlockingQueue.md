## PriorityBlockingQueue

## 一：概述

　　PriorityBlockingQueue是带优先级的无界阻塞队列，每次出队都返回优先级最高的元素，是**二叉树最小堆的实现**，直接遍历队列元素是无序的。

　　PriorityBlockingQueue类似于ArrayBlockingQueue内部使用一个**独占锁来控制同时只有一个线程可以进行入队和出队**，另外前者只使用了一个notEmpty条件变量而没有notFull这是因为PriorityBlockingQueue是无界队列，当put时候永远不会处于await所以也不需要被唤醒。

　　PriorityBlockingQueue始终保证出队的元素是优先级最高的元素，并且可以定制优先级的规则，内部通过使用一个**二叉树最小堆算法来维护内部数组，这个数组是可扩容的**，当当前元素个数>=最大容量时候会通过算法扩容。值得注意的是为了避免在扩容操作时候其他线程不能进行出队操作，实现上使用了先释放锁，然后通过cas保证同时只有一个线程可以扩容成功。

特点：

1. PriorityQueue是基于优先堆的一个无界队列，这个优先队列中的元素可以默认自然排序或者通过提供的Comparator（比较器）在队列实例化的时排序。

2. 优先队列不允许空值，而且不支持non-comparable（不可比较）的对象，比如用户自定义的类。优先队列要求使用Java Comparable和Comparator接口给对象排序，并且在排序时会按照优先级处理其中的元素。

3. 优先队列的头是基于自然排序或者Comparator排序的最小元素。如果有多个对象拥有同样的排序，那么就可能随机地取其中任意一个。当我们获取队列时，返回队列的头对象。

4. 优先队列的大小是不受限制的，所以在put是永远不会被阻塞。但在创建时可以指定初始大小，当我们向优先队列增加元素的时候，队列大小会自动增加。

5. PriorityQueue是非线程安全的，所以Java提供了PriorityBlockingQueue（实现BlockingQueue接口）用于Java多线程环境。

## 二：PriorityBlockingQueue详解

类图结构：

![image-20190405150204397](/Users/codecarver/Library/Application Support/typora-user-images/image-20190405150204397.png)

### 1.构造函数

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">构造函数
</blockquote>

```java
//默认容量
private static final int DEFAULT_INITIAL_CAPACITY = 11;

public PriorityBlockingQueue() {
  this(DEFAULT_INITIAL_CAPACITY, null);
}
//初始容量
public PriorityBlockingQueue(int initialCapacity) {
  this(initialCapacity, null);
}

// 初始容量和比较器
public PriorityBlockingQueue(int initialCapacity,
                             Comparator<? super E> comparator) {
  if (initialCapacity < 1)
    throw new IllegalArgumentException();
  this.lock = new ReentrantLock();
  this.notEmpty = lock.newCondition();
  this.comparator = comparator;
  this.queue = new Object[initialCapacity];
}

```

-----

### 2.类基本属性

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">类基本属性
</blockquote>

```java
    //存放元素
    private transient Object[] queue;

    /**
     * 用来存放队列元素个数
     */
    private transient int size;

    /**
     * 优先级队列所以有个比较器comparator用来比较元素大小。
     */
    private transient Comparator<? super E> comparator;

    /**
     * 用来在扩容队列时候做cas的
     目的是保证只有一个线程可以进行扩容。
     */
    private transient volatile int allocationSpinLock;

    //用于序列化
    private PriorityQueue<E> q;
```

-----

### 3.入队

增加元素有多种方法add(),offer(),put()，其中add(),put()方法调用offer()方法。offer()方法是无阻塞的。在队列插入一个元素，由于是无界队列，所以一直为成功返回true;    


```java
public boolean add(E e) {        
        return offer(e);    
    }
    
    public void put(E e) {
        offer(e); // never need to block
    }

    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e); // never need to block
    }

    public boolean offer(E e) {
        if (e == null)
            throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        int n, cap;
        Object[] array;
        //如果当前元素个数>=队列容量，则扩容(1)
        while ((n = size) >= (cap = (array = queue).length))
            tryGrow(array, cap);
        try {
            Comparator<? super E> cmp = comparator;
　　　　　　　//默认比较器为null(2)
            if (cmp == null)
                siftUpComparable(n, e, array);
            else
                 //自定义比较器(3)
                siftUpUsingComparator(n, e, array, cmp);
            //队列元素增加1，并且激活notEmpty的条件队列里面的一个阻塞线程(4)
            size = n + 1;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        return true;
    }
```

主流程比较简单，下面看看tryGrow函数，采用cas控制扩容。


```java
private void tryGrow(Object[] array, int oldCap) {
    lock.unlock(); //must release and then re-acquire main lock
    Object[] newArray = null;
 
    //cas成功则扩容(4)
    if (allocationSpinLock == 0 &&
        UNSAFE.compareAndSwapInt(this, allocationSpinLockOffset,
                                 0, 1)) {
        try {
            //oldGap<64则扩容新增oldcap+2,否者扩容50%，并且最大为MAX_ARRAY_SIZE
            int newCap = oldCap + ((oldCap < 64) ?
                                   (oldCap + 2) : // grow faster if small
                                   (oldCap >> 1));
            if (newCap - MAX_ARRAY_SIZE > 0) {    // possible overflow
                int minCap = oldCap + 1;
                if (minCap < 0 || minCap > MAX_ARRAY_SIZE)
                    throw new OutOfMemoryError();
                newCap = MAX_ARRAY_SIZE;
            }
            if (newCap > oldCap && queue == array)
                newArray = new Object[newCap];
        } finally {
            allocationSpinLock = 0;
        }
    }
 
    //第一个线程cas成功后，第二个线程会进入这个地方，然后第二个线程让出cpu，尽量让第一个线程执行下面点获取锁，但是这得不到肯定的保证。(5)
    if (newArray == null) // back off if another thread is allocating
        Thread.yield();
    lock.lock();(6)
    if (newArray != null && queue == array) {
        queue = newArray;
        System.arraycopy(array, 0, newArray, 0, oldCap);
    }
}
```

tryGrow目的是扩容，这里要思考下为啥在扩容前要先释放锁，然后使用cas控制只有一个线程可以扩容成功。我的理解是为了性能，因为扩容时候是需要花时间的，如果这些操作时候还占用锁那么其他线程在这个时候是不能进行出队操作的，也不能进行入队操作，这大大降低了并发性。

所以在扩容前释放锁，这允许其他出队线程可以进行出队操作，但是由于释放了锁，所以也允许在扩容时候进行入队操作，这就会导致多个线程进行扩容会出现问题，所以这里使用了一个spinlock用cas控制只有一个线程可以进行扩容，失败的线程调用Thread.yield()让出cpu，目的意在让扩容线程扩容后优先调用lock.lock重新获取锁，但是这得不到一定的保证，有可能调用Thread.yield()的线程先获取了锁。

那copy元素数据到新数组为啥放到获取锁后面那?原因应该是因为可见性问题，因为queue并没有被volatile修饰。另外有可能在扩容时候进行了出队操作，如果直接拷贝可能看到的数组元素不是最新的。而通过调用Lock后，获取的数组则是最新的，并且在释放锁前 数组内容不会变化。

具体建堆算法：

```java
private static <T> void siftUpComparable(int k, T x, Object[] array) {
    Comparable<? super T> key = (Comparable<? super T>) x;
 
    //队列元素个数>0则判断插入位置，否者直接入队(7)
    while (k > 0) {
        int parent = (k - 1) >>> 1;
        Object e = array[parent];
        if (key.compareTo((T) e) >= 0)
            break;
        array[k] = e;
        k = parent;
    }
    array[k] = key;(8)
}
```

----

### 4.出队

poll()在队列头部获取并移除一个元素，如果队列为空，则返回 null  

take()获取队列头元素，如果队列为空则阻塞。

```java
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return dequeue();
    } finally {
        lock.unlock();
    }
}

public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    E result;
    try {
 
        //如果队列为空，则阻塞，把当前线程放入notEmpty的条件队列
        while ( (result = dequeue()) == null)
            notEmpty.await();
    } finally {
        lock.unlock();
    }
    return result;
}

private E dequeue() {
 
    //队列为空，则返回null
    int n = size - 1;
    if (n < 0)
        return null;
    else {
 
 
        //获取队头元素(1)
        Object[] array = queue;
        E result = (E) array[0];
 
        //获取对尾元素，并值null(2)
        E x = (E) array[n];
        array[n] = null;
 
        Comparator<? super E> cmp = comparator;
        if (cmp == null)//cmp=null则调用这个，把对尾元素位置插入到0位置，并且调整堆为最小堆(3)
            siftDownComparable(0, x, array, n);
        else
            siftDownUsingComparator(0, x, array, n, cmp);
        size = n;（4）
        return result;
    }
}

private static <T> void siftDownComparable(int k, T x, Object[] array,
                                            int n) {
     if (n > 0) {
         Comparable<? super T> key = (Comparable<? super T>)x;
         int half = n >>> 1;           // loop while a non-leaf
         while (k < half) {
             int child = (k << 1) + 1; // assume left child is least
             Object c = array[child];（5）
             int right = child + 1;（6)
             if (right < n &&
                 ((Comparable<? super T>) c).compareTo((T) array[right]) > 0)(7)
                 c = array[child = right];
             if (key.compareTo((T) c) <= 0)(8)
                 break;
             array[k] = c;
             k = child;
         }
         array[k] = key;(9)
     }
 }
```

### 5.size()

获取队列元个数，由于加了独占锁所以返回结果是精确的.

```java
public int size() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return size;
    } finally {
        lock.unlock();
    }
}
```

## 三：简单使用

```java
public class PriorityBlockingQueueDemo {

    public static PriorityBlockingQueue<User> queue = new PriorityBlockingQueue<User>();

    public static void main(String[] args) {
        queue.add(new User(1,"wu"));
        queue.add(new User(5,"wu5"));
        queue.add(new User(23,"wu23"));
        queue.add(new User(55,"wu55"));
        queue.add(new User(9,"wu9"));
        queue.add(new User(3,"wu3"));
        for (User user : queue) {
            try {
                System.out.println(queue.take().name);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //静态内部类
    static class User implements Comparable<User>{

        public User(int age,String name) {
            this.age = age;
            this.name = name;
        }

        int age;
        String name;

        @Override
        public int compareTo(User o) {
            return this.age > o.age ? -1 : 1;
        }
    }
}
```



运行结果：

> wu55
> wu23
> wu9
> wu5
> wu3
> wu

PriorityBlockingQueue类是JDK提供的优先级队列 本身是线程安全的 内部使用显示锁 保证线程安全。

PriorityBlockingQueue存储的对象必须是实现Comparable接口的 因为PriorityBlockingQueue队列会根据内部存储的每一个元素的compareTo方法比较每个元素的大小。

这样在take出来的时候会根据优先级 将优先级最小的最先取出 。


