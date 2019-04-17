# ConcurrentHashMap

> 本文基于JDK1.8

## 一：前言

本文基于JDK8，与JDK6的版本有很大的差异。它摒弃了Segment（锁段）的概念，利用CAS算法来解决并发。但底层依然由“数组”+链表+红黑树的方式思想。

-----

## 二：数据结构

ConcurrentHashMap相比HashMap而言，是多线程安全的，其底层数据与HashMap的数据结构相同，数据结构如下:

![image-20190405091916200](https://ws4.sinaimg.cn/large/006tNc79ly1g1rht3d42dj30r80oitad.jpg)



## 三：ConcurrentHashMap原理

### 1.常见的属性

```java
private static final long serialVersionUID = 7249069246763182397L;
// 表的最大容量
private static final int MAXIMUM_CAPACITY = 1 << 30;
// 默认表的大小
private static final int DEFAULT_CAPACITY = 16;
// 最大数组大小
static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
// 默认并发数
private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
// 装载因子
private static final float LOAD_FACTOR = 0.75f;
// 转化为红黑树的阈值
static final int TREEIFY_THRESHOLD = 8;
// 由红黑树转化为链表的阈值
static final int UNTREEIFY_THRESHOLD = 6;
// 转化为红黑树的表的最小容量
static final int MIN_TREEIFY_CAPACITY = 64;
// 每次进行转移的最小值
private static final int MIN_TRANSFER_STRIDE = 16;
// 生成sizeCtl所使用的bit位数
private static int RESIZE_STAMP_BITS = 16;
// 进行扩容所允许的最大线程数
private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;
// 记录sizeCtl中的大小所需要进行的偏移位数
private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;    
// 一系列的标识
static final int MOVED     = -1; // hash for forwarding nodes
static final int TREEBIN   = -2; // hash for roots of trees
static final int RESERVED  = -3; // hash for transient reservations
static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

// 获取可用的CPU个数
static final int NCPU = Runtime.getRuntime().availableProcessors();
// 
/** For serialization compatibility. */
// 进行序列化的属性
private static final ObjectStreamField[] serialPersistentFields = {
  new ObjectStreamField("segments", Segment[].class),
  new ObjectStreamField("segmentMask", Integer.TYPE),
  new ObjectStreamField("segmentShift", Integer.TYPE)
};

// 表
transient volatile Node<K,V>[] table;
// 下一个表
private transient volatile Node<K,V>[] nextTable;

// 基本计数
private transient volatile long baseCount;

//hash表初始化或扩容时的一个控制位标识量。
 //负数代表正在进行初始化或扩容操作
 //-1代表正在初始化
 //-N 表示有N-1个线程正在进行扩容操作
 //正数或0代表hash表还没有被初始化，这个数值表示初始化或下一次进行扩容的大小
// 对表初始化和扩容控制
private transient volatile int sizeCtl;

// 扩容下另一个表的索引
private transient volatile int transferIndex;

// 旋转锁
private transient volatile int cellsBusy;

// counterCell表
private transient volatile CounterCell[] counterCells;

// views
// 视图
private transient KeySetView<K,V> keySet;
private transient ValuesView<K,V> values;
private transient EntrySetView<K,V> entrySet;

// Unsafe mechanics
private static final sun.misc.Unsafe U;
private static final long SIZECTL;
private static final long TRANSFERINDEX;
private static final long BASECOUNT;
private static final long CELLSBUSY;
private static final long CELLVALUE;
private static final long ABASE;
private static final int ASHIFT;

static {
  try {
    U = sun.misc.Unsafe.getUnsafe();
    Class<?> k = ConcurrentHashMap.class;
    SIZECTL = U.objectFieldOffset
      (k.getDeclaredField("sizeCtl"));
    TRANSFERINDEX = U.objectFieldOffset
      (k.getDeclaredField("transferIndex"));
    BASECOUNT = U.objectFieldOffset
      (k.getDeclaredField("baseCount"));
    CELLSBUSY = U.objectFieldOffset
      (k.getDeclaredField("cellsBusy"));
    Class<?> ck = CounterCell.class;
    CELLVALUE = U.objectFieldOffset
      (ck.getDeclaredField("value"));
    Class<?> ak = Node[].class;
    ABASE = U.arrayBaseOffset(ak);
    int scale = U.arrayIndexScale(ak);
    if ((scale & (scale - 1)) != 0)
      throw new Error("data type scale not a power of two");
    ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
  } catch (Exception e) {
    throw new Error(e);
  }
}
```

### 2.内部类

- Node类

  Node类主要用于存储具体键值对，其子类有ForwardingNode、ReservationNode、TreeNode和TreeBin四个子类。所有插入ConcurrentHashMap的数据都包装在这里面。它与HashMap中的定义很相似，但是但是有一些差别它对value和next属性设置了volatile同步锁，它不允许调用setValue方法直接改变Node的value域，它增加了find方法辅助map.get()方法。

- Traverser类

  Traverser类主要用于遍历操作，其子类有BaseIterator、KeySpliterator、ValueSpliterator、EntrySpliterator四个类，BaseIterator用于遍历操作。KeySplitertor、ValueSpliterator、EntrySpliterator则用于键、值、键值对的划分。

- CollectionView类

  CollectionView抽象类主要定义了视图操作，其子类KeySetView、ValueSetView、EntrySetView分别表示键视图、值视图、键值对视图。对视图均可以进行操作。



### 3.unsafe与cas

在ConcurrentHashMap中，随处可以看到U, 大量使用了U.compareAndSwapXXX的方法，这个方法是利用一个CAS算法实现无锁化的修改值的操作，他可以大大降低锁代理的性能消耗。这个算法的基本思想就是不断地去比较当前内存中的变量值与你指定的一个变量值是否相等，如果相等，则接受你指定的修改的值，否则拒绝你的操作。因为当前线程中的值已经不是最新的值，你的修改很可能会覆盖掉其他线程修改的结果。这一点与乐观锁，SVN的思想是比较类似的。

### 4.构造函数

```java
// 创建一个带有默认初始容量 (16)、加载因子 (0.75) 和 concurrencyLevel (16) 的新的空映射
public ConcurrentHashMap() {
}

// 创建一个带有指定初始容量、默认加载因子 (0.75) 和 concurrencyLevel (16) 的新的空映射
public ConcurrentHashMap(int initialCapacity) {
  if (initialCapacity < 0) // 初始容量小于0，抛出异常
    throw new IllegalArgumentException();
  int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
             MAXIMUM_CAPACITY :
             tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1)); // 找到最接近该容量的2的幂次方数
  // 初始化
  this.sizeCtl = cap;
}

// 该构造函数用于构造一个与给定映射具有相同映射关系的新映射。
public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
  this.sizeCtl = DEFAULT_CAPACITY;
  // 将集合m的元素全部放入
  putAll(m);
}

// 创建一个带有指定初始容量、加载因子和默认 concurrencyLevel (1) 的新的空映射。
public ConcurrentHashMap(int initialCapacity, float loadFactor) {
  this(initialCapacity, loadFactor, 1);
}

// 创建一个带有指定初始容量、加载因子和并发级别的新的空映射。
public ConcurrentHashMap(int initialCapacity,
                         float loadFactor, int concurrencyLevel) {
  if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0) // 合法性判断
    throw new IllegalArgumentException();
  if (initialCapacity < concurrencyLevel)   // Use at least as many bins
    initialCapacity = concurrencyLevel;   // as estimated threads
  long size = (long)(1.0 + (long)initialCapacity / loadFactor);
  int cap = (size >= (long)MAXIMUM_CAPACITY) ?
    MAXIMUM_CAPACITY : tableSizeFor((int)size);
  this.sizeCtl = cap;
}
```

对于构造函数而言，会根据输入的initialCapacity的大小来确定一个最小的且大于等于initialCapacity大小的2的n次幂，如initialCapacity为15，则sizeCtl为16。若initialCapacity大小超过了允许的最大值，则sizeCtl为最大值。值得注意的是，构造函数中的concurrencyLevel参数是用来确定sizeCtl大小，在JDK1.8中的并发控制都是针对具体的桶而言，即有多少个桶就可以允许多少个并发数。




### 5.初始化ConcurrentHashMap


ConcurrentHashMap的初始化主要由**initTable()**方法实现，在上面的构造函数中我们可以看到，其实ConcurrentHashMap在构造函数中并没有做什么事，仅仅只是设置了一些参数而已。其真正的初始化是发生在插入的时候，例如put、merge、compute、computeIfAbsent、computeIfPresent操作时。其方法定义如下：

```java
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        //sizeCtl < 0 表示有其他线程在初始化，该线程必须挂起
        if ((sc = sizeCtl) < 0)
            Thread.yield(); 
        // 如果该线程获取了初始化的权利，则用CAS将sizeCtl设置为-1，表示本线程正在初始化
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                // 开始初始化
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    // 下次扩容的大小
                    //相当于0.75*n 设置一个扩容的阈值  
                    sc = n - (n >>> 2);
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```

初始化方法initTable()的关键就在于sizeCtl，该值默认为0，如果在构造函数时有参数传入该值则为2的幂次方。该值如果 < 0，表示有其他线程正在初始化，则必须暂停该线程。如果线程获得了初始化的权限则先将sizeCtl设置为-1，防止有其他线程进入，最后将sizeCtl设置0.75 * n，表示扩容的阈值。

### 6.putVal函数分析

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    // 键或值为空，抛出异常
    if (key == null || value == null) throw new NullPointerException();
    // 键的hash值经过计算获得hash值
    int hash = spread(key.hashCode());
    int binCount = 0;
    // 无限循环
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        // 表为空或者表的长度为0
        if (tab == null || (n = tab.length) == 0)
            // 初始化表
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            // 表不为空并且表的长度大于0，并且该桶不为空
            // 比较并且交换值，如tab的第i项为空则用新生成的node替换
            if (casTabAt(tab, i, null,
                         new Node<K,V>(hash, key, value, null)))
                break;                   
        }
        else if ((fh = f.hash) == MOVED)
             // 该结点的hash值为MOVED
             // 进行结点的转移（在扩容的过程中）
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            // 加锁同步
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                     // 找到table表下标为i的节点
                    if (fh >= 0) {
                        // 该table表中该结点的hash值大于0
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            // 结点的hash值相等并且key也相等
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                 // 保存该结点的val值
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    // 将指定的value保存至结点，即进行了结点值的更新
                                    e.val = value;
                                break;
                            }
                             // 保存当前结点
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                // 当前结点的下一个结点为空，即为最后一个结点
                                 // 新生一个结点并且赋值给next域
                                pred.next = new Node<K,V>(hash, key,
                                                          value, null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        // 结点为红黑树结点类型
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                              value)) != null) {
                            // 将hash、key、value放入红黑树
                             // 保存结点的val
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    // 如果binCount大于等于转化为红黑树的阈值
                     // 进行转化
                    treeifyBin(tab, i);
                if (oldVal != null)
                    // 旧值不为空 返回旧值
                    return oldVal;
                break;
            }
        }
    }
     // 增加binCount的数量
    addCount(1L, binCount);
    return null;
}
```

说明：put函数底层调用了putVal进行数据的插入，对于putVal函数的流程大体如下。

1.  判断存储的key、value是否为空，若为空，则抛出异常，否则，进入步骤2
2. 计算key的hash值，随后进入无限循环，该无限循环可以确保成功插入数据，若table表为空或者长度为0，则初始化table表，否则，进入步骤3
3.  根据key的hash值取出table表中的结点元素，若取出的结点为空（该桶为空），则使用CAS将key、value、hash值生成的结点放入桶中。否则，进入步骤4
4. 若该结点的的hash值为MOVED，则对该桶中的结点进行转移，否则，进入步骤5
5. 对桶中的第一个结点（即table表中的结点）进行加锁，对该桶进行遍历，桶中的结点的hash值与key值与给定的hash值和key值相等，则根据标识选择是否进行更新操作（用给定的value值替换该结点的value值），若遍历完桶仍没有找到hash值与key值和指定的hash值与key值相等的结点，则直接新生一个结点并赋值为之前最后一个结点的下一个结点。进入步骤6
6. 若binCount值达到红黑树转化的阈值，则将桶中的结构转化为红黑树存储，最后，增加binCount的值。

### 7.get函数分析

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    // 计算key的hash值
    int h = spread(key.hashCode()); 
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) { // 表不为空并且表的长度大于0并且key所在的桶不为空
        if ((eh = e.hash) == h) { // 表中的元素的hash值与key的hash值相等
            if ((ek = e.key) == key || (ek != null && key.equals(ek))) // 键相等
                // 返回值
                return e.val;
        }
        else if (eh < 0) // 结点hash值小于0
            // 在桶（链表/红黑树）中查找
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) { // 对于结点hash值大于0的情况
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;

```

说明：get操作步骤：

1. 计算hash值
2. 判断table是否为空，如果为空，直接返回null 
3. 根据hash值获取table中的Node节点（tabAt(tab, (n - 1) & h)），然后根据链表或者树形方式找到相对应的节点，返回其value值

----

### 8.replaceNode函数分析

```java
final V replaceNode(Object key, V value, Object cv) {
        // 计算key的hash值
        int hash = spread(key.hashCode());
        for (Node<K,V>[] tab = table;;) { // 无限循环
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0 ||
                (f = tabAt(tab, i = (n - 1) & hash)) == null) // table表为空或者表长度为0或者key所对应的桶为空
                // 跳出循环
                break;
            else if ((fh = f.hash) == MOVED) // 桶中第一个结点的hash值为MOVED
                // 转移
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) { // 加锁同步
                    if (tabAt(tab, i) == f) { // 桶中的第一个结点没有发生变化
                        if (fh >= 0) { // 结点hash值大于0
                            validated = true;
                            for (Node<K,V> e = f, pred = null;;) { // 无限循环
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) { // 结点的hash值与指定的hash值相等，并且key也相等
                                    V ev = e.val;
                                    if (cv == null || cv == ev ||
                                        (ev != null && cv.equals(ev))) { // cv为空或者与结点value相等或者不为空并且相等
                                        // 保存该结点的val值
                                        oldVal = ev;
                                        if (value != null) // value为null
                                            // 设置结点value值
                                            e.val = value;
                                        else if (pred != null) // 前驱不为空
                                            // 前驱的后继为e的后继，即删除了e结点
                                            pred.next = e.next;
                                        else
                                            // 设置table表中下标为index的值为e.next
                                            setTabAt(tab, i, e.next);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        }
                        else if (f instanceof TreeBin) { // 为红黑树结点类型
                            validated = true;
                            // 类型转化
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                (p = r.findTreeNode(hash, key, null)) != null) { // 根节点不为空并且存在与指定hash和key相等的结点
                                // 保存p结点的value
                                V pv = p.val;
                                if (cv == null || cv == pv ||
                                    (pv != null && cv.equals(pv))) { // cv为空或者与结点value相等或者不为空并且相等
                                    oldVal = pv;
                                    if (value != null) 
                                        p.val = value;
                                    else if (t.removeTreeNode(p)) // 移除p结点
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (validated) {
                    if (oldVal != null) {
                        if (value == null)
                            // baseCount值减一
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;
    }
```

此函数对remove函数提供支持，remove函数底层是调用的replaceNode函数实现结点的删除



> 参考：
>
> https://www.cnblogs.com/leesf456/p/5453341.html
>
> http://www.cnblogs.com/huaizuo/archive/2016/04/20/5413069.html
>
> <https://blog.csdn.net/u010723709/article/details/48007881>

