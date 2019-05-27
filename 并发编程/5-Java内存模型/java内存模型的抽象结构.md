# java内存模型

## java内存模型

java内存模型(java memory model),JMM ,JMM 决定了一个线程对共享变量的写入何时对另一个线程可见。从抽象的角度来看，JMM定义了线程与主内存之间的抽象关系：线程之间的共享变量存储在主内存中，每个线程都有一个**私有的本地内存**，本地内存并不真实存在。

java内存模型的抽象示意图：

![1548125944592](https://ws1.sinaimg.cn/large/006tNc79ly1g1vb8la9udj30cv0dkjrq.jpg)

从示意图中可知，如果线程A和线程B要通信的话，必须经历两步：

1. 线程A把本地内存A中更新过的共享变量刷新到主内存中去。
2. 线程B到主内存中去读取线程A之前已更新过的共享变量。

如图：

![1548126446062](https://ws4.sinaimg.cn/large/006tNc79ly1g1vb8zdeyrj30d30doq3n.jpg)

当初始的时候：本地内存A和本地内存B和主内存中的X=0，线程A执行完之后，会把值临时放在本地内存中，当线程A需要跟线程B通信的时候，线程A会把自己本地内存中已经修改的值更新到主内存中，然后线程B会去从主内存中去读取线程A更新后的值，然后线程B的本地内存也会变为1。看上去也就变成了线程A和线程B之间的通信。其实是通过中介主内存来进行的。线程之间没有任何的交互。

---

## 从源代码到指令序列的重排序

重排序大概分为3中：

1. 编译器优化的重排序。编译器会在不改变**单线程程序语义**的前提下，重新安排语句的执行顺序。
2. 指令级并行的重排序。现代处理器采用了指令级并行技术来将多条指令重叠执行。如果不存在**数据依赖性**，处理器恶意改变语句对应的**机器指令**的执行顺序。
3. 内存系统的重排序。由于处理器使用缓存和读/写缓冲区，使得加载和存储操作看上去可能是在乱序执行。

从源代码到指令序列的重排序如图：

![1548127634820](https://ws2.sinaimg.cn/large/006tNc79ly1g1vb9kzxbkj30mk03j74f.jpg)

上述的1属于编译器重排序，2和3属于处理器重排序。这些重排序可能会导致多线程程序出现内存可见性问题。对于编译器，JMM的编译器重排序规则会禁止特定类型的编译器重排序（不是所有的编译器重排序都要禁止）。对于处理器重排序，JMM的处理器重排序规则会要求Java编译器在生成指令序列时，插入特定类型的内存屏障（Memory Barriers，Intel称之为Memory Fence）指令，通过内存屏障指令来禁止特定类型的处理器重排序。

-----

## 并发编程模型的分类

为了保证内存可见性，Java编译器在生成指令序列的适当位置会插入内存屏障指令来禁止特定类型的处理器重排序。JMM把内存屏障指令分为4类：

| 屏障类型            | 指令示例                   | 说明                                                         |
| ------------------- | -------------------------- | ------------------------------------------------------------ |
| LoadLoad Barriers   | Load1; LoadLoad; Load2     | 确保Load1数据的装载，之前于Load2及所有后续装载指令的装载。   |
| StoreStore Barriers | Store1; StoreStore; Store2 | 确保Store1数据对其他处理器可见（刷新到内存），之前于Store2及所有后续存储指令的存储。 |
| LoadStore Barriers  | Load1; LoadStore; Store2   | 确保Load1数据装载，之前于Store2及所有后续的存储指令刷新到内存。 |
| StoreLoad Barriers  | Store1; StoreLoad; Load2   | 确保Store1数据对其他处理器变得可见（指刷新到内存），之前于Load2及所有后续装载指令的装载。StoreLoad Barriers会使该屏障之前的所有内存访问指令（存储和装载指令）完成之后，才执行该屏障之后的内存访问指令。 |

StoreLoad 
Barriers是一个“全能型”的屏障，它同时具有其他三个屏障的效果。现代的多处理器大都支持该屏障（其他类型的屏障不一定被所有处理器支持）。执行该屏障开销会很昂贵，因为当前处理器通常要把写缓冲区中的数据全部刷新到内存中（buffer
fully flush）。

---

## happens-before

从JDK5开始，java使用新的JSR -133内存模型（本文除非特别说明，针对的都是JSR- 
133内存模型）。JSR-133提出了happens-before的概念，通过这个概念来阐述操作之间的内存可见性。**如果一个操作执行的结果需要对另一个操作可见，那么这两个操作之间必须存在happens-before关系**。这里提到的两个操作既可以是在一个线程之内，也可以是在不同线程之间。与程序员密切相关的happens-before规则如下：

- 程序顺序规则：一个线程中的每个操作，happens- before 于该线程中的任意后续操作。
- 监视器锁规则：对一个监视器锁的解锁，happens- before 于随后对这个监视器锁的加锁。
- volatile变量规则：对一个volatile域的写，happens- before 于任意后续对这个volatile域的读。
- 传递性：如果A happens- before B，且B happens- before C，那么A happens- before C。

注意，两个操作之间具有happens-before关系，并不意味着前一个操作必须要在后一个操作之前执行！happens-before仅仅要求前一个操作（执行的结果）对后一个操作可见，且前一个操作按顺序排在第二个操作之前.

-----

## 重排序

### 数据依赖性

> 重排序是指编译器和处理器为了优化程序性能而对指令序列进行重新排序的一种手段。

如果两个操作访问同一个变量，且这两个操作中有一个为写操作，此时这两个操作之间就存在数据依赖性。数据依赖分下列三种类型：

| 名称   | 代码示例     | 说明                           |
| ------ | ------------ | :----------------------------- |
| 写后读 | a = 1;b = a; | 写一个变量之后，再读这个位置。 |
| 写后写 |  a = 1;a = 2; |写一个变量之后，再写这个变量。 |
| 读后写 |   a = b;b = 1;           |   读一个变量之后，再写这个变量。|

 上面三种情况，只要重排序两个操作的执行顺序，程序的执行结果将会被改变。

 编译器和处理器可能会对操作做重排序。编译器和处理器在重排序时，**会遵守数据依赖性**，**编译器和处理器不会改变存在数据依赖关系的两个操作的执行顺序。**

---

### as-if-serial

 as-if-serial语义的意思指：不管怎么重排序（编译器和处理器为了提高并行度），（单线程）程序的执行结果不能被改变。编译器，runtime 和处理器都必须遵守as-if-serial语义。

 为了遵守as-if-serial语义，编译器和处理器不会对存在数据依赖关系的操作做重排序，因为这种重排序会改变执行结果。但是，如果操作之间不存在数据依赖关系，这些操作可能被编译器和处理器重排序。为了具体说明，请看下面计算圆面积的代码示例：

```java
double pi  = 3.14;    //A
double r   = 1.0;     //B
double area = pi * r * r; //C
```

 上面三个操作的数据依赖关系如下图所示：

 ![1548133441503](https://ws1.sinaimg.cn/large/006tNc79ly1g1vba7980ij308e07wjrn.jpg)

 如上图所示，A和C之间存在数据依赖关系，同时B和C之间也存在数据依赖关系。因此在最终执行的指令序列中，C不能被重排序到A和B的前面（C排到A和B的前面，程序的结果将会被改变）。但A和B之间没有数据依赖关系，编译器和处理器可以重排序A和B之间的执行顺序。下图是该程序的两种执行顺序：

![1548133592624](https://ws4.sinaimg.cn/large/006tNc79ly1g1vbaijktxj30jh05wgm1.jpg)

 as-if-serial语义把单线程程序保护了起来，遵守as-if-serial语义的编译器，runtime  和处理器共同为编写单线程程序的程序员创建了一个幻觉：单线程程序是按程序的顺序来执行的。as-if-serial语义使单线程程序员无需担心重排序会干扰他们，也无需担心内存可见性问题。



###  程序顺序规则

根据happens- before的程序顺序规则，上面计算圆的面积的示例代码存在三个happens- before关系：

1.  A happens- before B；
2.  B happens- before C；
3.  A happens- before C；

 这里的第3个happens- before关系，是根据happens- before的传递性推导出来的。

 这里A happens- before B，但实际执行时B却可以排在A之前执行（看上面的重排序后的执行顺序）。如果A  happens- before  B，JMM并不要求A一定要在B之前执行。JMM仅仅要求前一个操作（执行的结果）对后一个操作可见，且前一个操作按顺序排在第二个操作之前。这里操作A的执行结果不需要对操作B可见；而且重排序操作A和操作B后的执行结果，与操作A和操作B按happens-  before顺序执行的结果一致。在这种情况下，JMM会认为这种重排序并不非法（not illegal），JMM允许这种重排序。

 在计算机中，软件技术和硬件技术有一个共同的目标：在不改变程序执行结果的前提下，尽可能的开发并行度。编译器和处理器遵从这一目标，从happens- before的定义我们可以看出，JMM同样遵从这一目标。

----

### 重排序对多线程的影响

现在让我们来看看，重排序是否会改变多线程程序的执行结果。请看下面的示例代码：

```java
class ReorderExample {
int a = 0;
boolean flag = false;

public void writer() {
    a = 1;                   //1
    flag = true;             //2
}

Public void reader() {
    if (flag) {                //3
        int i =  a * a;        //4
        ……
    }
}
}
```

 flag变量是个标记，用来标识变量a是否已被写入。这里假设有两个线程A和B，A首先执行writer()方法，随后B线程接着执行reader()方法。线程B在执行操作4时，能否看到线程A在操作1对共享变量a的写入？

 答案是：不一定能看到。

 由于操作1和操作2没有数据依赖关系，编译器和处理器可以对这两个操作重排序；同样，操作3和操作4没有数据依赖关系，编译器和处理器也可以对这两个操作重排序。让我们先来看看，当操作1和操作2重排序时，可能会产生什么效果？请看下面的程序执行时序图：

![1548134403223](https://ws1.sinaimg.cn/large/006tNc79ly1g1vbaxhjqbj30g409yq39.jpg)

 如上图所示，操作1和操作2做了重排序。程序执行时，线程A首先写标记变量flag，随后线程B读这个变量。由于条件判断为真，线程B将读取变量a。此时，变量a还根本没有被线程A写入，在这里多线程程序的语义被重排序破坏了！

 ※注：本文统一用红色的虚箭线表示错误的读操作，用绿色的虚箭线表示正确的读操作。

下面再让我们看看，当操作3和操作4重排序时会产生什么效果（借助这个重排序，可以顺便说明控制依赖性）。下面是操作3和操作4重排序后，程序的执行时序图：

![1548134787155](https://ws1.sinaimg.cn/large/006tNc79ly1g1vbb88nfvj30n40c7gmq.jpg)

 在程序中，操作3和操作4存在控制依赖关系。当代码中存在控制依赖性时，会影响指令序列执行的并行度。为此，编译器和处理器会采用猜测（Speculation）执行来克服控制相关性对并行度的影响。以处理器的猜测执行为例，执行线程B的处理器可以提前读取并计算a*a，然后把计算结果临时保存到一个名为重排序缓冲（reorder  buffer ROB）的硬件缓存中。当接下来操作3的条件判断为真时，就把该计算结果写入变量i中。

 从图中我们可以看出，猜测执行实质上对操作3和4做了重排序。重排序在这里破坏了多线程程序的语义！

 在单线程程序中，对存在控制依赖的操作重排序，不会改变执行结果（这也是as-if-serial语义允许对存在控制依赖的操作做重排序的原因）；但在多线程程序中，对存在控制依赖的操作重排序，可能会改变程序的执行结果。

----

## 顺序一致性

> 顺序一致性内存模型是一个理论参考模型，在设计的时候，处理器的内存模型和编程语言的内存模型都会以顺序一致性内存模型作为参照。 

### 数据竞争与顺序一致性 

当程序未正确同步时，就可能会存在数据竞争。Java内存模型规范对数据竞争的定义如下。

在一个线程中写一个变量，在另一个线程读同一个变量，而且写和读没有通过同步来排序。

当代码中包含数据竞争时，程序的执行往往产生违反直觉的结果（前一章的示例正是如此）。如果一个多线程程序能正确同步，这个程序将是一个没有数据竞争的程序。
JMM对正确同步的多线程程序的内存一致性做了如下保证。

如果程序是正确同步的，程序的执行将具有顺序一致性（Sequentially Consistent）——即程序的执行结果与该程序在顺序一致性内存模型中的执行结果相同。马上我们就会看到，这对于程序员来说是一个极强的保证。这里的同步是指广义上的同步，包括对常用同步原语（synchronized、volatile和final）的正确使用。 

----

### 顺序一致性内存模型 

顺序一致性内存模型是一个被计算机科学家理想化了的理论参考模型，它为程序员提供
了极强的内存可见性保证。顺序一致性内存模型有两大特性。
1）一个线程中的所有操作必须按照程序的顺序来执行。
2）（不管程序是否同步）所有线程都只能看到一个单一的操作执行顺序。在顺序一致性内存模型中，每个操作都必须原子执行且立刻对所有线程可见。

顺序一致性内存模型为程序员提供的视图如图3-10所示。

![1548136358695](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbbpmkscj30dv0c275g.jpg)

在概念上，顺序一致性模型有一个单一的全局内存，这个内存通过一个左右摆动的开关
可以连接到任意一个线程，同时每一个线程必须按照程序的顺序来执行内存读/写操作。从上面的示意图可以看出，在任意时间点最多只能有一个线程可以连接到内存。当多个线程并发执行时，图中的开关装置能把所有线程的所有内存读/写操作串行化（即在顺序一致性模型中，所有操作之间具有全序关系）。

为了更好进行理解，下面通过两个示意图来对顺序一致性模型的特性做进一步的说明。假设有两个线程A和B并发执行。其中A线程有3个操作，它们在程序中的顺序是：A1→A2→A3。B线程也有3个操作，它们在程序中的顺序是：B1→B2→B3。

假设这两个线程使用监视器锁来正确同步：A线程的3个操作执行后释放监视器锁，随后B线程获取同一个监视器锁。那么程序在顺序一致性模型中的执行效果将如图3-11所示。 

![1548136394468](https://ws2.sinaimg.cn/large/006tNc79ly1g1vbc4nl7pj30gl09a41v.jpg)

现在我们再假设这两个线程没有做同步，下面是这个未同步程序在顺序一致性模型中的执行示意图，如图3-12所示。

![1548136415100](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbclibutj30gn099dja.jpg)

未同步程序在顺序一致性模型中虽然整体执行顺序是无序的，但所有线程都只能看到一个一致的整体执行顺序。以上图为例，线程A和B看到的执行顺序都是：B1→A1→A2→B2→A3→B3。之所以能得到这个保证是因为顺序一致性内存模型中的每个操作必须立即对任意线程可见。

但是，在JMM中就没有这个保证。未同步程序在JMM中不但整体的执行顺序是无序的，而且所有线程看到的操作执行顺序也可能不一致。比如，在当前线程把写过的数据缓存在本地内存中，在没有刷新到主内存之前，这个写操作仅对当前线程可见；从其他线程的角度来观察，会认为这个写操作根本没有被当前线程执行。只有当前线程把本地内存中写过的数据刷新到主内存之后，这个写操作才能对其他线程可见。在这种情况下，当前线程和其他线程看到的操作执行顺序将不一致。

### 同步程序的顺序一致性效果 

下面，对前面的示例程序ReorderExample用锁来同步，看看正确同步的程序如何具有顺序一致性。 
请看下面的示例代码。

```java
class SynchronizedExample{
    int a = 0;
    boolean flag = false;
    public synchronized void writer(){// 获取锁
        a = 1;
        flag = true;
    }// 释放锁
    
    public synchronized void reader(){//获取锁
        if(flag){
            int i = a;
            .....
        }// 释放锁
    }
}
```

在上面示例代码中，假设A线程执行writer()方法后，B线程执行reader()方法。这是一个正确同步的多线程程序。根据JMM规范，该程序的执行结果将与该程序在顺序一致性模型中的执行结果相同。下面是该程序在两个内存模型中的执行时序对比图，如图3-13所示。

顺序一致性模型中，所有操作完全按程序的顺序串行执行。而在JMM中，临界区内的代码可以重排序（但JMM不允许临界区内的代码“逸出”到临界区之外，那样会破坏监视器的语义）。JMM会在退出临界区和进入临界区这两个关键时间点做一些特别处理，使得线程在这两个时间点具有与顺序一致性模型相同的内存视图（具体细节后文会说明）。虽然线程A在临界区内做了重排序，但由于监视器互斥执行的特性，这里的线程B根本无法“观察”到线程A在临界区内的重排序。这种重排序既提高了执行效率，又没有改变程序的执行结果。

![1548136704667](https://ws2.sinaimg.cn/large/006tNc79ly1g1vbd7ytq6j30hb0dgju5.jpg)

从这里我们可以看到，JMM在具体实现上的基本方针为：在不改变（正确同步的）程序执行结果的前提下，尽可能地为编译器和处理器的优化打开方便之门。

### 未同步程序的执行特性 

对于未同步或未正确同步的多线程程序，JMM只提供最小安全性：线程执行时读取到的
值，要么是之前某个线程写入的值，要么是默认值（0，Null，False），JMM保证线程读操作读取到的值不会无中生有（Out Of Thin Air）的冒出来。为了实现最小安全性，JVM在堆上分配对象时，首先会对内存空间进行清零，然后才会在上面分配对象（JVM内部会同步这两个操作）。因此，在已清零的内存空间（Pre-zeroed Memory）分配对象时，域的默认初始化已经完成了。

JMM不保证未同步程序的执行结果与该程序在顺序一致性模型中的执行结果一致。因为
如果想要保证执行结果一致，JMM需要禁止大量的处理器和编译器的优化，这对程序的执行性能会产生很大的影响。而且未同步程序在顺序一致性模型中执行时，整体是无序的，其执行结果往往无法预知。而且，保证未同步程序在这两个模型中的执行结果一致没什么意义。

未同步程序在JMM中的执行时，整体上是无序的，其执行结果无法预知。未同步程序在两个模型中的执行特性有如下几个差异。

1. 顺序一致性模型保证单线程内的操作会按程序的顺序执行，而JMM不保证单线程内的操作会按程序的顺序执行（比如上面正确同步的多线程程序在临界区内的重排序）。这一点前面已经讲过了，这里就不再赘述。

2. 顺序一致性模型保证所有线程只能看到一致的操作执行顺序，而JMM不保证所有线程能看到一致的操作执行顺序。这一点前面也已经讲过，这里就不再赘述。

3. JMM不保证对64位的long型和double型变量的写操作具有原子性，而顺序一致性模型保证对所有的内存读/写操作都具有原子性。

第3个差异与处理器总线的工作机制密切相关。在计算机中，数据通过总线在处理器和内
存之间传递。每次处理器和内存之间的数据传递都是通过一系列步骤来完成的，这一系列步骤称之为总线事务（Bus Transaction）。总线事务包括读事务（Read Transaction）和写事务（Write Transaction）。读事务从内存传送数据到处理器，写事务从处理器传送数据到内存，每个事务会读/写内存中一个或多个物理上连续的字。这里的关键是，总线会同步试图并发使用总线的事务。在一个处理器执行总线事务期间，总线会禁止其他的处理器和I/O设备执行内存的读/写。
下面，让我们通过一个示意图来说明总线的工作机制，如图3-14所示。 

![1548136829986](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbdrkkcdj30fa0f2tag.jpg)

由图可知，假设处理器A，B和C同时向总线发起总线事务，这时总线仲裁（Bus Arbitration）会对竞争做出裁决，这里假设总线在仲裁后判定处理器A在竞争中获胜（总线仲裁会确保所有处理器都能公平的访问内存）。此时处理器A继续它的总线事务，而其他两个处理器则要等待处理器A的总线事务完成后才能再次执行内存访问。假设在处理器A执行总线事务期间（不管这个总线事务是读事务还是写事务），处理器D向总线发起了总线事务，此时处理器D的请求会被总线禁止。

总线的这些工作机制可以把所有处理器对内存的访问以串行化的方式来执行。在任意时
间点，最多只能有一个处理器可以访问内存。这个特性确保了单个总线事务之中的内存读/写操作具有原子性。

在一些32位的处理器上，如果要求对64位数据的写操作具有原子性，会有比较大的开销。为了照顾这种处理器，Java语言规范鼓励但不强求JVM对64位的long型变量和double型变量的写操作具有原子性。当JVM在这种处理器上运行时，可能会把一个64位long/double型变量的写操作拆分为两个32位的写操作来执行。这两个32位的写操作可能会被分配到不同的总线事务中执行，此时对这个64位变量的写操作将不具有原子性。

当单个内存操作不具有原子性时，可能会产生意想不到后果。请看示意图，如图3-15所示：

![1548136859818](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbe58wxvj30h00a4ab5.jpg)

如上图所示，假设处理器A写一个long型变量，同时处理器B要读这个long型变量。处理器A中64位的写操作被拆分为两个32位的写操作，且这两个32位的写操作被分配到不同的写事务中执行。同时，处理器B中64位的读操作被分配到单个的读事务中执行。当处理器A和B按上图的时序来执行时，处理器B将看到仅仅被处理器A“写了一半”的无效值。

注意，在JSR-133之前的旧内存模型中，一个64位long/double型变量的读/写操作可以被拆分为两个32位的读/写操作来执行。从JSR-133内存模型开始（即从JDK5开始），仅仅只允许把一个64位long/double型变量的写操作拆分为两个32位的写操作来执行，任意的读操作在JSR-133中都必须具有原子性（即任意读操作必须要在单个读事务中执行）。

----

## volatile的内存语义（重点）

### volatile的特性

理解volatile特性的一个好方法是：把对volatile变量的单个读/写，看成是使用同一个监视器锁对这些单个读/写操作做了同步。下面我们通过具体的示例来说明，请看下面的示例代码：

```java
class VolatileFeaturesExample {
    volatile long vl = 0L;  //使用volatile声明64位的long型变量
    public void set(long l) {
        vl = l;   //单个volatile变量的写
    }
    public void getAndIncrement () {
        vl++;    //复合（多个）volatile变量的读/写
    }
    public long get() {
        return vl;   //单个volatile变量的读
    }
}
```

假设有多个线程分别调用上面程序的三个方法，这个程序在语意上和下面程序等价：

```java
class VolatileFeaturesExample {
    long vl = 0L;               // 64位的long型普通变量

    public synchronized void set(long l) {     //对单个的普通 变量的写用同一个监视器同步
        vl = l;
    }

    public void getAndIncrement () { //普通方法调用
        long temp = get();           //调用已同步的读方法
        temp += 1L;                  //普通写操作
        set(temp);                   //调用已同步的写方法
    }
    public synchronized long get() { 
    //对单个的普通变量的读用同一个监视器同步
        return vl;
    }
}
```

 如上面示例程序所示，对一个volatile变量的单个读/写操作，与对一个普通变量的读/写操作使用同一个监视器锁来同步，它们之间的执行效果相同。

 监视器锁的happens-before规则保证释放监视器和获取监视器的两个线程之间的内存可见性，这意味着对一个volatile变量的读，总是能看到（任意线程）对这个volatile变量最后的写入。

 监视器锁的语义决定了临界区代码的执行具有原子性。这意味着即使是64位的long型和double型变量，只要它是volatile变量，对该变量的读写就将具有原子性。如果是多个volatile操作或类似于volatile++这种复合操作，这些操作整体上不具有原子性。

 简而言之，volatile变量自身具有下列特性：

-  可见性。**对一个volatile变量的读，总是能看到（任意线程）对这个volatile变量最后的写入。**
-  原子性：**对任意单个volatile变量的读/写具有原子性，但类似于volatile++这种复合操作不具有原子性。**

---



### volatile写-读建立的happens before关系

 上面讲的是volatile变量自身的特性，对程序员来说，volatile对线程的内存可见性的影响比volatile自身的特性更为重要，也更需要我们去关注。

 从JSR-133开始，volatile变量的写-读可以实现线程之间的通信。

 从内存语义的角度来说，volatile与监视器锁有相同的效果：volatile写和监视器的释放有相同的内存语义；volatile读与监视器的获取有相同的内存语义。

 请看下面使用volatile变量的示例代码：

```java
class VolatileExample {
    int a = 0;
    volatile boolean flag = false;
    public void writer() {
        a = 1;                   //1
        flag = true;               //2
    }
    public void reader() {
        if (flag) {                //3
            int i =  a;           //4
            ……
        }
    }
}
```

 假设线程A执行writer()方法之后，线程B执行reader()方法。根据happens before规则，这个过程建立的happens before 关系可以分为两类：

1.  根据程序次序规则，1 happens before 2; 3 happens before 4。
2.  根据volatile规则，2 happens before 3。
3.  根据happens before 的传递性规则，1 happens before 4。

![1548137781771](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbokpxdpj30cx0cg405.jpg)

 在上图中，每一个箭头链接的两个节点，代表了一个happens before 关系。黑色箭头表示程序顺序规则；橙色箭头表示volatile规则；蓝色箭头表示组合这些规则后提供的happens before保证。

 这里A线程写一个volatile变量后，B线程读同一个volatile变量。A线程在写volatile变量之前所有可见的共享变量，在B线程读同一个volatile变量后，将立即变得对B线程可见。

----

### volatile写-读的内存语义

 volatile写的内存语义如下：

-  当写一个volatile变量时，JMM会把该线程对应的本地内存中的共享变量刷新到主内存。

 以上面示例程序VolatileExample为例，假设线程A首先执行writer()方法，随后线程B执行reader()方法，初始时两个线程的本地内存中的flag和a都是初始状态。下图是线程A执行volatile写后，共享变量的状态示意图：

![1548137861489](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbpnjrtjj30hb0f8my3.jpg)

 如上图所示，线程A在写flag变量后，本地内存A中被线程A更新过的两个共享变量的值被刷新到主内存中。此时，本地内存A和主内存中的共享变量的值是一致的。

 volatile读的内存语义如下：

-  当读一个volatile变量时，JMM会把该线程对应的本地内存置为无效。线程接下来将从主内存中读取共享变量。

  下面是线程B读同一个volatile变量后，共享变量的状态示意图：

![1548137960200](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbqd31hlj30hj0futaf.jpg)

 如上图所示，在读flag变量后，本地内存B已经被置为无效。此时，线程B必须从主内存中读取共享变量。线程B的读取操作将导致本地内存B与主内存中的共享变量的值也变成一致的了。

 如果我们把volatile写和volatile读这两个步骤综合起来看的话，在读线程B读一个volatile变量后，写线程A在写这个volatile变量之前所有可见的共享变量的值都将立即变得对读线程B可见。

 下面对volatile写和volatile读的内存语义做个总结：

-  线程A写一个volatile变量，实质上是线程A向接下来将要读这个volatile变量的某个线程发出了（其对共享变量所在修改的）消息。
-  线程B读一个volatile变量，实质上是线程B接收了之前某个线程发出的（在写这个volatile变量之前对共享变量所做修改的）消息。
-  线程A写一个volatile变量，随后线程B读这个volatile变量，这个过程实质上是线程A通过主内存向线程B发送消息。

---

### volatile内存语义的实现

 下面，让我们来看看JMM如何实现volatile写/读的内存语义。

 前文我们提到过重排序分为编译器重排序和处理器重排序。为了实现volatile内存语义，JMM会分别限制这两种类型的重排序类型。下面是JMM针对编译器制定的volatile重排序规则表：

| 是否能重排序 | 第二个操作            |             |             |
| :----------- | ---------- | ---- | ---- |
| 第一个操作   | 普通读/写 | volatile读 |     	volatile写        |
| 普通读/写    |            |            | NO |
| volatile读   | NO | NO | NO |
| volatile写   |            | NO | NO |

 举例来说，第三行最后一个单元格的意思是：在程序顺序中，当第一个操作为普通变量的读或写时，如果第二个操作为volatile写，则编译器不能重排序这两个操作。

 从上表我们可以看出：

-  当第二个操作是volatile写时，不管第一个操作是什么，都不能重排序。这个规则确保volatile写之前的操作不会被编译器重排序到volatile写之后。
-  当第一个操作是volatile读时，不管第二个操作是什么，都不能重排序。这个规则确保volatile读之后的操作不会被编译器重排序到volatile读之前。
-  当第一个操作是volatile写，第二个操作是volatile读时，不能重排序。

 为了实现volatile的内存语义，编译器在生成字节码时，会在指令序列中插入内存屏障来禁止特定类型的处理器重排序。对于编译器来说，发现一个最优布置来最小化插入屏障的总数几乎不可能，为此，JMM采取保守策略。下面是基于保守策略的JMM内存屏障插入策略：

-  在每个volatile写操作的前面插入一个StoreStore屏障。
-  在每个volatile写操作的后面插入一个StoreLoad屏障。
-  在每个volatile读操作的后面插入一个LoadLoad屏障。
-  在每个volatile读操作的后面插入一个LoadStore屏障.

上述内存屏障插入策略非常保守，但它可以保证在任意处理器平台，任意的程序中都能得到正确的volatile内存语义

下面是保守策略下，volatile写插入内存屏障后生成的指令序列示意图：

![1548138477693](https://ws2.sinaimg.cn/large/006tNc79ly1g1vbr7ai54j30hf0ax0uc.jpg)

 上图中的StoreStore屏障可以保证在volatile写之前，其前面的所有普通写操作已经对任意处理器可见了。这是因为StoreStore屏障将保障上面所有的普通写在volatile写之前刷新到主内存。

 这里比较有意思的是volatile写后面的StoreLoad屏障。这个屏障的作用是避免volatile写与后面可能有的volatile读/写操作重排序。因为编译器常常无法准确判断在一个volatile写的后面，是否需要插入一个StoreLoad屏障（比如，一个volatile写之后方法立即return）。为了保证能正确实现volatile的内存语义，JMM在这里采取了保守策略：在每个volatile写的后面或在每个volatile读的前面插入一个StoreLoad屏障。从整体执行效率的角度考虑，JMM选择了在每个volatile写的后面插入一个StoreLoad屏障。因为volatile写-读内存语义的常见使用模式是：一个写线程写volatile变量，多个读线程读同一个volatile变量。当读线程的数量大大超过写线程时，选择在volatile写之后插入StoreLoad屏障将带来可观的执行效率的提升。从这里我们可以看到JMM在实现上的一个特点：首先确保正确性，然后再去追求执行效率。

下面是在保守策略下，volatile读插入内存屏障后生成的指令序列示意图：

![1548138504852](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbrqhtkyj30hp0altad.jpg)

 上图中的LoadLoad屏障用来禁止处理器把上面的volatile读与下面的普通读重排序。LoadStore屏障用来禁止处理器把上面的volatile读与下面的普通写重排序。

 上述volatile写和volatile读的内存屏障插入策略非常保守。在实际执行时，只要不改变volatile写-读的内存语义，编译器可以根据具体情况省略不必要的屏障。下面我们通过具体的示例代码来说明：

```java
class VolatileBarrierExample {
    int a;
    volatile int v1 = 1;
    volatile int v2 = 2;
    void readAndWrite() {
        int i = v1;           //第一个volatile读
        int j = v2;           // 第二个volatile读
        a = i + j;            //普通写
        v1 = i + 1;          // 第一个volatile写
        v2 = j * 2;          //第二个 volatile写
    }

    …                    //其他方法
}
```

 针对readAndWrite()方法，编译器在生成字节码时可以做如下的优化：

![1548138573261](https://ws4.sinaimg.cn/large/006tNc79ly1g1vbtxs61mj30fa0e20x0.jpg)

 注意，最后的StoreLoad屏障不能省略。因为第二个volatile写之后，方法立即return。此时编译器可能无法准确断定后面是否会有volatile读或写，为了安全起见，编译器常常会在这里插入一个StoreLoad屏障。

 上面的优化是针对任意处理器平台，由于不同的处理器有不同“松紧度”的处理器内存模型，内存屏障的插入还可以根据具体的处理器内存模型继续优化。以x86处理器为例，上图中除最后的StoreLoad屏障外，其它的屏障都会被省略。

 前面保守策略下的volatile读和写，在 x86处理器平台可以优化成：

![1548138631599](https://ws1.sinaimg.cn/large/006tNc79ly1g1vbu5wszkj30fb0chaay.jpg)

 前文提到过，x86处理器仅会对写-读操作做重排序。X86不会对读-读，读-写和写-写操作做重排序，因此在x86处理器中会省略掉这三种操作类型对应的内存屏障。在x86中，JMM仅需在volatile写后面插入一个StoreLoad屏障即可正确实现volatile写-读的内存语义。这意味着在x86处理器中，volatile写的开销比volatile读的开销会大很多（因为执行StoreLoad屏障开销会比较大）。

### JSR-133为什么要增强volatile的内存语义

 在JSR-133之前的旧Java内存模型中，虽然不允许volatile变量之间重排序，但旧的Java内存模型允许volatile变量与普通变量之间重排序。在旧的内存模型中，VolatileExample示例程序可能被重排序成下列时序来执行：

![1548138828551](https://ws1.sinaimg.cn/large/006tNc79ly1g1vbudw5hzj30hd0fc410.jpg)

 在旧的内存模型中，当1和2之间没有数据依赖关系时，1和2之间就可能被重排序（3和4类似）。其结果就是：读线程B执行4时，不一定能看到写线程A在执行1时对共享变量的修改。

 因此在旧的内存模型中  ，volatile的写-读没有监视器的释放-获所具有的内存语义。为了提供一种比监视器锁更轻量级的线程之间通信的机制，JSR-133专家组决定增强volatile的内存语义：严格限制编译器和处理器对volatile变量与普通变量的重排序，确保volatile的写-读和监视器的释放-获取一样，具有相同的内存语义。从编译器重排序规则和处理器内存屏障插入策略来看，只要volatile变量与普通变量之间的重排序可能会破坏volatile的内存语意，这种重排序就会被编译器重排序规则和处理器内存屏障插入策略禁止。

 由于volatile仅仅保证对单个volatile变量的读/写具有原子性，而监视器锁的互斥执行的特性可以确保对整个临界区代码的执行具有原子性。在功能上，监视器锁比volatile更强大；在可伸缩性和执行性能上，volatile更有优势。如果读者想在程序中用volatile代替监视器锁，请一定谨慎。

## 锁的内存语义

### 锁的释放——获取建立的happens-before关系

锁是Java并发编程中最重要的同步机制。锁除了让临界区互斥执行外，还可以让释放锁的线程向获取同一个锁的线程发送消息。

下面是锁释放——获取的示例代码。

```java
class MonitorExample {
    int a = 0;
    public synchronized void writer() {    // 1
        a++;                            // 2
    }                                    // 3
    public synchronized void reader() {    // 4
        int i = a;                        // 5
        ...                                
    }                                    // 6
}
```

假设线程A执行writer()方法，随后线程B执行reader()方法。根据happens-before规则，这个过程包含的happens-before关系可以分为3类。

- 根据程序次序规则，1 happens-before 2, 2 happens-before 3; 4 happens-before 5, 5 happens-before 6。
- 根据监视器锁规则，3 happens-before 4。
- 根据happens-before的传递性，2 happens-before 5。

上述happens-before关系的图形化表现如下图所示。

![1548138880059](https://ws4.sinaimg.cn/large/006tNc79ly1g1vbumujhwj30hn0dzwf3.jpg)

在上图中，每一个箭头链接的两个节点，代表了一个happens-before关系。黑色箭头表示程序顺序规则；橙色箭头表示监视器锁规则；蓝色箭头表示组合这些规则后提供的happens-before保证。

上图表示在线程A释放了锁之后，随后线程B获取同一个锁。在上图中，2 happens-before 5。因此，线程A在释放锁之前所有可见的共享变量，在线程B获取同一个锁之后，将立刻变得对B线程可见。

### 锁的释放和获取的内存语义

当线程释放锁时，JMM会把该线程对应的本地内存中的共享变量刷新到主内存中。以上面的MonitorExample程序为例，A线程释放锁后，共享数据的状态示意图如下所示：

![1548138898308](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbw0kqkrj30ho0dd3zn.jpg)

当线程获取锁时，JMM会把该线程对应的本地内存置为无效。从而使得被监视器保护的临界区代码必须从主内存中读取共享变量。下图是锁获取的状态示意图。

![1548138974348](https://ws4.sinaimg.cn/large/006tNc79ly1g1vbw7jtvnj30h10drtaq.jpg)

对比锁释放—获取的内存语义与volatile写-读的内存语义可以看出：锁释放与volatile写有相同的内存语义：锁获取与volatile读有相同的内存语义。

下面对锁释放和锁获取的内存语义做个总结。

- 线程A释放一个锁，实质上是线程A向接下来将要获取这个锁的某个线程发出了（线程A对共享变量所作修改的）消息。
- 线程B获取一个锁，实质上是线程B接收了之前某个线程发出的（在释放这个锁之前对共享变量所作修改的）消息。
- 线程A释放锁，随后线程B获取这个锁，这个过程实质上是线程A通过主内存向线程B发送消息。

### 锁内存语义的实现

将借助ReentrantLock的源代码，来分析所内存语义的具体实现机制。

请看下面的示例代码：

```java
public class ReentrantLockExample {
	int a = 0;
	ReentrantLock lock = new ReentrantLock();
	
	public void writer() {
		this.lock.lock();	// 获取锁
		try {
			a++;
		} finally {
			this.lock.unlock();	// 释放锁
		}
	}
	public void reader() {
		this.lock.lock();	// 获取锁
		try {
			int i = a;
		} finally {
			this.lock.unlock(); // 释放锁
		}
	}
}
```

在ReentrantLock中，调用lock()方法获取锁；调用unlock()方法释放锁。

ReentrantLock的实现依赖于Java同步器框架AbstractQueuedSynchronizer（本文简称之为AQS）。AQS使用一个整型的volatile变量（命名为state）来维护同步状态，马上我们会看到，这个volatile变量是ReentrantLock内存语义实现的关键。

下图是ReentrantLock的类图（仅显示与本文相关的部分）

![1548138974348](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbwzny8vj30h10dr3z5.jpg)ReentrantLock分为公平锁和非公平锁，我们首先分析公平锁。

使用公平锁时，加锁方法lock()调用轨迹如下。

1. ReentrantLock:lock()。
2. FairSync:lock()。
3. AbstractQueuedSynchronizer:acquire(int arg)。
4. ReentrantLock:tryAcquire(int acquire)。

在第4步真正开始加锁，下面是该方法的源代码。

```java
protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
               int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
```

从上面源代码中我们可以看出，加锁方法首先读volatile变量state。

在使用公平锁时，解锁方法unlock()调用轨迹如下。

1. ReentrantLock:unlock()。
2. AbstractQueuedSynchronizer:release(int arg)。
3. Sync:tryRelease(int releases)。

在第3步真正开始释放锁，下面是该方法的源代码。

```java
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }
```

从上面的源代码可以看出，在释放锁的最后写volatile变量state。

公平锁在释放锁的最后写volatile变量state，在获取锁时首先读这个volatile变量。根据volatile的happens-before规则，释放锁的线程在写volatile变量之前可见的共享变量，在获取锁的线程读取同一个volatile变量后将立即变得对获取锁的线程可见。

现在我们来分析非公平锁的内存语义的实现。非公平锁的释放和公平锁完全一样，所以这里仅仅分析非公平锁的获取。使用非公平锁时，加锁方法lock()调用轨迹如下。

1. ReentrantLock:lock()。
2. NofairSync:lock()。
3. AbstractQueuedSynchronizer:compareAndSetState(int expect, int update)。

在第3步真正开始加锁，下面是该方法的源代码。

```java
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }
```

该方法以原子操作的方式更新state变量，本文把Java的compareAndSet()方法调用简称为CAS。JDK文档对该方法的说明如下：如果当前状态值等于预期值，则以原子方式将同步状态设置为给定的更新值。此操作具有volatile读和写的内存语义。

这里我们分别从编译器和处理器的角度来分析，CAS如何同时具有volatile读和volatile写的内存语义。

前文我们提到过，编译器不会对volatile读与volatile读后面的任意内存操作重排序；编译器不会对volatile写与volatile写前面的任意内存操作重排序。组合这两个条件，意味着为了同时实现volatile读和volatile写的内存语义，编译器不能对CAS与CAS前面和后面的任意内存操作重排序。

下面我们来分析在常见的intel X86处理器中，CAS是如何同时具有volatile读和volatile写的内存语义的。

下面是sun.misc.Unsafe类的compareAndSwapInt()方法的源代码。

```java
public final native boolean compareAndSwapInt(Object o, long offset, int expected, int x);
```

可以看到，这时一个本地方法调用。这个本地方法在openjdk中依次调用的c++代码为：unsafe.cpp，atomic.cpp和atomic_windows_x86.inline.hpp。这个本地方法的最终实现在openjdk的如下位置openjdk\hotspot\src\os_cpu\windows_x86\vm\atomic_windows_x86.inline.hpp（对应于Windows操作系统，X86处理器）。下面是对应于intel
X86处理器的源代码的片段。

![1548139130616](https://ws2.sinaimg.cn/large/006tNc79ly1g1vbxcoequj30il05674k.jpg)

如上面源代码所示，程序会根据当前处理器的类型来决定是否为cmpxchg指令添加lock前缀。如果程序是在多处理器是在多处理器上运行，就为cmpxchg指令加上lock前缀（Lock   Cmpxchg）。反之，如果程序是在单处理器上运行，就省略lock前缀（单处理器自身会维护单处理器内的顺序一致性，不需要lock前缀提供的内存屏障效果）。

intel的手册对lock前缀的说明如下。

1. 确保对内存的读-改-写操作原子执行。在Pentium及Pentium之前的处理器中，带有lock前缀的指令在执行期间会锁住总线，使得其他处理器暂时无法通过总线访问内存。很显然，这会带来昂贵的开销。从Pentium  4、Intel Xeon及P6处理器开始，intel使用缓存锁定来保证指令执行的原子性。缓存锁定将大大降低lock前缀指令的执行开销。
2. 禁止该指令，与之前和之后的读和写指令重排序。
3. 把写缓冲区中的所有数据刷新到内存中。

上面的第2点和第3点所具有的内存屏障效果，足以同时实现volatile读和volatile写的内存语义。

经过上面的分析，现在我们终于能明白为什么JDK文档说CAS同时具有volatile读和volatile写的内存语义了。
 现在对公平锁和非公平锁的内存语义做个总结。

- 公平锁和非公平锁释放时，最后都要写一个volatile变量state。
- 公平锁获取时，首先会去读volatile变量。
- 非公平锁获取时，首先会用CAS更新volatile变量，这个操作同时具有volatile读和volatile写的内存语义。

从本文对ReentrantLock的分析可以看出，锁释放-获取的内存语义的实现至少有下面两种方式。

- 利用volatile变量的写-读所具有的内存语义。
- 利用CAS所附带的volatile读和volatile写的内存语义。

### concurrent包的实现

由于Java的CAS同时具有volatile读和volatile写的内存语义，因此Java线程之间的通信现在有了下面4种方式。

- A线程写volatile变量，随后B线程读这个volatile变量。
- A线程写volatile变量，随后B线程用CAS更新这个volatile变量。
- A线程用CAS更新一个volatile变量，随后B线程用CAS更新这个volatile变量。
- A线程用CAS更新一个volatile变量，随后B线程读这个volatile变量。

Java的CAS会使用现代处理器上提供的高效机器级别的原子指令，这些原子指令以原子方式对内存执行读-改-写操作，这时在多处理器中实现同步的关键（从本质上来说，能够支持原子性读-改-写指令的计算机，是顺序计算图灵机的异步等价机器，因此任何现代的处理器都会去支持某种能对内存执行原子性读-改-写操作的原子指令）。同时，volatile变量的读/写和CAS可以实现线程之间的通信。把这些特性整合在一起，就形成了整个concurrent包得以实现的基石。如果我们仔细分析concurrent包的源代码实现，会发现一个通用化的实现模式。

首先，声明共享变量为volatile。

然后，使用CAS的原子条件更新来实现线程之间的同步。

同时，配合以volatile的读/写和CAS所具有的volatile读和写的内存语义来实现线程之间的通信。

AQS，非阻塞数据结构和原子变量类（java.util.concurrent.atomic包中的类），这些concurrent包中的基础类都是使用这种模式来实现的，而concurrent包中的高层类又是依赖于这些基础类来实现的。从整体来看，concurrent包的实现示意图如下所示。

![1548139189398](https://ws3.sinaimg.cn/large/006tNc79ly1g1vbxj7w4ij30i60d00tv.jpg)

----

> 《并发编程的艺术》
>
>   JSR-133
>
> 《深入了解Java虚拟机》