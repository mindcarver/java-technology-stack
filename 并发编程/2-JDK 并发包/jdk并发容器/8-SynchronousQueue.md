# SynchronousQueue

## 一：概述

SynchronousQueue是一种阻塞队列，其中每个插入操作必须等待另一个线程的对应移除操作 ，反之亦然。同步队列没有任何内部容量，甚至连一个队列的容量都没有。

## 二：数据结构

由于SynchronousQueue的支持公平策略和非公平策略，所以底层可能两种数据结构：队列（实现公平策略）和栈（实现非公平策略），队列与栈都是通过链表来实现的。具体的数据结构如下

![](https://ws1.sinaimg.cn/large/006tNc79ly1g1smvciitij319q0ly76g.jpg)

说明：数据结构有两种类型，栈和队列；栈有一个头结点，队列有一个头结点和尾结点；栈用于实现非公平策略，队列用于实现公平策略。

## 三：继承体系

![image-20190407080649645](https://ws1.sinaimg.cn/large/006tNc79ly1g1tqyc3xo3j30qs0kqdh9.jpg)

说明：SynchronousQueue继承了AbstractQueue抽象类，AbstractQueue定义了对队列的基本操作；同时实现了BlockingQueue接口，BlockingQueue表示阻塞型的队列，其对队列的操作可能会抛出异常；同时也实现了Searializable接口，表示可以被序列化。

## 四：类的内部类

SynchronousQueue的内部类框架图如下：

![image-20190407143342298](/Users/codecarver/Library/Application Support/typora-user-images/image-20190407143342298.png)

DelayQueue 通过组合一个PriorityQueue 来实现元素的存储以及优先级维护，通过ReentrantLock 来保证线程安全，通过Condition 来判断是否可以取数据.

其中比较重要的类是左侧的三个类，Transferer是TransferStack栈和TransferQueue队列的公共类，定义了转移数据的公共操作，由TransferStack和TransferQueue具体实现，WaitQueue、LifoWaitQueue、FifoWaitQueue表示为了兼容JDK1.5版本中的SynchronousQueue的序列化策略所遗留的.

### 1.Transferer

```java
abstract static class Transferer<E> {

  // 转移数据，put或者take操作
  abstract E transfer(E e, boolean timed, long nanos);
```

Transferer定义了transfer操作，用于take或者put数据。transfer方法由子类实现。

### 2.TransfererStack

#### TransfererStack属性

```java
static final class TransferStack<E> extends Transferer<E> {
  // 表示消费数据的消费者
  static final int REQUEST    = 0;
  /** Node represents an unfulfilled producer */
  // 表示生产数据的生产者
  static final int DATA       = 1;
  /** Node is fulfilling another unfulfilled DATA or REQUEST */
  // 表示匹配另一个生产者或消费者
  static final int FULFILLING = 2;

  /** The head (top) of the stack */
  // 头结点
  volatile SNode head;
}
```

TransferStack有三种不同的状态，REQUEST，表示消费数据的消费者；DATA，表示生产数据的生产者；FULFILLING，表示匹配另一个生产者或消费者。任何线程对TransferStack的操作都属于上述3种状态中的一种。同时还包含一个head域，表示头结点。

#### 内部类SNode

##### 基本属性

```java
static final class SNode {
            // 下一个结点
            volatile SNode next;        // next node in stack
            // 相匹配的结点
            volatile SNode match;       // the node matched to this
            // 等待的线程
            volatile Thread waiter;     // to control park/unpark
            // 元素项
            Object item;                // data; or null for REQUESTs
            // 模式
            int mode;
            // item域和mode域不需要使用volatile修饰，因为它们在volatile/atomic操作之前写，之后读
            // Unsafe mechanics
            // 反射机制
            private static final sun.misc.Unsafe UNSAFE;
            // match域的内存偏移地址
            private static final long matchOffset;
            // next域的偏移地址
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = SNode.class;
                    matchOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("match"));
                    nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }
```

SNode类表示栈中的结点，使用了反射机制和CAS来保证原子性的改变相应的域值。

##### tryMatch函数　　

```java
boolean tryMatch(SNode s) { 
                if (match == null &&
                    UNSAFE.compareAndSwapObject(this, matchOffset, null, s)) { // 本结点的match域为null并且比较并替换match域成功
                    // 获取本节点的等待线程
                    Thread w = waiter;
                    if (w != null) { // 存在等待的线程    // waiters need at most one unpark 
                        // 将本结点的等待线程重新置为null
                        waiter = null;
                        // unpark等待线程
                        LockSupport.unpark(w);
                    }
                    return true;
                }
                // 如果match不为null或者CAS设置失败，则比较match域是否等于s结点，若相等，则表示已经完成匹配，匹配成功
                return match == s;
            }
```

说明：将s结点与本结点进行匹配，匹配成功，则unpark等待线程。具体流程如下:

1. 判断本结点的match域是否为null，若为null，则进入步骤2，否则，进入步骤5
2. CAS设置本结点的match域为s结点，若成功，则进入步骤3，否则，进入步骤5
3. 判断本结点的waiter域是否为null，若不为null，则进入步骤4，否则，进入步骤5
4. 重新设置本结点的waiter域为null，并且unparkwaiter域所代表的等待线程。进入步骤6
5. 比较本结点的match域是否为本结点，若是，则进入步骤6，否则，进入步骤7
6. 返回true
7. 返回false

#### 核心函数

##### isFulfilling函数　

```java
// 是否包含FULFILLING标记。
static boolean isFulfilling(int m) { return (m & FULFILLING) != 0; }
```

##### transfer函数

```java
E transfer(E e, boolean timed, long nanos) {
            SNode s = null; // constructed/reused as needed
            // 根据e确定此次转移的模式（是put or take）
            int mode = (e == null) ? REQUEST : DATA;
    
            for (;;) { // 无限循环
                // 保存头结点
                SNode h = head;
                if (h == null || h.mode == mode) {  // 头结点为null或者头结点的模式与此次转移的模式相同    // empty or same-mode
                    if (timed && nanos <= 0) { // 设置了timed并且等待时间小于等于0，表示不能等待，需要立即操作     // can't wait
                        if (h != null && h.isCancelled()) // 头结点不为null并且头结点被取消
                            casHead(h, h.next); // 重新设置头结点（弹出之前的头结点）    // pop cancelled node
                        else // 头结点为null或者头结点没有被取消
                            // 返回null
                            return null;
                    } else if (casHead(h, s = snode(s, e, h, mode))) { // 生成一个SNode结点；将原来的head头结点设置为该结点的next结点；将head头结点设置为该结点
                        // Spins/blocks until node s is matched by a fulfill operation.
                        // 空旋或者阻塞直到s结点被FulFill操作所匹配
                        SNode m = awaitFulfill(s, timed, nanos);
                        if (m == s) { // 匹配的结点为s结点（s结点被取消）               // wait was cancelled
                            // 清理s结点
                            clean(s);
                            // 返回
                            return null;
                        }
                        if ((h = head) != null && h.next == s) // h重新赋值为head头结点，并且不为null；头结点的next域为s结点，表示有结点插入到s结点之前，完成了匹配
                            // 比较并替换head域（移除插入在s之前的结点和s结点）
                            casHead(h, s.next);     // help s's fulfiller
                        // 根据此次转移的类型返回元素
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    }
                } else if (!isFulfilling(h.mode)) { // 没有FULFILLING标记，尝试匹配 // try to fulfill
                    if (h.isCancelled()) // 被取消           // already cancelled
                        // 比较并替换head域（弹出头结点）
                        casHead(h, h.next);         // pop and retry
                    else if (casHead(h, s=snode(s, e, h, FULFILLING|mode))) { // 生成一个SNode结点；将原来的head头结点设置为该结点的next结点；将head头结点设置为该结点
                        for (;;) { // 无限循环    // loop until matched or waiters disappear
                            // 保存s的next结点
                            SNode m = s.next;       // m is s's match
                            if (m == null) { // next域为null       // all waiters are gone
                                // 比较并替换head域
                                casHead(s, null);   // pop fulfill node
                                // 赋值s为null
                                s = null;           // use new node next time
                                break;              // restart main loop
                            }
                            // m结点的next域
                            SNode mn = m.next;
                            if (m.tryMatch(s)) { // 尝试匹配，并且成功
                                // 比较并替换head域（弹出s结点和m结点）
                                casHead(s, mn);     // pop both s and m
                                // 根据此次转移的类型返回元素
                                return (E) ((mode == REQUEST) ? m.item : s.item);
                            } else // 匹配不成功            // lost match
                                // 比较并替换next域（弹出m结点）
                                s.casNext(m, mn);   // help unlink
                        }
                    }
                } else { // 头结点正在匹配                            // help a fulfiller
                    // 保存头结点的next域
                    SNode m = h.next; // m与h可以匹配             // m is h's match
                    if (m == null) // next域为null                 // waiter is gone
                        // 比较并替换head域（m被其他结点匹配了，需要弹出h）
                        casHead(h, null);           // pop fulfilling node
                    else { // next域不为null
                        // 获取m结点的next域
                        SNode mn = m.next;
                        if (m.tryMatch(h)) // m与h匹配成功         // help match
                            // 比较并替换head域（弹出h和m结点）
                            casHead(h, mn);         // pop both h and m
                        else // 匹配不成功                       // lost match
                            // 比较并替换next域（移除m结点）
                            h.casNext(m, mn);       // help unlink
                    }
                }
            }
        }
```

此函数用于生产或者消费一个元素，并且transfer函数调用了awaitFulfill函数.

##### awaitFulfill函数

```java
SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            // 根据timed标识计算截止时间
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            // 获取当前线程
            Thread w = Thread.currentThread();
            // 根据s确定空旋等待的时间
            int spins = (shouldSpin(s) ?
                         (timed ? maxTimedSpins : maxUntimedSpins) : 0); 
            for (;;) { // 无限循环，确保操作成功
                if (w.isInterrupted()) // 当前线程被中断
                    // 取消s结点
                    s.tryCancel();
                // 获取s结点的match域
                SNode m = s.match;
                if (m != null) // m不为null，存在匹配结点
                    // 返回m结点
                    return m;
                if (timed) { // 设置了timed
                    // 确定继续等待的时间
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) { // 继续等待的时间小于等于0，等待超时
                        // 取消s结点
                        s.tryCancel();
                        // 跳过后面的部分，继续
                        continue;
                    }
                }
                if (spins > 0) // 空旋等待的时间大于0
                    // 确实是否还需要继续空旋等待
                    spins = shouldSpin(s) ? (spins-1) : 0;
                else if (s.waiter == null) // 等待线程为null
                    // 设置waiter线程为当前线程
                    s.waiter = w; // establish waiter so can park next iter
                else if (!timed) // 没有设置timed标识
                    // 禁用当前线程并设置了阻塞者
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold) // 继续等待的时间大于阈值
                    // 禁用当前线程，最多等待指定的等待时间，除非许可可用
                    LockSupport.parkNanos(this, nanos);
            }
        }
```

此函数表示当前线程自旋或阻塞，直到结点被匹配。awaitFulfill函数调用了shouldSpin函数

##### shouldSpin函数

```java
boolean shouldSpin(SNode s) {
            // 获取头结点
            SNode h = head;
            // s为头结点或者头结点为null或者h包含FULFILLING标记,返回true
            return (h == s || h == null || isFulfilling(h.mode)); 
        }
```

　说明：此函数表示是当前结点所包含的线程（当前线程）进行空旋等待，有如下情况需要进行空旋等待

  　　1. 当前结点为头结点

  　　2. 头结点为null

   　    3. 头结点正在匹配中

##### clean函数　　

```java
void clean(SNode s) {
            // s结点的item设置为null
            s.item = null;   // forget item
            // waiter域设置为null
            s.waiter = null; // forget thread
            // 获取s结点的next域
            SNode past = s.next;
            if (past != null && past.isCancelled()) // next域不为null并且next域被取消
                // 重新设置past
                past = past.next;

            // Absorb cancelled nodes at head
            SNode p;
            while ((p = head) != null && p != past && p.isCancelled()) // 从栈顶头结点开始到past结点（不包括），将连续的取消结点移除
                // 比较并替换head域（弹出取消的结点）
                casHead(p, p.next);

            // Unsplice embedded nodes
            while (p != null && p != past) { // 移除上一步骤没有移除的非连续的取消结点
                // 获取p的next域
                SNode n = p.next;
                if (n != null && n.isCancelled()) // n不为null并且n被取消
                    // 比较并替换next域
                    p.casNext(n, n.next);
                else
                    // 设置p为n
                    p = n;
            }
        }
```

此函数用于移除从栈顶头结点开始到该结点（不包括）之间的所有已取消结点。

### 3.TransferQueue

##### 类的属性

```java
static final class TransferQueue<E> extends Transferer<E> {
        /** Head of queue */
        // 队列的头结点
        transient volatile QNode head;
        /** Tail of queue */
        // 队列的尾结点
        transient volatile QNode tail;
        /**
         * Reference to a cancelled node that might not yet have been
         * unlinked from queue because it was the last inserted node
         * when it was cancelled.
         */
        // 指向一个取消的结点，当一个结点是最后插入队列时，当被取消时，它可能还没有离开队列
        transient volatile QNode cleanMe;
    }
```

队列存在一个头结点和一个尾节点，分别指示队头和队尾，还包含了一个指示取消结点的域。

##### 类的内部类QNode

```java
static final class QNode {
            // 下一个结点
            volatile QNode next;          // next node in queue
            // 元素项
            volatile Object item;         // CAS'ed to or from null
            // 等待线程
            volatile Thread waiter;       // to control park/unpark
            // 是否为数据
            final boolean isData;

            // 构造函数
            QNode(Object item, boolean isData) {
                // 初始化item域
                this.item = item;
                // 初始化isData域
                this.isData = isData;
            }
            // 比较并替换next域
            boolean casNext(QNode cmp, QNode val) {
                return next == cmp &&
                    UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }
            // 比较并替换item域
            boolean casItem(Object cmp, Object val) {
                return item == cmp &&
                    UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
            }

            /**
             * Tries to cancel by CAS'ing ref to this as item.
             */
            // 取消本结点，将item域设置为自身
            void tryCancel(Object cmp) {
                UNSAFE.compareAndSwapObject(this, itemOffset, cmp, this);
            }
            // 是否被取消
            boolean isCancelled() {
                // item域是否等于自身
                return item == this;
            }

            /**
             * Returns true if this node is known to be off the queue
             * because its next pointer has been forgotten due to
             * an advanceHead operation.
             */
            // 是否不在队列中
            boolean isOffList() {
                // next与是否等于自身
                return next == this;
            }

            // Unsafe mechanics
            // 反射机制
            private static final sun.misc.Unsafe UNSAFE;
            // item域的偏移地址
            private static final long itemOffset;
            // next域的偏移地址
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = QNode.class;
                    itemOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("item"));
                    nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }
```

QNode表示队列中的结点，并且通过反射和CAS原子性的修改对应的域值。

##### 构造函数

```java
TransferQueue() {
  // 初始化一个哨兵结点
  QNode h = new QNode(null, false); // initialize to dummy node.
  // 设置头结点
  head = h;
  // 设置尾结点
  tail = h;
}
```

##### 函数分析

###### transfer函数　　 

```java
E transfer(E e, boolean timed, long nanos) {
            QNode s = null; // constructed/reused as needed
            // 确定此次转移的类型（put or take）
            boolean isData = (e != null);

            for (;;) { // 无限循环，确保操作成功
                // 获取尾结点
                QNode t = tail;
                // 获取头结点
                QNode h = head;
                if (t == null || h == null) // 看到未初始化的头尾结点         // saw uninitialized value
                    // 跳过后面的部分，继续
                    continue;                       // spin
            
                if (h == t || t.isData == isData) { // 头结点与尾结点相等或者尾结点的模式与当前结点模式相同    // empty or same-mode
                    // 获取尾结点的next域
                    QNode tn = t.next;
                    if (t != tail) // t不为尾结点，不一致，重试                 // inconsistent read
                        continue;
                    if (tn != null) { // tn不为null，有其他线程添加了tn结点              // lagging tail
                        // 设置新的尾结点为tn
                        advanceTail(t, tn);
                        // 跳过后面的部分，继续
                        continue;
                    }
                    if (timed && nanos <= 0) // 设置了timed并且等待时间小于等于0，表示不能等待，需要立即操作        // can't wait
                        // 返回null
                        return null;
                    if (s == null) // s为null
                        // 新生一个结点并赋值给s
                        s = new QNode(e, isData);
                    if (!t.casNext(null, s)) // 设置t结点的next域不成功        // failed to link in
                        // 跳过后面的部分，继续
                        continue;
                    // 设置新的尾结点
                    advanceTail(t, s);              // swing tail and wait
                    // Spins/blocks until node s is fulfilled
                    // 空旋或者阻塞直到s结点被匹配
                    Object x = awaitFulfill(s, e, timed, nanos);
                    if (x == s) { // x与s相等，表示已经取消               // wait was cancelled
                        // 清除
                        clean(t, s);
                        // 返回null
                        return null;
                    }

                    if (!s.isOffList()) { // s结点还没离开队列        // not already unlinked
                        // 设置新的头结点
                        advanceHead(t, s);          // unlink if head
                        if (x != null) // x不为null             // and forget fields
                            // 设置s结点的item
                            s.item = s;
                        // 设置s结点的waiter域为null
                        s.waiter = null;
                    }
                    
                    return (x != null) ? (E)x : e;

                } else { // 模式互补                        // complementary-mode
                    // 获取头结点的next域（匹配的结点）
                    QNode m = h.next;                // node to fulfill
                    if (t != tail || m == null || h != head) // t不为尾结点或者m为null或者h不为头结点（不一致）
                        // 跳过后面的部分，继续
                        continue;                   // inconsistent read
                    // 获取m结点的元素域
                    Object x = m.item;
                    if (isData == (x != null) ||    // m结点被匹配                // m already fulfilled
                        x == m ||                   // m结点被取消                // m cancelled
                        !m.casItem(x, e)) {         // CAS操作失败                // lost CAS
                        advanceHead(h, m);          // 队列头结点出队列，并重试    // dequeue and retry
                        continue;
                    }
                    // 匹配成功，设置新的头结点
                    advanceHead(h, m);              // successfully fulfilled
                    // unpark m结点对应的等待线程
                    LockSupport.unpark(m.waiter);
                    return (x != null) ? (E)x : e;
                }
            }
        }
```

此函数用于生产或者消费一个元素，并且transfer函数调用了awaitFulfill函数.

###### awaitFulfill函数

```java
Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            /* Same idea as TransferStack.awaitFulfill */
            // 根据timed标识计算截止时间
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            // 获取当前线程
            Thread w = Thread.currentThread();
            // 计算空旋时间
            int spins = ((head.next == s) ?
                         (timed ? maxTimedSpins : maxUntimedSpins) : 0); 
            for (;;) { // 无限循环，确保操作成功
                if (w.isInterrupted()) // 当前线程被中断
                    // 取消
                    s.tryCancel(e);
                // 获取s的元素域
                Object x = s.item;
                if (x != e) // 元素不为e
                    // 返回
                    return x;
                if (timed) { // 设置了timed
                    // 计算继续等待的时间
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) { // 继续等待的时间小于等于0
                        // 取消
                        s.tryCancel(e);
                        // 跳过后面的部分，继续
                        continue;
                    }
                }
                if (spins > 0) // 空旋时间大于0
                    // 减少空旋时间
                    --spins;
                else if (s.waiter == null) // 等待线程为null
                    // 设置等待线程
                    s.waiter = w;
                else if (!timed) // 没有设置timed标识
                    // 禁用当前线程并设置了阻塞者
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold) // 继续等待的时间大于阈值
                    // 禁用当前线程，最多等待指定的等待时间，除非许可可用
                    LockSupport.parkNanos(this, nanos);
            }
        }
```

此函数表示当前线程自旋或阻塞，直到结点被匹配。

###### clean函数

```java
void clean(QNode pred, QNode s) {
            // 设置等待线程为null
            s.waiter = null; // forget thread
            /*
             * At any given time, exactly one node on list cannot be
             * deleted -- the last inserted node. To accommodate this,
             * if we cannot delete s, we save its predecessor as
             * "cleanMe", deleting the previously saved version
             * first. At least one of node s or the node previously
             * saved can always be deleted, so this always terminates.
             */
            /*
             * 在任何时候，最后插入的结点不能删除，为了满足这个条件
             * 如果不能删除s结点，我们将s结点的前驱设置为cleanMe结点
             * 删除之前保存的版本，至少s结点或者之前保存的结点能够被删除
             * 所以最后总是会结束
             */
            while (pred.next == s) { // pred的next域为s    // Return early if already unlinked
                // 获取头结点
                QNode h = head;
                // 获取头结点的next域
                QNode hn = h.next;   // Absorb cancelled first node as head
                if (hn != null && hn.isCancelled()) { // hn不为null并且hn被取消
                    // 设置新的头结点
                    advanceHead(h, hn);
                    // 跳过后面的部分，继续
                    continue;
                }
                // 获取尾结点，保证对尾结点的读一致性
                QNode t = tail;      // Ensure consistent read for tail
                if (t == h) // 尾结点为头结点，表示队列为空
                    // 返回
                    return;
                // 获取尾结点的next域
                QNode tn = t.next;
                if (t != tail) // t不为尾结点，不一致，重试
                    // 跳过后面的部分，继续
                    continue;
                if (tn != null) { // tn不为null
                    // 设置新的尾结点
                    advanceTail(t, tn);
                    // 跳过后面的部分，继续
                    continue;
                }
                if (s != t) { // s不为尾结点，移除s       // If not tail, try to unsplice
                    QNode sn = s.next;
                    if (sn == s || pred.casNext(s, sn)) // 
                        return;
                }
                // 获取cleanMe结点
                QNode dp = cleanMe;
                if (dp != null) { // dp不为null，断开前面被取消的结点    // Try unlinking previous cancelled node
                    // 获取dp的next域
                    QNode d = dp.next;
                    QNode dn;
                    if (d == null ||               // d is gone or
                        d == dp ||                 // d is off list or
                        !d.isCancelled() ||        // d not cancelled or
                        (d != t &&                 // d not tail and
                         (dn = d.next) != null &&  //   has successor
                         dn != d &&                //   that is on list
                         dp.casNext(d, dn)))       // d unspliced
                        casCleanMe(dp, null);
                    if (dp == pred)
                        return;      // s is already saved node
                } else if (casCleanMe(null, pred))
                    return;          // Postpone cleaning s
            }
        }
```

此函数用于移除已经被取消的结点。

### 五：类的属性

```java
public class SynchronousQueue<E> extends AbstractQueue<E>
    implements BlockingQueue<E>, java.io.Serializable {
    // 版本序列号
    private static final long serialVersionUID = -3223113410248163686L;
    // 可用的处理器
    static final int NCPUS = Runtime.getRuntime().availableProcessors();
    // 最大空旋时间
    static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;
    // 无限时的等待的最大空旋时间
    static final int maxUntimedSpins = maxTimedSpins * 16;
    // 超时空旋等待阈值
    static final long spinForTimeoutThreshold = 1000L;
    
    // 用于序列化
    private ReentrantLock qlock;
    private WaitQueue waitingProducers;
    private WaitQueue waitingConsumers;
}
```

SynchronousQueue类的属性包含了空旋等待时间相关的属性。

### 六：类的构造函数

```java
// 该构造函数用于创建一个具有非公平访问策略的 SynchronousQueue。
public SynchronousQueue() {
  // 非公平策略（先进后出）
  this(false);
}


// 创建一个具有指定公平策略的 SynchronousQueue。
public SynchronousQueue(boolean fair) {
  // 根据指定的策略生成不同的结构
  transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
}
```

### 七：核心函数分析

```java
// 将指定元素添加到此队列，如有必要则等待另一个线程接收它
    public void put(E e) throws InterruptedException {
        // e为null则抛出异常
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, false, 0) == null) { // 进行转移操作
            // 中断当前线程
            Thread.interrupted();
            throw new InterruptedException();
        }
    }
    
    // 将指定元素插入到此队列，如有必要则等待指定的时间，以便另一个线程接收它
    public boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException {
        // e为null则抛出异常
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, true, unit.toNanos(timeout)) != null) // 进行转移操作
            return true;
        if (!Thread.interrupted()) // 当前线程没有被中断
            // 返回
            return false;
        throw new InterruptedException();
    }
    
    // 如果另一个线程正在等待以便接收指定元素，则将指定元素插入到此队列
    public boolean offer(E e) {
        // e为null则抛出异常
        if (e == null) throw new NullPointerException();
        return transferer.transfer(e, true, 0) != null; // 进行转移操作
    }
    
    // 获取并移除此队列的头，如有必要则等待另一个线程插入它
    public E take() throws InterruptedException {
        // 进行转移操作
        E e = transferer.transfer(null, false, 0);
        if (e != null)
            return e;
        Thread.interrupted();
        throw new InterruptedException();
    }
    
    // 获取并移除此队列的头，如有必要则等待指定的时间，以便另一个线程插入它
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = transferer.transfer(null, true, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) // 元素不为null或者当前线程没有被中断
            return e;
        throw new InterruptedException();
    }
    
    // 如果另一个线程当前正要使用某个元素，则获取并移除此队列的头
    public E poll() {
        return transferer.transfer(null, true, 0);
    }
    
    // 始终返回 true
    public boolean isEmpty() {
        return true;
    }
    
    // 始终返回 0
    public int size() {
        return 0;
    }
    
    // 始终返回 0
    public int remainingCapacity() {
        return 0;
    }
    
    // 不执行任何操作
    public void clear() {
    }
    
    // 始终返回false
    public boolean contains(Object o) {
        return false;
    }
    
    // 始终返回false
    public boolean remove(Object o) {
        return false;
    }
    
    // 除非给定 collection 为空，否则返回 false
    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }
    
    // 始终返回 false
    public boolean removeAll(Collection<?> c) {
        return false;
    }
    
    // 始终返回 false
    public boolean retainAll(Collection<?> c) {
        return false;
    }
    
    // 始终返回 null
    public E peek() {
        return null;
    }
    
    // 返回一个空迭代器，其中 hasNext 始终返回 false
    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }
    
    // 
    public Spliterator<E> spliterator() {
        return Spliterators.emptySpliterator();
    }
    
    // 返回一个 0 长度的数组
    public Object[] toArray() {
        return new Object[0];
    }
    
    // 将指定数组的第 0 个元素设置为 null（如果该数组有非 0 的长度）并返回它
    public <T> T[] toArray(T[] a) {
        if (a.length > 0)
            a[0] = null;
        return a;
    }
    
    // 移除此队列中所有可用的元素，并将它们添加到给定 collection 中
    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }
    
    // 最多从此队列中移除给定数量的可用元素，并将这些元素添加到给定 collection 中
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; n < maxElements && (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }
```

SynchronousQueue的函数很大程度都是依托于TransferStack或TransferQueue的transfer函数，所以，了解transfer函数就可以了解SynchronousQueue的原理。

### 八：实例演示

```java
   public static void main(String[] args) {
        SynchronousQueue<Integer> queue = new SynchronousQueue<Integer>();
        Producer p1 = new Producer("p1", queue, 10);
        Producer p2 = new Producer("p2", queue, 50);

        Consumer c1 = new Consumer("c1", queue);
        Consumer c2 = new Consumer("c2", queue);

        c1.start();
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        c2.start();
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        p1.start();
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        p2.start();

    }

    static class Producer extends Thread {
        private SynchronousQueue<Integer> queue;
        private int n;
        public Producer(String name, SynchronousQueue<Integer> queue, int n) {
            super(name);
            this.queue = queue;
            this.n = n;
        }

        public void run() {
            System.out.println(getName() + " offer result " + queue.offer(n));
        }
    }

    static class Consumer extends Thread {
        private SynchronousQueue<Integer> queue;
        public Consumer(String name, SynchronousQueue<Integer> queue) {
            super(name);
            this.queue = queue;
        }

        public void run() {
            try {
                System.out.println(getName() + " take result " + queue.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

> 运行结果：
>
> p1 offer result true
> c2 take result 10
> p2 offer result true
> c1 take result 50

该示例中，有两个生产者p1、p2和两个消费者c1、c2，按照c1、c2、p1、p2的顺序启动，并且每个线程启动后休眠100ms，则可能有如下的时序图:

![image-20190407151800148](https://ws1.sinaimg.cn/large/006tNc79ly1g1u3f1snavj30nk0p0jsx.jpg)



时序图中，c1线程的take操作早于c2线程的take操作早于p1线程的offer操作早于p2线程的offer操作。

　　根据示例源码可知，此SynchronousQueue采用非公平策略，即底层采用栈结构。

① c1执行take操作，主要的函数调用如下

![img](data:image/png;base64,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f/vbX/3qV8PWcfVXBERABETACCiOojOhigQIosyZM+fZs2cEUb7++muFUqo4SLJJBERABEomoDhKyYClPhcBgig4KFRlO7ZCKbkQqpIIiIAI1J6A4ii1H8LmdcCDKNY1hVKaN8TqkQiIgAhkITCSRUgyItBnAmyVpUWWe1joIfG9732vzwaoOREQAREQgYETUBxl4EMgAxIJ6G6sRDQqEAEREIEhIKD9KEMwyOqiCIiACIiACNSQgHyUGg6aTBYBERABERCBISAgH2UIBlldFAEREAEREIEaEpCPUsNBk8kiIAIiIAIiMAQE5KMMwSCriyIgAiIgAiJQQwLyUWo4aDJZBERABERABIaAgHyUIRhkdVEEREAEREAEakhAPkoNB00mi4AIiIAIiMAQEGi4j8LbXqZPn86jwBhKvqdMmfLo0aMhGFZ1UQREQAREQARqT6DhPgqvetmxY4eP0qZNm2bPnu2HSoiACIiACIiACFSWQPOfhU8ohde+8D0yMnLv3r358+dXdjBkWISAnoUfAaJDERABERgqAg2PozCWHkohiCIHZahObnVWBERABESg1gR6iqPcuXNn3759t27d0iaPQk6CmTNnjo+Pv/vuu3wXorDuShRHqfsIyn4REAER6IVA/jjKkSNH1qxZs2zZss8///ybyn++/PLLytv4zdmzZ998880NGzbs3bu3l0FVXREQAREQARFoAIGccRQiKDgod+/eHR0dbQCFSnWBrTNjY2PHjx9fvnx5pQzrvzGKo/SfuVoUAREQgeoQyOmjvPXWW0RQ3nnnner0pEmWTExMnDhxgrBKkzqVoy/yUXJAUxUREAERaAyBnD4Kd8pcunRJ9/GWdB48efKEUMrz589L0l8XtfJR6jJSslMEREAEyiCQ00fR5FHGYIQ6RRgaghCeEkqLgAiIwLARyL9ndthIqb8iIAIiIAIiIAL9JCAfJSftq1ev8iv/4cOHVp8Eh3xWr14dajx9+jSZlrN9+/ZIaSiptAiIgAiIgAiIQEigLB/FpvBvZ+3vfNkkHZYyc4cGlZe2RnEa2jZx4MCB7xga+B9t5SOZ9GLbtm3c3nzu3DnzS2guIqNDERABERABERCB7ATK8lEWL17szyPBmv3799shUziT95IlS06dOmU5RCD6OZ3znDSjg0cS+is8hm7evHluM4m5c+dm53j+/PmlS5eG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IiACIiACIiACOQj020fBFSAu4obavTm2q4NlHQ+ZsBnFnAa+iV6kP26EVR52k/hzVqhC9GXv3r20gvOB62BpDpPcCFZqWFoyL8f2z5qFpH1bDDlYiwbbtsKh78ylRTbweitWV98iIAIiIAIiIAK9EOj381FwGljBCd0UW+6J9IGbdNyfYGuquwURMT/ksSV2r7Ll0IRXMVXWIu6Oq/W6JBB+/PgxO2NII+BieD/c1+PWUhQ+zQXDuC3ZNudmMTJsUWkREAEREAEREIF0AjlfX8y03da3SG+sSaXEV3BfLAJURr9EuAyq0ikCIiACIlAjAv1e66kRGpkqAiIgAiIgAiIwQALyUQYIX02LgAiIgAiIgAgkEsi5ZKOViESiBRWIcEEgpUYEREAERKCuBHLGUbjdl0fB1rXTlbf7yZMno6OjlTdTBoqACIiACIhAiQRy+ijj4+Md3wBcotVNV809zxBuei/VPxEQAREQARFII5BzrefGjRu8Ue/27dvTpk1LU6+y7gm8ePFiwYIFBw8eXLt2bfe1VUMEREAEREAEGkIgfxxly5YtY2NjExMTLEw0BMaguwFJHrSPg7Ju3To5KIMeDbUvAiIgAiIwYAI54yhm9RdffPHpp5/eunXr2bNnA+5HI5pnDwpLPG+//bYclEaMpzohAiIgAiLQE4GefJSeWu5jZd0j00fYakoEREAEREAEiiGQc62nmMalRQREQAREQAREQAQSCMhHSQCjbBEQAREQAREQgYESkI8yUPxqXAREQAREQAREIIGAfJQEMMoWAREQAREQAREYKAH5KAPFr8ZFQAREQAREQAQSCMhHSQCjbBEQAREQAREQgYESkI8yUPxqXAREQAREQAREIIGAfJQEMMoWAREQAREQAREYKAH5KAPFr8ZFQAREQAREQAQSCMhHSQCjbBEQAREQAREQgYESkI8yUPxqXAREQAREQAREIIGAfJQEMMoWAREQAREQAREYKAH5KAPFr8ZFQAREQAREQAQSCMhHSQCjbBEQAREQAREQgYESaLKPMjk5OX369FdeeQXCfE+ZMuXRo0cDpa3GRUAEREAEREAEshJoso8ybdq0HTt2OIlNmzbNnj3bD5UQAREQAREQARGoMoFXvvnmmyrb16NthFLmzJnD98jIyL179+bPn9+jQlUXAREQAREQARHoD4Emx1Eg6KEUgihyUPpzSqkVERABERABESiEQMPjKDAiiPLaa69dv35dPkohZ4yUiIAIiIAIiEB/CHTwUe7cubNv375bt25pt2l/xiOplVdffXV8fHz37t18J8koXwREQAREQASaRCBtrefIkSNr1qxZtmzZpUuX2LaizwAJEAdatWrVhg0b9u7d26TzT30RAREQAREQgSQCiXEUIig4KHfv3h0dHU2qrPw+E2Ddamxs7Pjx48uXL+9z02pOBERABERABPpMINFH4Sf7T3/603feeafPBqm5dAITExMnTpw4e/ZsuphKRUAEREAERKDuBBLXer766iv9WK/g6DIoN27cqKBhMkkEREAEREAEiiWQGEfhwaxsvyi2MWkrhICGphCMUiICIiACIlBxAolxlIrbLfNEQAREQAREQASaTUA+SrPHV70TAREQAREQgboSkI8y4JG7evUqazd8D9gONS8CIiACIiACFSNQgI+yfft2Ztn45/Tp03Q2LG07EyNG3bZFxmr1t5+KcZM5IiACIiACIiAC5RIowEc5dOiQPdzs1KlTGPvgwQM7XL9+PQ7K4cOH7ZD8zZs3J/VmxowZSUUZ82kLZyajcBliAzegjE5JpwiIgAiIgAgMikABPkqK6RcuXNi2bZsJzJ079/79+3FhXBmcGErjRcoRAREQAREQAREYWgLl+ii8xg83JQ43XN+xDRkPHz40MarYspEtFUXqEik5cOAAEQuTIWECHBKwOX/+vOVbJpJ2yLdrs+bsm3zapYhGzSRyqBuJiHDo7yM0A1yz51MxowHo99ap9eTJE7NW3yIgAiIgAiIgAiGBcn0UFndY4mEmZlYOW01Kmzdga0PHjh1rK8Z79WbPno0MmnELzPngkIANb7SxulTEjUDSDpHksbnuplC6ZMkSK7L4DQJ79uzxum3b9UzU8oZF12x+EocZDcArovX9+/dTxQxzzUqIgAiIgAiIgAg4gXJ9FFvHoTFm5dBTsfzFixe7HZZg/vZFn3PnzkVK7RBXYNeuXaSRxCm5fPlyW7GjR4/a/hiTpFbo9Fy5ciVSq228JyJjh6hiC46lcTXwk9qKJRlw5syZefPmeRfilrTVpkwREAEREAERGDYC5fooRtPjE3gqvqbTFjTBDKZ8vJkw5tFW0jPbKiTTQhSosk/Ek4hv0XXfyDVnSSxatAixuA0pBhCD8RWiLE1IRgREQARE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EkIpbTsV5uPKmAzys2bNaiuvTBEQAREQgVoTUByl1sPXQONZx7EFHf+28AkOCjESyyRw0sCeq0siIAIiIALfJSAf5bs8dDRQArNnzz5//nzcBFvuYeknXkRA5fHjx/F85YiACIiACNSdgHyUuo9go+y3nSjcp2O9evjwoe2ZnTFjBjk3b97kG3+FPbbebfbV+qHtn/UiJURABERABGpNQD5KrYevgcazmsO9Oazs8CFGYntj2RJ76tSpDRs2kLlkyZJwrefQoUMsD30r/gqbWhBrIBR1SQREQASGksArTAlD2XF1WgREQAREQAREoNIEFEep9PDIOBEQAREQAREYWgLyUYZ26NVxERABERABEag0AfkolR4eGScCIiACIiACQ0tAPsrQDr06LgIiIAIiIAKVJiAfpdLDI+NEQAREQAREYGgJyEcZ2qFXx0VABERABESg0gSm4YTbAAAAa0lEQVTko1R6eGScCIiACIiACAwtAfkoQzv06rgIiIAIiIAIVJqAfJRKD4+MEwEREAEREIGhJSAfZWiHXh0XAREQAREQgUoTkI9S6eGRcSIgAiIgAiIwtATkowzt0KvjIiACIiACIlBpAv8fnOpVeXWwxIsAAAAASUVORK5CYII=)

　　说明：其中，c1线程进入awaitFulfill后，会空旋等待，直到空旋时间消逝，会调用LockSupport.park函数，会禁用当前线程（c1），直至许可可用。

　　② c1执行take操作，主要的函数调用如下

![img](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAiIAAAGTCAIAAACaqO4wAAAgAElEQVR4Ae2db6hVV3r/z/xQmhe+ULgvghjxqoGE5sIIqdxkNDHJ1D/UF3lhqA6WWiwmWotTkupALMkQQ9A2OCWgjqVE8DoKk9KBvPBPZ1RU4sXccgUDBnr9Q2KD0AGFsVSYAX8ffTJr1t173/3nnL3O2Xuf70aua6/1rGc967PWWc9ea+0/33nw4EFLhwiIgAiIgAiEIfD/wqiVVhEQAREQARF4SEBuRv1ABERABEQgIAG5mYBwpVoEREAEREBuRn1ABERABEQgIAG5mYBwpVoEREAEREBuRn1ABERABEQgIIHCbuby5cuvvfba4ODgd3SUQeCJJ56A5+joaMBGlmoREAER6B2BYm7mwIEDq1atevHFFz/99FMeuNHROYHjx4+/8sor69at27VrV++6gUoWAREQgVAEvsNAmVM38xh8zJUrVwYGBnJmkVhOAnfv3h0aGjp8+PCyZctyZpGYCIiACNSCQAE3w9oO85itW7fWomK1M3JkZOTIkSNMbmpnuQwWAREQgRQCBdwM+zFnzpyZN29eijoltU3g1q1bTGju3LnTtgZlFAEREIEKEijgZtjwzr/CVsGqVt8kEa5+G8lCERCBogSK3QJQVLvkRUAEREAE+pyA3EyfdwBVXwREQATCEuhrN3PhwgXWqa5fvx6WsbSLgAiIQB8TqJybsaHfHnzcsmWL3zR+EgJ79uzxUxUWAREQARGoIIFquRkcydKlS48ePWqPPTLPIMao4XJIOn/+vCVdu3Ztx44dK1euTGRqDknTlEQ4ihQBERCBbhKY1s3CMsv67LPPkFm7dq1JnjhxwgK4jf379+NjlixZYjHz58/nFMdDkou0JP0VAREQARGoDoFqzWbmzp0LGjeDcZh+9rOfLViwIOJOOCWSJBNjZrNw4ULCNu8hQCpra8eOHSPMX1uIm2q1zSZAthDHNMgJTzVhskL1VwREQAREIJ1AtdyMzWOYo0R2ZRj3zYVEKkNkfGVs3759THSQZGGNFTZ04mMOHTpkq20ksdoW8WQoodDdu3dv376dMP7JLdyhR54mgl2nIiACIpCfQLXcDHbjDDZv3swSGfMJ39mwShavlR/JCtvExERchhg8jVt/sykRj9z7kvgV8zFEfvLJJytWrHALdzt37jx58mTcmfnZFRYBERABEZiKQOXcDIYyHXHOxt1OljjQJ0YmVhU9bh0sIoCPwbExj7H406dP41ecMLOciLxORUAEREAE8hOoopsx63E2jP4HDx7klFlL4kyFSH9CM1W18TEslNkaGg4sIsb6GJMn589IpVxbYXN/85QSUatTERABERABCFTXzfjN88ILL+AkIhsqnBL5gx/8wJdMDDNBwXNM5SoWL16Mp3EbNoidOnUqUY8iRUAEREAEihKolpths91uDKMaeBEmGfaxL3ZK2C9h/cqtkpHKKc7D3X7m7jQj7+zZs/n7zTffGA7fcyTu56MfVbY+9tZbb+G93OSGghKzmGb9FQEREAERyCDg1oUyAyjKlOlcwDfX3e5latml91NxDJzifiyVALsszgBLRYBby4gkyfIST9g0+zekmYxpw824gnydTnmgAIUG0iy1IiACItArAgXe7c+uOFa68bciAaYa7NjjGKZaE6uInXnMqCbhPJZLRgREQASmIlDAc2gQnApiWfEiXBZJ6REBEagOgWrtzVSHiywRAREQAREohUABNzNnzpybN2+WUqqUxAnwxOjAwEA8XjEiIAIiUGsCBdzM8PCwe5a+1nWupvHc0gbhatomq0RABESgbQIF9mZGR0fXrVs3Pj4+c+bMtstTxkQC9+7dW7Ro0d69e1evXp0ooEgREAERqCmBYrOZjRs3Dg0NjYyM3L17t6YVrprZkORRIXzMmjVr5GOq1jqyRwREoHMCBWYzVtjZs2d5foWZjTxN5/TRMGPGDNbKtm3bJh9TCk8pEQERqBqBwm6mahVIsUf3B6fAUZIIiIAIdIdAgUWz7hikUkRABERABJpEQG6mSa2puoiACIhA5QjIzVSuSWSQCIiACDSJgNxMk1pTdREBERCByhGQm6lck8ggERABEWgSAbmZJrWm6iICIiAClSMgN1O5JpFBIiACItAkAnIzTWpN1UUEREAEKkdAbqZyTSKDREAERKBJBORmmtSaqosIiIAIVI6A3EzlmkQGiYAIiECTCMjNNKk1VRcREAERqBwBuZnKNYkMEgEREIEmEZCbaVJrqi4iIAIiUDkCDXQzfAhn1qxZfAUA2PydPn36zZs3KwdeBomACIhAfxBooJvhG9J8Jcw13/r16+fNm+dOFRABERABEegmgWZ+1owJzeDgIH+nTZt29erVhQsXdpOpyhIB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)

　　说明：其中，c2线程进入awaitFulfill后，会空旋等待，直到空旋时间消逝，会调用LockSupport.park函数，会禁用当前线程（c2），直至许可可用。并且此时栈中有两个节点，c2线程所在的结点和c1线程所在的结点。

　　③ p1线程执行offer(10)操作，主要的函数调用如下

![img](data:image/png;base64,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)

　　说明：在执行offer(10)操作后，c2线程所在的结点与头结点进行了匹配（头结点生产数据，c2线程所在的结点消费数据），c2线程被unpark，可以继续运行，而c1线程还是被park中（非公平策略）。

　　③ c2线程被unpark后，继续运行，主要函数调用如下（由于c2线程是在awaitFulfill函数中被park的，所以，恢复也是在awaitFulfill函数中）

![img](data:image/png;base64,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)

　　说明：c2线程从unpark恢复时，结构如上图所示，先从awaitFulfill函数中返回，然后再从transfer函数中返回10，再从take函数中返回10。

　　④ p2线程执行offer(50)操作，主要的函数调用如下

![img](data:image/png;base64,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)

　　说明：在执行offer(50)操作后，c1线程所在的结点与头结点进行了匹配（头结点生产数据，c1线程所在的结点消费数据），c1线程被unpark，可以继续运行。

　　⑤ c1线程被unpark后，继续运行，主要函数调用如下（由于c1线程是在awaitFulfill函数中被park的，所以，恢复也是在awaitFulfill函数中）

　　![img](data:image/png;base64,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)

　　说明：c1线程从unpark恢复时，结构如上图所示，先从awaitFulfill函数中返回，然后再从transfer函数中返回50，再从take函数中返回50。

　　上述是使用非公平策略的结果（首先匹配c2线程所在的结点，之后再匹配c1线程所在结点）。

SynchronousQueue适合一对一的匹配场景，没有容量，无法缓存。有了这个基础，之后会方便分析线程池框架的源码.

---

> 转载；
>
> <https://www.cnblogs.com/leesf456/p/5560362.html>

