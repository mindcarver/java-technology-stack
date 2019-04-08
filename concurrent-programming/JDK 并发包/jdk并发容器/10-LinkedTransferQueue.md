# LinkedTransferQueue

## 一：前言

此队列也是基于链表的，对于所有给定的生产者都是先入先出的。注意，该队列的size方法和ConcurrentLinkedQueue一样不是常量时间。由于队列的实现，其需要遍历队列才能计算出队列的大小，这期间队列发生的改变，遍历的结果会不正确。bulk操作并不保证原子性，比如迭代器迭代的时候执行addAll()方法，迭代器可能只能看到部分新加的元素。

## 二：继承体系

![image-20190407162735237](https://ws1.sinaimg.cn/large/006tNc79ly1g1u5fda8zij30qw0oi76v.jpg)

对比前面的阻塞队列，会发现LinkedTransferQueue 的继承体系有特殊之处。前面的阻塞队列都直接实现的BlockingQueue接口，在LinkedTransferQueue 却多了一个TransferQueue 接口，而该接口继承至BlockingQueue。 
BlockingQueue 接口代表的是普通的阻塞队列，TransferQueue 则代表的是另一种特殊阻塞队列，它是指这样的一个队列：当生产者向队列添加元素但队列已满时，生产者会被阻塞；当消费者从队列移除元素但队列为空时，消费者会被阻塞。

前面我们分析的SynchronousQueue 不就是有这种特性吗，但是SynchronousQueue 并没有实现TransferQueue 接口，原因就在于TransferQueue 接口也是在jdk 1.7才出现的，应该是为了和前面的阻塞队列进行区分，同时为了后面扩充这种特殊的阻塞队列，才加入了TransferQueue ，这样功能才不至于混乱（单一职能原则）。

## 三：数据结构

首先来看看队列的节点定义：

```java
static final class Node {
     final boolean isData;   // 指示的是item 是否为数据
     volatile Object item;   // 数据域
     volatile Node next;     //后继指针
     volatile Thread waiter; // 等待线程
     final boolean casNext(Node cmp, Node val) {
         return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
     }
     final boolean casItem(Object cmp, Object val) {
         // assert cmp == null || cmp.getClass() != Node.class;
         return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
     }
     Node(Object item, boolean isData) {
         UNSAFE.putObject(this, itemOffset, item); // relaxed write
         this.isData = isData;
     }
     ...
}
```

数据形式：

![image-20190407163301472](https://ws2.sinaimg.cn/large/006tNc79ly1g1u5l05fv4j310o0g0mzo.jpg)

**unmatched node**：未被匹配的节点。可能是一个生产者节点（item不为null），也可能是一个消费者节点（item为null）。
 **matched node**：已经被匹配的节点。可能是一个生产者节点（item不为null）的数据已经被一个消费者拿走；也可能是一个消费者节点（item为null）已经被一个生产者填充上数据。

## 四：源码分析

### 1.核心属性

```java
//队列头节点，第一次入列之前为空
transient volatile Node head;

//队列尾节点，第一次添加节点之前为空
private transient volatile Node tail;

//累计到一定次数再清除无效node
private transient volatile int sweepVotes;

//当一个节点是队列中的第一个waiter时，在多处理器上进行自旋的次数(随机穿插调用thread.yield)
private static final int FRONT_SPINS   = 1 << 7;

// 当前继节点正在处理，当前节点在阻塞之前的自旋次数，也为FRONT_SPINS
// 的位变化充当增量，也可在自旋时作为yield的平均频率
private static final int CHAINED_SPINS = FRONT_SPINS >>> 1;

//sweepVotes的阀值
static final int SWEEP_THRESHOLD = 32;
/*
 * Possible values for "how" argument in xfer method.
 * xfer方法类型
 */
private static final int NOW   = 0; // for untimed poll, tryTransfer
private static final int ASYNC = 1; // for offer, put, add
private static final int SYNC  = 2; // for transfer, take
private static final int TIMED = 3; // for timed poll, tryTransfer
```

### 2.入队

LinkedTransferQueue提供了add、put、offer三类方法，用于将元素放到队列中。 
注意我们这里所说的入队操作是指add,put,offer这几个方法，而不是指真正的把节点入队的操作，因为LinkedTransferQueue 中针对的不是数据，而是操作，操作可能需要入队，而这个操作可能是放数据操作，也可能是取数据操作，这里注意区分一下，不要搞混了。
```java
    public void put(E e) {
        xfer(e, true, ASYNC, 0);
    }

    public boolean offer(E e, long timeout, TimeUnit unit) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    public boolean offer(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    public boolean add(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }
```

可以看到，这几个入队方法，都调用的是同一个方法xfer，LinkedTransferQueue 是一个由链表组成的无界队列，因此不会有容量限制（一定范围内），因此这里入队的操作都不会阻塞（因此超时入队方法实际也没有用），也就是说，入队后线程会立即返回，这个是参数ASYNC的作用。

#### xfer 方法

LinkedTransferQueue和SynchronousQueue 是一样的，队列中主要的不是针对数据，而且操作（put或take,注意这里put，take 指的是放入数据和取数据），队列中既可以存储入队操作，也可以存储出队操作，当队列为空时，如果有线程进行出队操作，那么这个时候队列是没有数据的，那么这个操作就会被入队，同时线程也会阻塞，直到数据的到来（或出现异常），如果最开始队列为空，放入数据的操作到来，那么数据就会被放到队列中，此后如果取数据操作到来，那么就会从队列中取出数据，因此可以知道队列中存放的都是一系列相同的操作（put(放数据操作)或take(取数据操作)）。

这里我们说的是放数据操作，那么如果队列为空，那么直接将数据入队即可，同时因为是无界队列，线程不会阻塞，直接返回，如果队列不为空，那么队列里面可能有两种情况：（1）存放的都是数据（2）存放的都是取数据操作 
如果是情况1：那么本次操作的和队列中的节点的操作是一样的，因此直接把数据放到队列末尾，线程返回。 
如果是情况2：那么本次操作和队列中的节点的操作是不一样的（也就是匹配的,放入数据操作和取数据操作是匹配的，也就是不同的操作是匹配的，相同的操作是不匹配的），那么就把队头的节点出队，把本次的数据给队头节点，同时唤醒该节点的线程。

```java

 private E xfer(E e, boolean haveData, int how, long nanos) {
     if (haveData && (e == null))
         throw new NullPointerException();
     Node s = null;                        // the node to append, if needed
     retry:
     for (;;) {                            // restart on append race
         // 遍历队列，看看有没有匹配的操作
         for (Node h = head, p = h; p != null;) { // find & match first node
             boolean isData = p.isData;
             Object item = p.item;
             //正常的操作节点，item等于本身则表示该操作被取消了或者匹配过了
             if (item != p && (item != null) == isData) { 
                 // 该节点的操作和本次操作不匹配，那么整个队列都将不匹配，break出来
                 if (isData == haveData)   
                     break;
                 // 该节点的操作和本次操作是匹配的，设置节点item域为本次操作数据域e
                 if (p.casItem(item, e)) { 
                     /**
                     *队列中的操作都是一样的，和节点p匹配成功，但是p不是head，那么p之前的节点都失效了（被匹配过了）
                     *但是还没有将这些失效节点移除队列，因此这里会帮忙做这个工作
                     */
                     for (Node q = p; q != h;) {
                         Node n = q.next;  // update by 2 unless singleton
                         //设置新head
                         if (head == h && casHead(h, n == null ? q : n)) {
                             h.forgetNext(); //head 成环，移除队列，方便gc
                             break;
                         } 
                         /**
                         *如果head 为null，则跳出来
                         *如果只有head，那么也跳出来（开始进入这个循环时，p在head 后面）
                         *如果q已经被匹配过了，跳出来
                         */                
                         if ((h = head)   == null ||
                             (q = h.next) == null || !q.isMatched())
                             break;        // unless slack < 2
                     }
                     //唤醒操作节点p的线程
                     LockSupport.unpark(p.waiter);

                     /**
                      *返回操作节点p的item(如果操作节点p 是take 那么item 就是null
                      *如果是put，那么本次就是take,返回的就是数据)
                     */
                     return LinkedTransferQueue.<E>cast(item);
                 }
             }
             // 节点p 不正常了（该操作被取消了）
             Node n = p.next;
             //如果p 成环了，那么重新从head 开始遍历（成环了表示节点p已经完成了匹配了）
             p = (p != n) ? n : (h = head); // Use head if p offlist
         }
         //本次操作和队列中的节点操作是一直的，那么就将该操作入队，如果how 是NOW 则直接返回，不入队
         if (how != NOW) {                 // No matches available
             if (s == null)
                 s = new Node(e, haveData); //生成节点
             //将节点入队，同时返回其全驱    
             Node pred = tryAppend(s, haveData);
             if (pred == null)
                 continue retry;           // lost race vs opposite mode
             //如果how 是ASYNC 那么就不阻塞，直接返回
             if (how != ASYNC)
                 //否则进行自旋然后阻塞（如果设置了超时，则进行超时等待）
                 return awaitMatch(s, pred, e, (how == TIMED), nanos);
         }
         return e; // not waiting
     }
 }
```

逻辑如下:
找到 `head` 节点,如果 `head` 节点是匹配的操作,就直接赋值,如果不是,添加到队列中。

注意：队列中永远只有一种类型的操作,要么是 `put` 类型, 要么是 `take` 类型.

整个过程如下图：

![image-20190407163809433](https://ws1.sinaimg.cn/large/006tNc79ly1g1u5qdes6pj31420p8dpp.jpg)

相比较 `SynchronousQueue` 多了一个可以存储的队列，相比较 `LinkedBlockingQueue` 多了直接传递元素，少了用锁来同步。

性能更高，用处更大。

### 3.出队

LinkedTransferQueue提供了poll、take方法用于出列元素

```java
    public E take() throws InterruptedException {
        //这里的参数又ASYNC 变成了SYNC
        E e = xfer(null, false, SYNC, 0);
        if (e != null)
            return e;
        Thread.interrupted();
        throw new InterruptedException();
    }

    public E poll() {
        //参数为NOW，如果取不到数据，该操作不会入队阻塞等待，而是直接返回
        return xfer(null, false, NOW, 0);
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        //具有超时等待的操作
        E e = xfer(null, false, TIMED, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted())
            return e;
        throw new InterruptedException();
    }
```

这里再次强调，这里的出队操作，指的是poll、take方法，而不是真正指的是出队操作，因为poll、take操作也可能会入队（队列针对的是操作，不是数据）。

通过上面一系列的方法，我们看到，其实poll、take内部调用的仍然是xfer 方法，因为是取数据，因此参数部分发生了变化，这个注意一下。

```java
 /**
  * Implements all queuing methods. See above for explanation.
  *
  * @param e the item or null for take
  * @param haveData true if this is a put, else a take
  * @param how NOW, ASYNC, SYNC, or TIMED
  * @param nanos timeout in nanosecs, used only if mode is TIMED
  * @return an item if matched, else e
  * @throws NullPointerException if haveData mode but e is null
  */
 private E xfer(E e, boolean haveData, int how, long nanos) {
     if (haveData && (e == null))
         throw new NullPointerException();
     Node s = null;                        // the node to append, if needed
     retry:
     for (;;) {                            // restart on append race
         //这里是取数据操作，那么遍历队列看看有没有匹配的操作（即放数据操作）
         for (Node h = head, p = h; p != null;) { // find & match first node
             boolean isData = p.isData;
             Object item = p.item; 
             if (item != p && (item != null) == isData) { // unmatched
                 if (isData == haveData)   // can't match
                     break;
                 /**
                 *队列里面确实都是放数据的操作，则和当前操作是匹配的
                 *设置匹配操作节点的item域为null (e为null，原本item 域是数据)
                 */
                 if (p.casItem(item, e)) { // match
                     //协助推进head,这个和上面是一样的
                     for (Node q = p; q != h;) {
                         Node n = q.next;  // update by 2 unless singleton
                         if (head == h && casHead(h, n == null ? q : n)) {
                             h.forgetNext();
                             break;
                         }                 // advance and retry
                         if ((h = head)   == null ||
                             (q = h.next) == null || !q.isMatched())
                             break;        // unless slack < 2
                     }
                     //唤醒阻塞线程（实际这里p.waiter是为null的，因为放数据操作是非阻塞的）
                     LockSupport.unpark(p.waiter);
                     // item线程是数据，本次操作是取数据操作，因此返回数据
                     return LinkedTransferQueue.<E>cast(item);
                 }
             }
             Node n = p.next;
             p = (p != n) ? n : (h = head); // Use head if p offlist
         }
         //如果参数指定为NOW，那么就算没有被匹配，那么还是不入队，直接返回
         if (how != NOW) {                 // No matches available
             if (s == null)
                 s = new Node(e, haveData);
             //添加节点    
             Node pred = tryAppend(s, haveData);
             if (pred == null)
                 continue retry;           // lost race vs opposite mode
             /**
             *如果参数不是ASYC的这种，这可能需要阻塞等待
             *取数据操作其参数都不是ASYNC，因此如果没有取到数据（被匹配），那么就可能进行阻塞等待
             */    
             if (how != ASYNC)
                 return awaitMatch(s, pred, e, (how == TIMED), nanos);
         }
         return e; // not waiting
     }
 }
```

当在理解上面代码和注释的时候，记住这里我们分析的是取数据部分，因为有了前面放数据部分的分析，这里应该还是很好理解，取数据和放数据都是差不多的，都是和队列里面的操作进行匹配，如果队里里面的操作是取数据操作，本次操作是取数据操作，那么此时是不匹配的，需要把本次操作入队（参数NOW，ASYNC,SYNC,TIMED 不一样），如果队列的操作都是放数据的操作，本次操作是取数据操作，那么这个是匹配的，就把队头的数据取出来，返回即可。

下面我们来看看awaitMatch 方法：

```java
/**
 * Spins/yields/blocks until node s is matched or caller gives up.
 *
 * @param s the waiting node
 * @param pred the predecessor of s, or s itself if it has no
 * predecessor, or null if unknown (the null case does not occur
 * in any current calls but may in possible future extensions)
 * @param e the comparison value for checking match
 * @param timed if true, wait only until timeout elapses
 * @param nanos timeout in nanosecs, used only if timed is true
 * @return matched item, or e if unmatched on interrupt or timeout
 */
private E awaitMatch(Node s, Node pred, E e, boolean timed, long nanos) {
    final long deadline = timed ? System.nanoTime() + nanos : 0L;
    Thread w = Thread.currentThread();
    int spins = -1; // initialized after first item and cancel checks
    ThreadLocalRandom randomYields = null; // bound if needed

    for (;;) {
        Object item = s.item;
        //原本item是等于e的，匹配过后或者取消后，会改变item
        if (item != e) {                  // matched  
            //将item 设置成自身，waiter 设置为null
            s.forgetContents();           // avoid garbage
            return LinkedTransferQueue.<E>cast(item);
        }
        //如果被中断，或者发生超时了，那么就取消该操作（设置item 为自身）
        if ((w.isInterrupted() || (timed && nanos <= 0)) &&
                s.casItem(e, s)) {        // cancel
            //从队列中移除该节点
            unsplice(pred, s);
            return e;
        }
        //下面都是进行的超时或者自旋操作
        if (spins < 0) {                  // establish spins at/near front
            if ((spins = spinsFor(pred, s.isData)) > 0)
                randomYields = ThreadLocalRandom.current();
        }
        else if (spins > 0) {             // spin
            --spins;
            if (randomYields.nextInt(CHAINED_SPINS) == 0)
                Thread.yield();           // occasionally yield
        }
        else if (s.waiter == null) {
            s.waiter = w;                 // request unpark then recheck
        }
        else if (timed) {
            nanos = deadline - System.nanoTime();
            if (nanos > 0L)
                //如果设置了超时，则进行超时等待
                LockSupport.parkNanos(this, nanos);
        }
        else {
            //阻塞等待
            LockSupport.park(this);
        }
    }
}
```

```java
final void forgetContents() {
    UNSAFE.putObject(this, itemOffset, this);
    UNSAFE.putObject(this, waiterOffset, null);
}
```

看起来稍微有点长，其实不算难，和SynchronousQueue的awaitFulfill差不多，这里主要进行了自旋，如果自旋后，仍然没有被匹配或者取消，则进行阻塞（如果设置了超时阻塞，则进行一段时间的阻塞），如果发生了中断异常，会取消该操作，该边item的值，匹配成功后也会更改item的值，因此如果item和原来的值不相等时，则说明发生了改变，返回即可。

在awaitMatch过程中，如果线程被中断了，或者超时了则会调用unsplice()方法去除该节点。

```java
/**
 * Unsplices (now or later) the given deleted/cancelled node with
 * the given predecessor.
 *
 * @param pred a node that was at one time known to be the
 * predecessor of s, or null or s itself if s is/was at head
 * @param s the node to be unspliced
 */
final void unsplice(Node pred, Node s) {
    //清除s的部分数据
    s.forgetContents(); // forget unneeded fields
    /*
     * See above for rationale. Briefly: if pred still points to
     * s, try to unlink s.  If s cannot be unlinked, because it is
     * trailing node or pred might be unlinked, and neither pred
     * nor s are head or offlist, add to sweepVotes, and if enough
     * votes have accumulated, sweep.
     */
    if (pred != null && pred != s && pred.next == s) {
        Node n = s.next;
        if (n == null ||
            (n != s && pred.casNext(s, n) && pred.isMatched())) {
            /**
            *这个for循环，用于推进head,如果head已经被匹配了，则需要更新head
            */
            for (;;) {               // check if at, or could be, head
                Node h = head;
                if (h == pred || h == s || h == null)
                    return;          // at head or list empty
                 //h 没有被匹配，跳出循环，否则可能需要更新head  
                if (!h.isMatched())
                    break;
                Node hn = h.next;
                //遍历结束了，退出循环
                if (hn == null)
                    return;          // now empty
                //head 被匹配了，重新设置设置head    
                if (hn != h && casHead(h, hn))
                    h.forgetNext();  // advance head
            }
            //s节点被移除后，需要记录删除的操作次数，如果超过阀值，则需要清理队列
            if (pred.next != pred && s.next != s) { // recheck if offlist
                for (;;) {           // sweep now if enough votes
                    int v = sweepVotes;
                    //没超过阀值，则递增记录值
                    if (v < SWEEP_THRESHOLD) {
                        if (casSweepVotes(v, v + 1))
                            break;
                    }
                    else if (casSweepVotes(v, 0)) {
                        //重新设置记录数，并清理队列
                        sweep();
                        break;
                    }
                }
            }
        }
    }
}
```

```java

private void sweep() {
    for (Node p = head, s, n; p != null && (s = p.next) != null; ) {
        if (!s.isMatched()) // s节点未被匹配，则继续向后遍历
            // Unmatched nodes are never self-linked
            p = s;
        else if ((n = s.next) == null) //s节点被匹配，但是是尾节点，则退出循环
            //s为尾结点，则可能其它线程刚好匹配完，所有这里不移除s，让其它匹配线程操作
            break;
        else if (s == n)    // stale s节点已经脱离了队列了，重头开始遍历
            // No need to also check for p == s, since that implies s == n
            p = head;
        else
            p.casNext(s, n); //移除s节点
    }
}
```

看看这个移除操作其实也不是那么简单，这里并没有简单的就将节点移除就ok了，同时还检查了队列head的有效性，如果head被匹配了，则会推荐head,保持队列head 是有效的， 
如果移除节点的前驱也失效了，说明其它线程再操作，这里就不操作了，当移除了节点后，需要记录移除节点的操作次数sweepVotes，如果这个值超过了阀值，则会对队列进行清理（移除那些失效的节点）

### 4.获取队列首个有效操作

在SynchronousQueue 中其队列是无法遍历的，而且也无法获取队头信息，但是在LinkedTransferQueue却不一样，LinkedTransferQueue可以获取对头，也可以进行遍历

#### peek

```java
public E peek() {
    return firstDataItem();
}
```

```java
/**
 * Returns the item in the first unmatched node with isData; or
 * null if none.  Used by peek.
 */
private E firstDataItem() {
    //遍历队列，查找第一个有效的操作节点
    for (Node p = head; p != null; p = succ(p)) {
        Object item = p.item;
        //如果该节点是数据节点，同时没有被取消，则返回数据
        if (p.isData) {
            if (item != null && item != p)
                return LinkedTransferQueue.<E>cast(item);
        }
        else if (item == null)// 非数据节点返回null，这里注意
            return null;
    }
    return null;
}
```

```java

final Node succ(Node p) {  //如果节点p 失效则返回head,否则返回p的后继
    Node next = p.next;
    return (p == next) ? head : next;
}
```

这个peek 方法返回的是队列的第一个有效的节点，而这个节点可能是数据节点，也可能是取数据的操作节点，那么peek 可能返回数据，也可能返回null,但是返回null 并不一定是队列为空，也可能是队列里面都是取数据的操作节点，这个需要**注意**一下。

## 五：总结

LinkedTransferQueue 和SynchronousQueue 其实基本是差不多的，两者都是无锁带阻塞功能的队列，SynchronousQueue 通过内部类Transferer 来实现公平和非公平队列 。

在LinkedTransferQueue 中没有公平与非公平的区分，LinkedTransferQueue 实现了TransferQueue接口，该接口定义的是带阻塞操作的操作，相比SynchronousQueue 中的Transferer 功能更丰富。 

