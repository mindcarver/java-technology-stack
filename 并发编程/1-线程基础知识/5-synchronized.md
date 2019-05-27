# synchrnized关键字

## 一：简单介绍

之前我们写的大多数程序都是一个线程在操作，那么先来看一下下面这个例子：

```java
public class UnsafeThreadCallDemo {
    private static int i = 0;
    public static class Mythread extends Thread{
        @Override
        public void run() {
            for (int j = 0; j < 1000; j++) {
                i++;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Mythread t1 = new Mythread();
        Mythread t2 = new Mythread();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("i最后的值是：" + i);
    }
}
```

我们从结果中可以发现在很多时候，i的值总是会小于2000，并不是我们以为的2000.因为在i++的时候可能t1线程和t2线程同时读取了i,并且做了++操作，导致了重复。所以我们必须保证多个线程对i进行操作的时候完全同步。即t1线程写入的时候，t2不能读也不能写。因此引出了synchonized关键字。

## 二：java同步块

Java中的同步块用`synchronized`关键字标记。Java中的同步块在`某个对象上`同步。在同一对象上同步的所有同步块只能`同时在其中执行一个线程`。`尝试进入同步块的所有其他线程将被阻塞，直到同步块内的线程退出块`。

该`synchronized`关键字可用于标记四种不同类型的块：

1. 实例方法中的代码块
2. 静态方法
3. 实例方法
4. 静态方法中的代码块

---

> <font color="red">1.实例方法中的同步块</font>

**基本用法：**

```java
 public void add(int value){
    synchronized(this){
       this.count += value;   
    }
  }
```

**案例：**

> 现在有50个人去银行排队叫号，一共3个窗口，如何有条不紊的进行？

```java
public class SynchronizedRunableDemo implements Runnable {
	// 叫号从1号开始
    private int index = 1;
	// 一天只服务200人
    private final static int MAX = 200;
	// 监视器
    private final Object MONITOR = new Object();

    @Override
    public void run() {

        while (true) {
            synchronized (MONITOR) {
                // 如果超过500 直接跳出
                if (index > MAX)
                    break;
                try {
                    // 线程停顿50毫秒
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread() + " 的号码是:" + (index++));
            }
        }
    }

```

```java
public class Client {
    public static void main(String[] args) {
        final SynchronizedRunableDemo ticketWindow = new SynchronizedRunableDemo();
        Thread windowThread1 = new Thread(ticketWindow, "一号窗口");
        Thread windowThread2 = new Thread(ticketWindow, "二号窗口");
        Thread windowThread3 = new Thread(ticketWindow, "三号窗口");
        windowThread1.start();
        windowThread2.start();
        windowThread3.start();
    }
}
```

**运行结果：**

>截取的部分结果：
>
>Thread[三号窗口,5,main] 的号码是:59
>Thread[三号窗口,5,main] 的号码是:60
>Thread[三号窗口,5,main] 的号码是:61
>Thread[三号窗口,5,main] 的号码是:62
>Thread[三号窗口,5,main] 的号码是:63
>Thread[三号窗口,5,main] 的号码是:64
>Thread[一号窗口,5,main] 的号码是:65
>Thread[二号窗口,5,main] 的号码是:66
>Thread[二号窗口,5,main] 的号码是:67
>
>.....
>
>Thread[一号窗口,5,main] 的号码是:200

在这个例子中，通过synchronized代码块来保证同一时间只有一个线程在synchronized同步的代码块中进行执行，也就是说synchronized中的代码块是以单线程执行的。所以最后输出来的一定是200，而如果不用synchronized进行同步，那么输出的结果也许就不止200了。

---

> <font color="red">2.同步实例方法</font>


**基本用法：**

```java
 public synchronized void add(int value){
      this.count += value;
  }
```

> 案例依旧是银行取号

```java
public class SynchronizedInstanceMethodDemo implements Runnable {
	
    private int index = 1;

    private final static int MAX = 200;

    @Override
    public void run() {
       while (true){
            if(!callNumber()){
                break;
            }
       }

    }
    // 叫号 同步实例方法
    private synchronized boolean callNumber(){
        if(index > MAX){
            return false;
        } 
        try {
            // 模拟取号耗时
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread() + " 的号码是:" + (index++));
        return  true;
    }
}
```

> 运行结果：
>
> Thread[二号窗口,5,main] 的号码是:176
> ......
> Thread[二号窗口,5,main] 的号码是:198
> Thread[二号窗口,5,main] 的号码是:199
> Thread[二号窗口,5,main] 的号码是:200

----

> <font color="red">3.同步静态方法</font>

```java
public class SynchronizedStaticInstanceDemo  {

    public static synchronized void addM1(){
        for(int i=1 ;i<5;i++){
            System.out.println(Thread.currentThread().getName()+":"+i);
        }
        try {
            // 假设要执行5秒
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static synchronized void addM2(){
        for(int i=6 ;i<10;i++){
            System.out.println(Thread.currentThread().getName()+":"+i);
        }
        try {
            // 假设要执行5秒
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

```java
public class Client {
    public static void main(String[] args) {
        Thread t1 = new Thread(new Runnable(){
            @Override
            public void run() {
                SynchronizedStaticInstanceDemo.addM1();
            }
        },"Thread-1");

        Thread t2 = new Thread(new Runnable(){
            @Override
            public void run() {
                SynchronizedStaticInstanceDemo.addM2();
            }
        },"Thread-2");

        t1.start();
        t2.start();
    }
}

```

**运行结果：**

> Thread-2:6
> Thread-2:7
> Thread-2:8
> Thread-2:9
> Thread-1:1
> Thread-1:2
> Thread-1:3
> Thread-1:4

线程会在线程2执行完后等待5秒左右才会执行。

----
> <font color="red">4.静态方法中的同步块</font>


同样还是上面的计算案例：

```java
 public static  void addM2(){
     // 只不过这里发生的写法变化
        synchronized (SynchronizedStaticInstanceDemo.class){
            for(int i=6 ;i<10;i++){
                System.out.println(Thread.currentThread().getName()+":"+i);
            }
            try {
                // 假设要执行5秒
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
```



## 三：this锁、class锁和object锁

> <font color="red">1.syschronized(this)</font>

`this锁指的是锁住同步方法或者同步代码块所属的对象实例`。synchronized以此作为监视器对象，我们通过同步代码块和同步方法来证实这一点：

```java
public class SynchronizedThisLock {
    public static void main(String[] args) {

        ThisLock thisLock = new ThisLock();
        new Thread("Thread-T1") {
            @Override
            public void run() {
                thisLock.m1();
            }
        }.start();

        new Thread("Thread-T2") {
            @Override
            public void run() {
                thisLock.m2();
            }
        }.start();
    }
}

class ThisLock {
    // m1方法是一个同步方法
    public synchronized void m1() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
	
    // 而m2是一个普通方法，但是同步代码块的锁是this锁
    public void m2() {
        synchronized (this) {
            try {
                System.out.println(Thread.currentThread().getName());
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

```

**运行结果：**

> Thread-T1
> Thread-T2

在线程T1执行5秒之后，打印出T2，说明了synchronized同步方法的锁是`this锁`.此处的this指的就是thisLock。

---

> <font color="red">2.synchronized(class)</font>

static synchronized 表示锁住的是类的class对象，我们来验证一下：

```java
public class SynchronizedStaticDemo {

    public synchronized static void m1() {
        System.out.println("m1 " + Thread.currentThread().getName());
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void m2() {
        synchronized (SynchronizedStaticDemo.class){
            System.out.println("m2 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

```

```java
public class Client {
    public static void main(String[] args) {
        new Thread("T1") {
            @Override
            public void run() {
                SynchronizedStaticDemo.m1();
            }
        }.start();

        new Thread("T2") {
            @Override
            public void run() {
                SynchronizedStaticDemo.m2();
            }
        }.start();

    }
}
```

运行结果：

> m1 T1
> m2 T2

在打印出m1 T1 之后，停顿了5秒，才打印出m2 T2， 说明静态同步方法锁住的是类的class对象。

---

> <font color="red">3. syschronized(Object)</font>

通过创建不可变对象Object作为同步的监视器：

```java
public class SynchronizedObjectLock {

    private final Object LOCK = new Object();

    public  void m1() {
        synchronized (LOCK){
            System.out.println("m1 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
    public void m2() {
        synchronized (LOCK){
            System.out.println("m2 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

```java
public class Client {
    public static void main(String[] args) {
        SynchronizedObjectLock lock = new SynchronizedObjectLock();
        new Thread("T1") {
            @Override
            public void run() {
                lock.m1();
            }
        }.start();

        new Thread("T2") {
            @Override
            public void run() {
                lock.m2();
            }
        }.start();
    }
}

```

**打印结果：**

> m1 T1
> m2 T2

m1 T1在执行了5秒之后，才执行m2 T2.

----

> <font color="red">4.同时使用this锁和class锁和Object锁</font>

来测试一下同时使用3种锁：

```java
public class ThreeLockDemo {

    private final Object LOCK = new Object();

    public void m1() {
        synchronized (this){
            System.out.println("m1 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static void m2() {
        synchronized (ThreeLockDemo.class){
            System.out.println("m2 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public  void m3() {
        synchronized (LOCK){
            System.out.println("m3 " + Thread.currentThread().getName());
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
}
```

```java
public class Client {
    public static void main(String[] args) {
        ThreeLockDemo threeLockDemo = new ThreeLockDemo();
        new Thread("T1") {
            @Override
            public void run() {
                threeLockDemo.m1();
            }
        }.start();

        new Thread("T2") {
            @Override
            public void run() {
                threeLockDemo.m2();
            }
        }.start();

        new Thread("T3") {
            @Override
            public void run() {
                threeLockDemo.m3();
            }
        }.start();
    }
}
```


**运行结果：**

> m1 T1
> m2 T2
> m3 T3

上面3行同时打印出来，说明了每个线程之间的锁不一样，不需要等待其他线程执行完毕再次执行。

---

