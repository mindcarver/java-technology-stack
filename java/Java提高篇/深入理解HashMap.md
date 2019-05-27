# HashMap工作原理

[TOC]

-----

## 一.HashMap概述

HashMap基于哈希表的实现的`Map`接口。而哈希表是一种以 键-值(key-value) 存储数据的结构，只需要根据key即可找到对应的值。

哈希的思路很简单，如果所有的键都是整数，那么就可以使用一个简单的无序数组来实现：将键作为索引，值即为其对应的值，这样就可以快速访问任意键的值。这是对于简单的键的情况，我们可以将其扩展到更为复杂的键上。

使用哈希函数将被查找的键转换为数组的索引。**在理想的情况下**，不同的键会被转换为不同的索引值，但是在有些情况下我们需要处理多个键被哈希到同一个索引值的情况。

  ![1550043052799](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\1550043052799.png)

从上图可以看出，当多个键被哈希到同一个索引值的时候，HashMap会在索引处生成单向链表，而当链表长度大于 8 的时候又会转成红黑树（JDK1.8新增部分）。

---

## 二.HashMap原理解析

### 1.基本元素以及构造函数

```java
=====基本元素=====
// 默认初始容量为16，就是我们上面看到图的数组table的容量，必须为2的n次幂
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; 

// 最大容量为2的30次方
static final int MAXIMUM_CAPACITY = 1 << 30;

// 默认填充因子为0.75
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// 当桶(bucket)上的结点数大于8，转成红黑树
static final int TREEIFY_THRESHOLD = 8;

// 当桶(bucket)上的结点数小于6，红黑树转链表
static final int UNTREEIFY_THRESHOLD = 6;

// 在转变成树之前，还会有一次判断，只有键值对数量大于 64 才会发生转换。
// 这是为了避免在哈希表建立初期，多个键值对恰好被放入了同一个链表中而导致不必要的转化。
static final int MIN_TREEIFY_CAPACITY = 64;

// 存储元素的数组，总是2的幂
transient Node<K,V>[] table;

// 存放具体元素的set集
transient Set<Map.Entry<K,V>> entrySet;

// 存放元素(key-value)的个数，而不是数组的长度。
transient int size;

// 每次扩容和更改map结构的计数器
transient int modCount;

// 临界值 当实际结点个数超过临界值(容量*填充因子，JDK1.7，1.8的算法不是这样)时，会进行扩容
int threshold;

// 填充因子
final float loadFactor;


=====构造函数=====
public HashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                                           initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                                           loadFactor);
    this.loadFactor = loadFactor;
    // 计算 扩容的临界值
    this.threshold = tableSizeFor(initialCapacity);
}    

public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}

public HashMap() {
    this.loadFactor = DEFAULT_LOAD_FACTOR; 
}

public HashMap(Map<? extends K, ? extends V> m) {   
    this.loadFactor = DEFAULT_LOAD_FACTOR;
    putMapEntries(m, false);
}

```
-----

> <font color="red">DEFAULT_INITIAL_CAPACITY & MAXIMUM_CAPACITY</font>

如果我们创建HashMap的时候不指定默认容量，那么系统会指定默认为16，但是根据阿里云规范来说，必须要指定且为2<sup>n</sup>,并且小于2<sup>30</sup>.



> <font color="red">DEFAULT_LOAD_FACTOR & loadFactor</font>

LOAD_FACTOR(负载因子)和上面的CAPACITY(容量)的关系，简单来说,**Capacity就是数组的长度/大小，loadFactor是这个数组填满程度的最大比比例。**同样的，我们可以根据有参的HashMap构造函数来指定初始负载容量的大小，如果不指定，默认的负载因子为0.75。



> <font color="red">size & threshold</font>

size表示当前HashMap中已经储存的Node<key,value>的数量，包括**数组和链表**中的的Node<key,value>。threshold表示扩容的临界值，如果size大于这个值，则必需调用resize()方法进行扩容，扩容之后的容量是之前的两倍。在jdk1.7及以前，**threshold = length \* Load factor**，其中length为数组的长度，也就是说数组的长度成负载因子的数量，也就是说，在数组定义好长度之后，负载因子越大，所能容纳的键值对个数越多。默认的负载因子0.75是对空间和时间效率的一个平衡选择，建议大家不要修改，除非在时间和空间比较特殊的情况下，如果内存空间很多而又对时间效率要求很高，可以降低负载因子Load factor的值；相反，如果内存空间紧张而对时间效率要求不高，可以增加负载因子loadFactor的值，这个值可以大于1。而到了jdk1.8之后，threshold 算法改变后文会有提及。



> <font color="red">modCount</font>

modCount字段主要用来记录HashMap内部结构发生变化的次数，主要用于迭代的快速失败。内部结构发生变化指的是结构发生变化，例如put新键值对，但是某个key对应的value值被覆盖不属于结构变化。



> <font color="red">Node<K,V>[] table</font>

  ```java
  static class Node<K,V> implements Map.Entry<K,V> {
          final int hash;     //每个储存元素key的哈希值
          final K key;        //key
          V value;            //value
          Node<K,V> next;     //链表下一个node
          Node(int hash, K key, V value, Node<K,V> next) {
              this.hash = hash;
              ......
          }
          public final K getKey()        { return key; }
          public final V getValue()      { return value; }
          public final String toString() { return key + "=" + value; }
          public final int hashCode() { ...... }
          public final V setValue(V newValue) { ...... }
          public final boolean equals(Object o) { ....... }
      }
  ```

  Node<K,V>[]是HashMap的一个内部类，既是**HashMap底层数组**的组成元素，又是**每个单向链表**的组成元素。它其中包含了数组元素所需要的**key与value**，以及链表所需要的指向下一个节点的**引用域next**。那么它是如何计算出hash值的呢？



---------

### 2.如何确定哈希桶数组索引的位置

我们在对HashMap进行读、增加、删除的时候首先第一步就是定位到哈希桶数组的位置，而HashMap数据结构是数组+链表+红黑树，而对于HashMap的性能优化来说，自然是希望HashMap里面的元素能够均匀的分布，尽量确保数组的每一个位置都只有一个元素，而不是数组某一个索引位置有多个元素（形成链表），如果链表过长，我们就需要遍历链表，大大降低查询效率。因此好的哈希算法直接决定了HashMap的离散性能，我们来看看JDK1.8的实现。

```java
static final int hash(Object key) {
    // h = key.hashCode() 为第一步 取hashCode值
    // h ^ (h >>> 16)  为第二步 高位参与运算
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
Node<K,V>[] tab;
// 第三步 取模运算
tab[(n - 1) & hash])
```

hash算法本质就三步：第一步 取hashCode值、第二步 高位参与运算、第三步 取模运算

这里可以看到，首先将得到key对应的哈希值：`h = key.hashCode()`，然后通过**hashCode()的高16位异或低16位实现的**：`(h = k.hashCode()) ^ (h >>> 16)`，最后是通过`(n-1) & hash`来进行取模运算得到数组索引位置，接下来分步讲解这个index的产生:

1. **取key的hashcode值：**

   返回对象的经过**处理后的内存地址**，由于每个对象的内存地址都不一样，所以哈希码也不一样。这个是**native方法**，取决于JVM的内部设计，一般是某种C地址的偏移。

2. **hashCode()的高16位异或低16位**

   在JDK1.8的实现中，优化了高位运算的算法，通过hashCode()的高16位异或低16位实现的：(h = k.hashCode()) ^ (h >>> 16)，主要是从速度、功效、质量来考虑的，这么做可以在数组table的length比较小的时候，也能保证考虑到高低Bit都参与到Hash的计算中，同时不会有太大的开销。

3. **(n-1) & hash; 取模运算**

   这个n是table的长度，那么n-1就是table数组元素的下标。它通过hash & (table.length -1)来得到**该对象的保存位**，而**HashMap底层数组的长度总是2的n次方**，这是HashMap在速度上的优化。**当length总是2的n次方时，hash&(length-1) 运算等价于对length取模，也就是hash % length，但是&比%具有更高的效率。**

下面举例说明下，n为table的长度。

![1550110867990](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\1550110867990.png)

计算之后得到的5就是数组索引位置。

那么为什么要先高16位异或低16位再取模运算，我们这里先看第三步：

我们知道，n代表的是table的长度length，之前一再强调，表table的长度需要取2的整数次幂，就是为了这里等价这里进行取模运算时的方便——取模运算转化成位运算公式:**a%(2^n) 等价于 a&(2^n-1)**,而&操作比%操作具有更高的效率。
   当length=2n时，(length - 1)正好相当于一个**"低位掩码"**,"与"操作的结果就是散列值的高位全部归零，只保留低位，用来做数组下标访问:

![1550111626841](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\1550111626841.png)

可以看到，当我们的length为16的时候，哈希码(字符串“abcabcabcabcabc”的key对应的哈希码)对(16-1)与操作，对于多个key生成的hashCode，只要哈希码的后4位为0，不论不论高位怎么变化，最终的结果均为0。也就是说，如果支取后四位(低位)的话，这个时候产生"碰撞"的几率就非常大(当然&运算中产生碰撞的原因很多，这里只是举个例子)。为了解决低位与操作碰撞的问题，于是便有了第二步中高16位异或低16位的“扰动函数”。
右移16位，自己的高半区和低半区异或，就是为了混合原始哈希码的高位和低位，以此来加大低位随机性。

![1550111686272](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\1550111686272.png)

可以看到:
扰动函数优化前：1954974080 % 16 = 1954974080 & (16 - 1) = 0
扰动函数优化后：1955003654 % 16 = 1955003654 & (16 - 1) = 6
很显然，减少了碰撞的几率。

---

### 3.put方法详解

```java
public V put(K key, V value) {
    // hash(key)对key做hash，其实这里就是上面的第一、二步骤
    return putVal(hash(key), key, value, false, true);
}
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    // (n - 1) & hash(第三步骤，取模运算，获得数组的index位置)
    // tab为数组，p是每个桶(链表的每个结点)
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    // 1.对table判空，如果为空就调用resize()方法创建
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    // 2.计算数组索引，如果为null，直接新建node即可
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        // 表示计算之后的索引存在
        // 如果节点key存在的话，直接就覆盖值
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        // 该链是红黑树
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        else {
            // 该链为链表
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    //链表长度大于8转换为红黑树进行处理
                    if (binCount >= TREEIFY_THRESHOLD - 1) 
                        treeifyBin(tab, hash);
                    break;
                }
                // key已经存在直接覆盖value
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) {
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;
    // 超过最大容量 就扩容
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);
    return null;
}
```

接下来分步解析put方法：

1. 判断键值对数组table[i]是否为空或为null，否则执行resize()进行扩容；也就是说table真正初始化的时机并不是构造函数开始的时候，而是在**第一次向HashMap添加元素的时候，会调用resize()创建并初始化数组。**

2. 根据键值key计算hash值得到插入的数组索引i，如果table[i]==null，直接新建节点添加，转向6，如果table[i]不为空，转向3；

3. 判断table[i]的首个元素是否和key一样，如果相同直接覆盖value，否则转向4，这里的相同指的是hashCode以及equals；

4. 判断table[i] 是否为treeNode，即table[i] 是否是红黑树，如果是红黑树，则直接在树中插入键值对，否则转向5；

5. 遍历table[i]，判断链表长度是否大于8，大于8的话把链表转换为红黑树，在红黑树中执行插入操作，否则进行链表的插入操作；遍历过程中若发现key已经存在直接覆盖value即可；

   ```java
   else if (p instanceof TreeNode)
           e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value); //是红黑树
       else {
           for (int binCount = 0; ; ++binCount) {      //不是红黑树而是链表
               if ((e = p.next) == null) {
                   p.next = newNode(hash, key, value, null);   //
                   if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                       treeifyBin(tab, hash);
                   break;
               }
               if (e.hash == hash &&
                   ((k = e.key) == key || (key != null && key.equals(k))))
                   break;
               p = e;
           }
       }
   ```

   `for (int binCount = 0; ; ++binCount) {`表示循环遍历链表，这个for循环当中实际上经历了以下几个步骤：

   - **e = p.next**以及for语句之外后面的`p = e;`实际上是在向后循环遍历链表。
     开始的时候P为每个桶的头元素，然后将P的引用域(本来指向的是下一个元素)指向空节点e，这个时候实际上就相当于将p的下一个元素赋值给了e,即e已经变成了p的下一个元素。
   - 此时我们把这个复制的e单独提出来，进行了两个判断：
      第一个if：`if ((e = p.next) == null)`
        如果e也就是p.next == null,那么说明当前的这个P已经是链表最后一个元素了。这个时候采取**尾插法**添加一个新的元素:`p.next = newNode(hash, key, value, null);`,即直接将p的引用域指向这个新添加的元素。如果添加新元素之后发现链表的长度超过了`TREEIFY_THRESHOLD - 1`也就是超过了8，那么调用`treeifyBin(tab, hash);`把这个链表转换成红黑树接着玩。
      第二个if:`if (e.hash == hash &&((k = e.key) == key || (key != null && key.equals(k))))`
        如果发现key值重复了，也就是要插入的key已经存在，那么直接break，结束遍历.
   - 然后又将e赋给p，这个时候的p已经向后移动了一位。重复上面的过程，直到循环完整个链表，或者break出来。

6. 插入成功后，判断实际存在的键值对数量size是否超多了最大容量threshold，如果超过，进行扩容。

最后以一张图来总结一下：

![1550120515141](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\1550120515141.png)

### 4.HashMap的扩容机制

扩容(resize)就是重新计算容量，向HashMap对象里不停的添加元素，而HashMap对象内部的数组无法装载更多的元素时，对象就需要扩大数组的长度，以便能装入更多的元素。当然Java里的数组是无法自动扩容的，方法是使用一个新的数组代替已有的容量小的数组，就像我们用一个小桶装水，如果想装更多的水，就得换大水桶。

```java
if (++size > threshold)
    resize();
---------------------------
    final Node<K,V>[] resize() {
    //创建一个oldTab数组用于保存之前的数组
    Node<K,V>[] oldTab = table;
    //获取原来数组的长度
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    //原来数组扩容的临界值
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        //如果原来的数组长度大于最大值(2^30)
        if (oldCap >= MAXIMUM_CAPACITY) {
            //扩容临界值提高到正无穷（意味着不再需要扩容）
            threshold = Integer.MAX_VALUE;
            //返回原来的数组
            return oldTab;
        }
        // 如果新数组长度（老数组长度*2）小于最大值（2^30）并且
        // 原来的数组大于等于初始长度(2^4)，那么新数组的长度可以为老数组两倍
        // 也就是说扩容是以两倍扩容的
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                 oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1; 
    }
    else if (oldThr > 0) 
        //新数组的初始容量设置为老数组扩容的临界值
        newCap = oldThr;
    else {          
        // 否则 oldThr == 0,零初始阈值表示使用默认值
        //新数组初始容量设置为默认值
        newCap = DEFAULT_INITIAL_CAPACITY;
         //计算默认容量下的阈值
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    if (newThr == 0) {
        //如果newThr == 0，说明为上面 else if (oldThr > 0)
        //的情况(其他两种情况都对newThr的值做了改变),此时newCap = oldThr;
        //ft为临时变量，用于判断阈值的合法性
        float ft = (float)newCap * loadFactor;
        //计算新的阈值
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                  (int)ft : Integer.MAX_VALUE);
    }
    //改变threshold值为新的阈值
    threshold = newThr;
    @SuppressWarnings({"rawtypes","unchecked"})
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    //改变table全局变量为，扩容后的newTable
    table = newTab;
    if (oldTab != null) {
        //遍历数组，将老数组迁移到新的数组中
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            //新建一个Node<K,V>类对象，用它来遍历整个数组
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null)
                    //将e也就是oldTab[j]放入newTab中e.hash & (newCap - 1)的位置，
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else { 
                    // 链表重排
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}    
```

接下来分析链表重排部分：

可以知道，如果(e.hash & oldCap) == 0，则  newTab[j] = loHead = e = oldTab[j]，即索引位置没变。反之 (e.hash & oldCap) != 0, newTab[j + oldCap] = hiHead = e = oldTab[j],也就是说，此时把**原数组[j]位置上的桶移到了新数组[j+原数组长度]**的位置上了。

JDK8使用的是2次幂的扩展(指长度扩为原来2倍)，所以，元素的位置要么是在原位置，要么是在原位置再移动2次幂的位置。看下图可以明白这句话的意思，n为table的长度，图（a）表示扩容前的key1和key2两种key确定索引位置的示例，图（b）表示扩容后key1和key2两种key确定索引位置的示例，其中hash1是key1对应的哈希与高位运算结果。

![1550121792381](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\1550121792381.png)

元素在重新计算hash之后，因为n变为2倍，那么n-1的mask范围在高位多1bit(红色)，因此新的index就会发生这样的变化：

![1550123637184](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\1550123637184.png)

因此，我们在扩充HashMap的时候，不需要像JDK1.7的实现那样重新计算hash，只需要看看原来的hash值新增的那个bit是1还是0就好了，是0的话索引没变，是1的话索引变成“原索引+oldCap”，可以看看下图为16扩充为32的resize示意图:

![1550123590019](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\1550123590019.png)

这个设计确实非常的巧妙，既省去了重新计算hash值的时间，而且同时，由于新增的1bit是0还是1可以认为是随机的，因此resize的过程，均匀的把之前的冲突的节点分散到新的bucket了。

----



### 5.为什么要指定初始容量以及指定多大

> [推荐]集合初始化时，指定集合初始化值大小
>
> 说明：HashMap使用HashMao(int initialCapacity)初始化

先做个例子测试：

```java
public class HashMapTest {
    public static void main(String[] args) {
        int num = 10000000;


        Map<Integer, Integer> map = new HashMap<>();

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
            map.put(i, i);
        }
        long t2 = System.currentTimeMillis();

        System.out.println("未初始化容量，耗时 ： " + (t2 - t1));


       /*****************************************************/

        Map<Integer, Integer> map_1 = new HashMap<>(num / 2);

        long t3 = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
            map_1.put(i, i);
        }
        long t4 = System.currentTimeMillis();

        System.out.println("初始化容量num/2，耗时 ： " + (t4 - t3));


        /*****************************************************/
        Map<Integer, Integer> map_2 = new HashMap<>(num);

        long t5 = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
            map_2.put(i, i);
        }
        long t6 = System.currentTimeMillis();

        System.out.println("初始化容量为num，耗时 ： " + (t6 - t5));
    }
}

```

> 未初始化容量，耗时 ： 14706
> 初始化容量num/2，耗时 ： 8939
> 初始化容量为num，耗时 ： 3685

从执行结果可以看出，当进行了容量的初始化时，耗时会比未初始化的耗时少，并且初始化容量的值的设定也会影响耗时。

上面HashMap的扩容机制中讲到过，当HashMap中的元素个数（size）超过临界值（threshold）时就会自动扩容，扩容为原数组长度的两倍，临界值也会扩展到原数组临界值的两倍。因此当我们没有设置初始值的时候，或者设置的初始值不是合理的，那么可能会发生多次扩容，导致性能下降，那么当已知需要存储的元素个数（size）的时候，如何设置容量呢（initialCapcity）?

我们先随便设定一个值，比如是6

```java
Map<String,String> map = new HashMap(6);
// 跟入源码查看
public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}
public HashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                                           initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                                           loadFactor);
    this.loadFactor = loadFactor;
    // 计算临界值的核心方法
    this.threshold = tableSizeFor(initialCapacity);
}
// 最终会返回给定容量2的幂
// 经过两步操作，无符号位移和或操作
static final int tableSizeFor(int cap) {
    int n = cap - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

我们根据用户传过来的6( 容量值)，通过tableSizeFor(initialCapacity)来计算得出临界值（不是数组长度，而是数组能存的最大size），下面来分析一下tableSizeFor是如何计算得出最终数组能存的最大size(<k,v>键值对的个数)。

```java
首先 n = 6-1 =5 二进制表示
0000 0000 0000 0101 n=5
0000 0000 0000 0100 >>>1 (无符号右移1位)
——————————————————————— "或"操作 得到
0000 0000 0000 0111 n=7
0000 0000 0000 0001 >>>2 (无符号右移2位)    
———————————————————————— "或"操作 得到
0000 0000 0000 0111 n=7
0000 0000 0000 0000 >>>4 (无符号右移4位)
———————————————————————— "或"操作 得到
0000 0000 0000 0111 n=7
.........接下来的>>>8 和>>> 16都是一样，最终结果为7
```

最后根据(n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1计算得出8，也就是2的3次幂，作为容量为6的数组临界值8。多测几个数据发现：6 -> 8, 9 -> 16,40 -> 64等等。

那么如何设置HashMap的容量值呢？

> 阿里巴巴规范：
>
> initialCapacity = **（需要存储的元素个数 / 负载因子）+ 1**，注意负载因子即为loaderfactor，默认为0.75，如果暂时无法确定初始值大小，则设置为16（即默认值）

举个例子，比如你要存6个元素，那么根据公式计算 6 / 0.75 + 1 = 9，这是你传入的容量，而JDK会根据上面的tableSizeFor（）方法，将临界值（threshold）设置为16，也就是最多能存16个键值对，超过就会扩容，**一定要注意传入的初始化容量并不是指能存的键值对个数.** 

所以如果我们已知这个Map中即将存放的元素个数，给HashMap设置初始容量可以在一定程度上提升效率。当然计算的过程需要根据上面的公式去计算，但是



-----

## 三.常见HashMap面试问题

1. **如果HashMap的大小（size）超过了负载因子(load factor)定义的容量（指临界值=数组length*loadfactor），怎么办？**

   默认的负载因子大小为0.75，也就是说，当一个map填满了75%的bucket时候，和其它集合类(如ArrayList等)一样，将会创建原来HashMap大小的两倍的bucket数组，来重新调整map的大小，并将原来的对象放入新的bucket数组中。这个过程叫作rehashing，因为它调用hash方法找到新的bucket位置。

2. **重新调整HashMap大小存在什么问题吗?**

   如果两个线程都发现HashMap需要重新调整大小了，它们会同时试着调整大小。在调整大小的过程中，存储在链表中的元素的次序会反过来，因为移动到新的bucket位置的时候，HashMap并不会将元素放在链表的尾部，而是放在头部，这是为了避免尾部遍历(tail traversing)。如果条件竞争发生了，那么就死循环了。

3. **为什么String, Interger这样的包装类适合作为键？**

   string, Interger这样的包类作为HashMap的键是再适合不过了，而且String最为常用。因为String是不可变的，也是final的，而且已经重写了equals()和hashCode()方法了。其他的包装类也有这个特点。不可变性是必要的，因为为了要计算hashCode()，就要防止键值改变，如果键值在放入时和获取时返回不同的hashcode的话，那么就不能从HashMap中找到你想要的对象。不可变性还有其他的优点如线程安全。如果你可以仅仅通过将某个field声明成final就能保证hashCode是不变的，那么请这么做吧。因为获取对象的时候要用到equals()和hashCode()方法，那么键对象正确的重写这两个方法是非常重要的。如果两个不相等的对象返回不同的hashcode的话，那么碰撞的几率就会小些，这样就能提高HashMap的性能。



-----

## 四.总结

1. Java8中hash计算是通过key的hashCode()的高16位异或低16位实现的，既保证高低bit都能参与到hash的计算中，又不会有太大的开销。
2. 数组大小n总是2的整数次幂，计算下标时直接( hash & n-1)
3. Java8引入红黑树，当链表长度达到8， 执行treeifyBin，当桶数量达到64时，将链表转为红黑树，否则，执行resize()。
4. 判断Node是否符合，首先判断哈希值要相等，但因为哈希值不是唯一的，所以还要对比key是否相等，最好是同一个对象，能用＝＝对比，否则要用equals()
5. 扩容是一个特别耗性能的操作，所以当程序员在使用HashMap的时候，估算map的大小，初始化的时候给一个大致的数值，避免map进行频繁的扩容。
6. 负载因子是可以修改的，也可以大于1，但是建议不要轻易修改，除非情况非常特殊。
7. HashMap是线程不安全的，不要在并发的环境中同时操作HashMap，建议使用ConcurrentHashMap。



-----

参考：https://zhuanlan.zhihu.com/p/21673805

​           https://www.jianshu.com/p/17177c12f849

​           http://www.importnew.com/7099.html

​           https://blog.csdn.net/v_july_v/article/details/6105630







