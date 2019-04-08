# ThreadLocal

## 1.概念

ThreadLocal即线程变量,是一个以ThreadLocal对象为键、任意对象为值的存储结构。这个结构被附带在线程上，也就是说一个线程可以根据一个ThreadLocal对象查询到绑定在这个线程上的一个值。

ThreadLocal提供了get和set等访问接口或方法，**这些方法为每个使用该变量的线程都存有一份独立的副本**，因此get总是返回由当前执行线程在调用set时设置的最新值。

ThreadLocal对象通常**用于防止对可变的单实例变量或全局变量进行共享。**

## 2.TheadLocal常用API

1. get: 获取ThreadLocal中当前线程共享变量的值。
2. set: 设置ThreadLocal中当前线程共享变量的值。
3. remove: 移除ThreadLocal中当前线程共享变量的值。
4. initialValue: ThreadLocal没有被当前线程赋值时或当前线程刚调用remove方法后调用get方法，返回此方法值。



##3.案例演示

```java
public class ThreadLocalDemo {
    private static final ThreadLocal<Object> threadLocal = new ThreadLocal<Object>(){
        /**
         * ThreadLocal没有被当前线程赋值时或当前线程刚调用remove方法后调用get方法，返回此方法值
         */
        @Override
        protected Object initialValue()
        {
            System.out.println("调用get方法时，当前线程共享变量没有设置，调用initialValue获取默认值！");
            return null;
        }
    };

    public static void main(String[] args)
    {
        new Thread(new IntegerRunner("IntegerRunner1")).start();
        new Thread(new StringRunner("StringRunner1")).start();
        new Thread(new IntegerRunner("IntegerRunner2")).start();
        new Thread(new StringRunner("StringRunner2")).start();
    }

    public static class IntegerRunner implements Runnable
    {
        private String name;

        IntegerRunner(String name)
        {
            this.name = name;
        }

        @Override
        public void run()
        {
            for(int i = 0; i < 5; i++)
            {
                // ThreadLocal.get方法获取线程变量
                if(null == ThreadLocalDemo.threadLocal.get())
                {
                    // ThreadLocal.et方法设置线程变量
                    ThreadLocalDemo.threadLocal.set(0);
                    System.out.println("线程" + name + ": 0");
                }
                else
                {
                    int num = (Integer)ThreadLocalDemo.threadLocal.get();
                    ThreadLocalDemo.threadLocal.set(num + 1);
                    System.out.println("线程" + name + ": " + ThreadLocalDemo.threadLocal.get());
                    if(i == 3)
                    {
                        ThreadLocalDemo.threadLocal.remove();
                    }
                }
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }

    }

    public static class StringRunner implements Runnable {
        private String name;

        StringRunner(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                if (null == ThreadLocalDemo.threadLocal.get()) {
                    ThreadLocalDemo.threadLocal.set("a");
                    System.out.println("线程" + name + ": a");
                } else {
                    String str = (String) ThreadLocalDemo.threadLocal.get();
                    ThreadLocalDemo.threadLocal.set(str + "a");
                    System.out.println("线程" + name + ": " + ThreadLocalDemo.threadLocal.get());
                }
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

```

运行结果如下：

>调用get方法时，当前线程共享变量没有设置，调用initialValue获取默认值！
>线程IntegerRunner1: 0
>调用get方法时，当前线程共享变量没有设置，调用initialValue获取默认值！
>线程StringRunner1: a
>调用get方法时，当前线程共享变量没有设置，调用initialValue获取默认值！
>线程IntegerRunner2: 0
>调用get方法时，当前线程共享变量没有设置，调用initialValue获取默认值！
>线程StringRunner2: a
>线程StringRunner1: aa
>线程StringRunner2: aa
>线程IntegerRunner2: 1
>线程IntegerRunner1: 1
>线程StringRunner1: aaa
>线程StringRunner2: aaa
>线程IntegerRunner1: 2
>线程IntegerRunner2: 2
>线程StringRunner1: aaaa
>线程StringRunner2: aaaa
>线程IntegerRunner2: 3
>线程IntegerRunner1: 3
>线程StringRunner1: aaaaa
>线程StringRunner2: aaaaa
>调用get方法时，当前线程共享变量没有设置，调用initialValue获取默认值！
>调用get方法时，当前线程共享变量没有设置，调用initialValue获取默认值！
>线程IntegerRunner1: 0
>线程IntegerRunner2: 0

我们从这个例子中可以发现同样是调用的Thread Local.get方法，StringRunner线程和IntegerRunner线程获取的值并不一样，这也验证了Thread Local为每个使用该变量的线程都存有一份独立的副本。

## 4.ThreadLocal原理剖析

### 数据结构

![](<https://ws2.sinaimg.cn/large/006tKfTcly1g1o8rygu67j30me0n9q5h.jpg>)

我们根据这张图来分析一下ThreadLocal：

- 每个线程中都有一个ThreadLocalMap
- ThreadLocalMap里存储的是线程本地对象（Thread Local对象->key）和任意对象（value）
- Thread内部的Map是由ThreadLocal维护的，由ThreadLocal负责向map获取和设置线程的变量值。
- 线程里可以定义多个Thread Local对象。

所以对于不同的线程，每次获取副本值时，别的线程并不能获取到当前线程的副本值，形成了副本的隔离，互不干扰。

线程类Thread中对Thread Local Map的定义如下：

```java
/* ThreadLocal values pertaining to this thread. This map is maintained
     * by the ThreadLocal class. */
ThreadLocal.ThreadLocalMap threadLocals = null;
```

### Thread Local的核心方法

#### get()

- get()方法用于获取当前线程的副本变量值。

```java
public T get() {
  Thread t = Thread.currentThread();
  ThreadLocalMap map = getMap(t);
  if (map != null) {
    ThreadLocalMap.Entry e = map.getEntry(this);
    if (e != null) {
      @SuppressWarnings("unchecked")
      T result = (T)e.value;
      return result;
    }
  }
  return setInitialValue();
}
```

 1.获取当前线程的ThreadLocalMap对象threadLocals
 2.从map中获取线程存储的Entry节点。
 3.从Entry节点获取存储的Value副本值返回。

####set()

- set()方法用于保存当前线程的副本变量值。

```java
public void set(T value) {
  Thread t = Thread.currentThread();
  ThreadLocalMap map = getMap(t);
  if (map != null)
    map.set(this, value);
  else
    createMap(t, value);
}
```

1.获取当前线程的成员变量map
2.map非空，则重新将ThreadLocal和新的value副本放入到map中。
3.map空，则对线程的成员变量ThreadLocalMap进行初始化创建，并将ThreadLocal和value副本放入map中。

#### remove()

```java
public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }
 private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    expungeStaleEntry(i);
                    return;
                }
            }
        }
```

1. 获取当前线程成员变量map
2. 然后根据key一个个的删除

---

## 5.Thread Local Map原理

###数据结构

ThreadLocalMap是ThreadLocal的内部类，没有实现Map接口，用独立的方式实现了Map的功能，其内部的Entry也独立实现。

![](<https://upload-images.jianshu.io/upload_images/7432604-5bbe090d46789084.png>)

在ThreadLocalMap中，也是用Entry来保存K-V结构数据的。但是Entry中key只能是ThreadLocal对象，这点被Entry的构造方法已经限定死了。

```java
static class Entry extends WeakReference<ThreadLocal> {
    /** The value associated with this ThreadLocal. */
    Object value;

    Entry(ThreadLocal k, Object v) {
        super(k);
        value = v;
    }
}
```

Entry继承自WeakReference（弱引用，生命周期只能存活到下次GC前），但只有Key是弱引用类型的，Value并非弱引用。

### Hash冲突如何解决

和HashMap的最大的不同在于，ThreadLocalMap结构非常简单，没有next引用，也就是说ThreadLocalMap中解决Hash冲突的方式并非链表的方式，而是采用线性探测的方式，所谓线性探测，就是根据初始key的hashcode值确定元素在table数组中的位置，如果发现这个位置上已经有其他key值的元素被占用，则利用固定的算法寻找一定步长的下个位置，依次判断，直至找到能够存放的位置。

ThreadLocalMap解决Hash冲突的方式就是简单的步长加1或减1，寻找下一个相邻的位置。

```java
/**
 * Increment i modulo len.
 */
private static int nextIndex(int i, int len) {
    return ((i + 1 < len) ? i + 1 : 0);
}

/**
 * Decrement i modulo len.
 */
private static int prevIndex(int i, int len) {
    return ((i - 1 >= 0) ? i - 1 : len - 1);
}
```

显然ThreadLocalMap采用线性探测的方式解决Hash冲突的效率很低，如果有大量不同的ThreadLocal对象放入map中时发送冲突，或者发生二次冲突，则效率很低。

**所以这里引出的良好建议是：每个线程只存一个变量，这样的话所有的线程存放到map中的Key都是相同的ThreadLocal，如果一个线程要保存多个变量，就需要创建多个ThreadLocal，多个ThreadLocal放入Map中时会极大的增加Hash冲突的可能。**

### ThreadLocalMap存在的问题

由于ThreadLocalMap的key是弱引用，而Value是强引用。这就导致了一个问题，ThreadLocal在没有外部对象强引用时，发生GC时弱引用Key会被回收，而Value不会回收，如果创建ThreadLocal的线程一直持续运行，那么这个Entry对象中的value就有可能一直得不到回收，发生内存泄露。

**如何避免泄漏**
 既然Key是弱引用，那么我们要做的事，就是在调用ThreadLocal的get()、set()方法时完成后再调用remove方法，将Entry节点和Map的引用关系移除，这样整个Entry对象在GC Roots分析后就变成不可达了，下次GC的时候就可以被回收。

如果使用ThreadLocal的set方法之后，没有显示的调用remove方法，就有可能发生内存泄露，所以养成良好的编程习惯十分重要，使用完ThreadLocal之后，记得调用remove方法。

```java
ThreadLocal<Session> threadLocal = new ThreadLocal<Session>();
try {
    threadLocal.set(new Session(1, "Misout的博客"));
    // 其它业务逻辑
} finally {
    threadLocal.remove();
}
```

## 6.ThreadLocal的应用场景

1. 比如线程中处理一个非常复杂的业务，可能方法有很多，那么，使用 ThreadLocal 可以代替一些参数的显式传递；
2. 比如用来存储用户 Session。Session 的特性很适合 ThreadLocal ，因为 Session 之前当前会话周期内有效，会话结束便销毁。
3. 在一些多线程的情况下，如果用线程同步的方式，当并发比较高的时候会影响性能，可以改为 ThreadLocal 的方式，例如高性能序列化框架 Kyro 就要用 ThreadLocal 来保证高性能和线程安全；
4. 像线程内上线文管理器、数据库连接等可以用到 ThreadLocal;

## 7.总结

- 每个ThreadLocal只能保存一个变量副本，如果想要上线一个线程能够保存多个副本以上，就需要创建多个ThreadLocal。

- ThreadLocal内部的ThreadLocalMap键为弱引用，会有内存泄漏的风险。

- 适用于无状态，副本变量独立后不影响业务逻辑的高并发场景。如果如果业务逻辑强依赖于副本变量，则不适合用ThreadLocal解决，需要另寻解决方案。

---

> 参考：
>
> <https://www.jianshu.com/p/98b68c97df9b>
>
> 并发编程的艺术