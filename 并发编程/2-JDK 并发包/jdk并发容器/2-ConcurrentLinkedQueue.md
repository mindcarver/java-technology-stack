# ConcurrentLinekedQueue

## 一：概述

通常实现一个线程安全队列有两种方式：一种是阻塞算法，另一种则是非阻塞算法。而ConcurrentLinekedQueue则是采用CAS方式实现的，它是一个**基于链接节点的无界线程安全队列。**

## 二：Node

ConcurrentLinekedQueue由head节点和tail节点组成，每个节点是一个Node，Node是ConcurrentLinkedQueue定义的内部对象，其内部定义了item变量用来包裹实际入队元素及next变量用来保存当前节点的下一节点引用。且上述变量都被volatile关键字修饰，这意味着对item变量和next变量的读写都会被立刻刷入主存，可以被其他线程及时看到。默认情况下head节点存储的元素为空，tail节点等于head节点。

```
private transient volatile Node<E> tail = head;
```

## 三：入队

###1.入队过程

`入队：`将入队节点添加到队列的尾部。这里最关键的就是理解head节点以及tail节点的变化，我们通过图示演示：

![](http://ifeve.com/wp-content/uploads/2013/01/ConcurrentLinekedQueue队列入队结构变化图.jpg)

**过程如下：**

1. `添加元素1：`head节点的next指向元素1节点，又因为默认情况下，tail 等于 head，所以都是一样。
2. `添加元素2：`队列先设置元素1节点的next指向元素2节点，然后更新tail节点，指向元素2节点
3. `添加元素3：`设置tail节点的next节点为元素3节点。
4. `添加元素4：`设置元素3的next节点为元素4节点，然后将tail节点指向元素4节点。

<blockquote style=" border-left-color:red;">疑问？为什么tail节点不总是指向最后一个呢
</blockquote>

其实入队主要做两件事：第一是将入队节点设置成当前队列尾节点的下一个节点；第二即更新tail节点，**若tail节点的next不为空，则将入队节点设置成tail节点，若tail节点的next节点为空，则将入队节点设置为tail的next节点**，所以我们看到tail节点不一定总是在最后面。

上述节点的入队操作仅仅是单线程运行情况下，那么我们看看ConcurrentLinkedQueue是如何通过CAS来入队的：

```java
public boolean offer(E e) {
        checkNotNull(e);
    	// 入队前，创建一个入队节点
        final Node<E> newNode = new Node<E>(e);
		// p是用来表示队列的尾节点
        for (Node<E> t = tail, p = t;;) {
            Node<E> q = p.next;
            if (q == null) {
                // 如果p是尾节点，则设置p节点的next节点为入队节点
                if (p.casNext(null, newNode)) {
                   /*
                   如果tail节点有大于等于1个next节点，则将入队节点设置成tail节点,
                   更新失败则表示有其他线程成功更新了tail节点
                   */
                    if (p != t) 
                       // 这儿允许设置tail为最新节点的时候失败，因为添加node的时候是根据p.next是不是为null判断的
                        casTail(t, newNode); 
                    return true;
                }
                // Lost CAS race to another thread; re-read next
            }
            else if (p == q)
                // 虽然q是p.next，但是因为是多线程，在offer的同时也在poll，如offer的时候正好p被poll了，那么在poll方法中的updateHead方法会将head指向当前的q，而把p.next指向自己，即：p.next == p
            // 这个时候就会造成tail在head的前面，需要重新设置p
            // 如果tail已经改变，将p指向tail，但这个时候tail依然可能在head前面
            // 如果tail没有改变，直接将p指向head
                p = (t != (t = tail)) ? t : head;
            else
                // tail已经不是最后一个节点，将p指向最后一个节点
                p = (p != t && t != (t = tail)) ? t : q;
        }
    }
```

从整个源码来看，整个入队过程做了两件事：第一定位出尾节点；第二是使用CAS算法将入队节点设置成尾节点的next节点，不成功就重试。

### 2.定位尾节点

tail节点并不总是尾节点，所以每次入队都必须先通过tail节点来找到尾节点，尾节点可能就是tail节点，也可能是tail节点的next节点。代码中循环体中就是判断tail是否有next节点，有则表示next节点可能是尾节点。获取tail节点的next节点需要注意的是p节点等于p的next节点的情况，只有一种可能就是p节点和p的next节点都等于空，表示这个队列刚初始化，正准备添加第一次节点，所以需要返回head节点。获取p节点的next节点代码如下：

```java
final Node<E> succ(Node<E> p) {
        Node<E> next = p.next;
        return (p == next) ? head : next;
    }
```

### 3.设置入队节点为尾节点

p.casNext(**null**, n)方法用于将入队节点设置为当前队列尾节点的next节点，p如果是null表示p是当前队列的尾节点，如果不为null表示有其他线程更新了尾节点，则需要重新获取当前队列的尾节点。

---

## 四：出队列

出队列的就是**从队列里返回一个节点元素**，并清空该节点对元素的引用。让我们通过每个节点出队的快照来观察下head节点的变化。

![](http://ifeve.com/wp-content/uploads/2013/01/出队列.jpg)

从上图可知，并不是每次出队时都更新head节点，当head节点里有元素时，直接弹出head节点里的元素，而不会更新head节点。只有当head节点里没有元素时，出队操作才会更新head节点。这种做法也是通过hops变量来减少使用CAS更新head节点的消耗，从而提高出队效率。让我们再通过源码来深入分析下出队过程。

```java
public E poll() {
        restartFromHead:
        for (;;) {
            for (Node<E> h = head, p = h, q;;) {
              // p表示头节点，需要出队的节点
              // 	获取p节点的元素
                E item = p.item;
								// 如果p节点的元素不为空，使用CAS设置p节点引用的元素为null
              	// 如果成功则返回p节点的元素
                if (item != null && p.casItem(item, null)) {
           					// 将p节点下一个节点设置为head节点
                    if (p != h) // hop two nodes at a time
                        updateHead(h, ((q = p.next) != null) ? q : p);
                    return item;
                } 
              	// 如果p的下一个节点也为空，说明这个队列已经空了
                else if ((q = p.next) == null) {
                  // 更新头节点
                    updateHead(h, p);
                    return null;
                }
                else if (p == q)
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }

```

首先获取头节点的元素，然后判断头节点元素是否为空，如果为空，表示另外一个线程已经进行了一次出队操作将该节点的元素取走，如果不为空，则使用CAS的方式将头节点的引用设置成null，如果CAS成功，则直接返回头节点的元素，如果不成功，表示另外一个线程已经进行了一次出队操作更新了head节点，导致元素发生了变化，需要重新获取头节点。

----


> 参考:
>
> 《并发编程的艺术》

