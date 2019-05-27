# 一 : 接口

## 1.1 接口概念

- 在 Java 程序设计语言中， 接口不是类，而是对类的一组需求描述，这些类要遵从接口描述的统一格式进行定义。

- 接口中的所有方法自动地属于 public。 因此，在接口中声明方法时，不必提供关键字 public ,在实现接口时， 必须把方法声明为 public.

- 为了让类实现一个接口， 通常需要下面两个步骤：

  1. 将类声明为实现给定的接口。
  2. 对接口中的所有方法进行定义。

- 使用Comparable接口为对象排序, Arrays类中的 sort 方法承诺可以对对象数组进行排序，但要求满 足下列前提：对象所属的类必须实现了 Comparable 接口

  ```java
  public class Test {
  
              public static void main(String[] args) throws IOException {
  
                  People[] arr = new People[5];
                  arr[0] = new People("张三",18);
                  arr[1] = new People("李四",8);
                  arr[2] = new People("大白",9);
                  arr[3] = new People("小白",34);
                  arr[4] = new People("零",24);
  
                  Arrays.sort(arr);
  
                  for(People people: arr){
                      System.out.println(people.toString());
                  }
      }
  }
  ///////////////////////////////////////////////////////
  public class People implements  Comparable<People>  {
  
      private String name;
      private int age;
  
      public People() {
      }
  
      public People(String name, int age) {
          this.name = name;
          this.age = age;
      }
  
      public String getName() {
          return name;
      }
  
      public void setName(String name) {
          this.name = name;
      }
  
      public int getAge() {
          return age;
      }
  
      public void setAge(int age) {
          this.age = age;
      }
      
      @Override
      public String toString() {
          return "People{" +
                  "name='" + name + '\'' +
                  ", age=" + age +
                  '}';
      }
  
      @Override
      public int compareTo(People o) {
      //如果 x < y 返回一个负整数；如果 x 和 y 相等，则返回 0; 否则返回一个负整数。
          return Integer.compare(age, o.getAge());
      }
  }
  
  ```

## 1.2 接口特性

- 接口不是类，尤其不能使用 new运算符实例化一个接口：
  `x = new Comparable(. . .); // ERROR`
- 然而， 尽管不能构造接口的对象，却能声明接口的变量：
  `Comparable x; // OK`
- 也可以使用 instance 检查一个对象是否实现了某个特定的接口：
  `if (anObject instanceof Comparable) { . . . }`
- 接口也可以被扩展。这里允许存在多条从具有较高通用 性的接口到较高专用性的接口的链

```java
public interface Moveable {
    void move(double x, double y);
}
\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
public interface Powered extends  Moveable{
    double milesPerCallon();
    // 接口中的域将被自动设为 public static final
    double SPEED_LIHIT = 95; // a public static final constant
}

```

虽然在接口中不能包含实例域或静态方法，但却可以包含常量。与接口中的方法都自动地被设置为 public—样，接口中的域将被自动设为public static final


- **接口中的常量在实现类中可以就像类的私有变量一样直接使用;**

  

## 1.3 接口与抽象类

- 接口与抽象类的区别:
  - 每个类只能扩展于一个类.
  - 一个类可以实现多个接口.

## 1.4 静态方法

- 在 Java SE 8中，允许在接口中增加静态方法。理论上讲，没有任何理由认为这是不合法 的。只是这有违于将接口作为抽象规范的初衷。

  ```java
  public interface Moveable {
      void move(double x, double y);
      // 静态方法
      static int add(int a,int b){
          return a+b;
      }
  }
  \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
  		// 直接类名调用就行
          int add = Moveable.add(1, 2);
          System.out.println(add);
  
  ```

- 不过整个 Java 库都以这种方式重构也是不太可能的， 但是实现你自己的接口时，不再需 要为实用工具方法另外提供一个伴随类。

- **静态方法不需要实现类实现.**

## 1.5 默认方法

- **可以为接口方法提供一个默认实现。必须用 default 修饰符标记这样一个方法。**

- **默认方法可以不用覆盖,直接使用实现类的实例调用即可.**

- **默认方法可以什么也不做。 然后使用实现类覆盖即可,这是使用默认方法的正确方式.**

- 当一个接口中有默认方法时,我们实现这个接口时就不用在实现这个方法,而当我们有需要使用这个方法时,就可以覆盖这个方法.

- 当我们需要使用某一个接口的方法时,必须实现这个接口,也就必须实现接口的所有方法,那么我们只是需要一个方法,但必须实现所有的方法,这就比较麻烦,我们可以使用默认方法解决这个问题,把所有方法都设置为默认方法,当我们实现这个接口时,只需要覆盖我们需要的那个方法,其他方法则不需要关心.

  ```java
  public interface Moveable {
      void move(double x, double y);
      int NUM = 5;
      // 默认方法
      default int add(int a,int b){
          return a+b+NUM;
      }
  }
  ////////////////////////////////////////
  // 在实现类中覆盖 或者不覆盖,直接对象调用,但最好别这样做.
      @Override
      public int add(int a, int b) {
          return (a+b+NUM)*2;
      }
  
  ```

- 默认方法的一个重要作用是`接口演化`;我们要为以前的接口类再添加一个方法,则实现该接口的所有类都必须实现该方法,那么我们要修改源代码,非常麻烦,
  但是我们可以定义一个默认方法,就不用在实现该方法了.只需用到的实现类重写该方法即可.

## 1.6 解决默认方法冲突

- 如果先在一个接口中将一个方法定义为默认方法， 然后又在超类或另一个接口中定义了 同样的方法， 会发生什么情况？ 诸如 Scala 和 C++ 等语言对于解决这种二义性有一些复杂的 规则。幸运的是，Java 的相应规则要简单得多。规则如下：

  - 超类优先。如果超类提供了一个具体方法，同名而且有相同参数类型的默认方法会 被忽略。
  - 接口冲突。 如果一个超接口提供了一个默认方法，另一个接口提供了一个同名而且 参数类型（不论是否是默认参数）相同的方法， 必须覆盖这个方法来解决冲突。
- **如果两个接口都有相同的默认方法,我们可以在实现类中重写这个方法,就不会产生冲突了.**

# 二 接口示例

## 2.1 接口与回调

- 回调（callback) 是一种常见的程序设计模式。在这种模式中，可以指出某个特定事件发 生时应该采取的动作。例如，可以指出在按下鼠标或选择某个菜单项时应该采取什么行动。

- 程序 给出了定时器和监听器的操作行为。在定时器启动以后， 程序将弹出一个 消息对话框， 并等待用户点击 Ok 按钮来终止程序的执行。在程序等待用户操作的同时， 每 隔 10 秒显示一次当前的时间。 运行这个程序时要有一些耐心。程序启动后， 将会立即显示一个包含“ Quit program?” 字样的对话框， 10 秒钟之后， 第 1 条定时器消息才会显示出来。

  ```java
  import java.awt.*;
  import java.awt.event.ActionEvent;
  import java.awt.event.ActionListener;
  import java.util.Date;
  
  public class TimePrinter implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {
          System.out.println("At the tone, the time is " + new Date());
          // static Toolkit getDefaultToolkit() : 获得默认的工具箱。T.具箱包含有关 GUI 环境的信息。 
          // void beep() 发出一声铃响。 
          Toolkit.getDefaultToolkit().beep();
      }
  }
  \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
  import javax.swing.*;
  import java.awt.event.ActionListener;
  import java.io.IOException;
  
  public class Test {
  
      public static void main(String[] args) throws IOException {
          ActionListener listener =  new TimePrinter();
  		//构造一个定时器， 每隔 interval 毫秒通告 listener—次，
          Timer timer = new Timer(10000,listener);
          //启动定时器一旦启动成功， 定时器将调用监听器的 actionPerformed。 
          timer.start();
          //显示一个包含一条消息和 OK 按钮的对话框。这个对话框将位于其 parent组件的中 央。如果 parent 为 mill, 对话框将显示在屏幕的中央，
          JOptionPane.showMessageDialog(null, "Quit program?");
          System.exit(0);
      }
  }
  
  
  ```
  

## 2.2 Comparator 接口

- 我们已经了解了如何对一个对象数组排序，前提是这些对象是实现了 Comparable 接口的类的实例 , 例如， 可以对一个字符串数组排序， 因为 String类实现了 Comparable, 而且 String.compareTo方法可以按字典顺序比较字符串。

- 现在假设我们希望按长度递增的顺序对字符串进行排序，而不是按字典顺序进行排序。 肯定不能让 String类用两种不同的方式实现 compareTo方法— —更何况，String类也不应由我们来修改 。

- 要处理这种情况，ArrayS.Sort 方法还有第二个版本， 有一个数组和一个比较器 (comparator )作为参数， 比较器是实现了 Comparator 接口的类的实例。

  ```java
  import java.util.Comparator;
  
  /**
   * 按长度比较字符串，可以如下定义一个实现 Comparator<String> 的类
   */
  public class LengthComparator implements Comparator<String> {
      @Override
      public int compare(String first, String second) {
          return first.length() - second.length();
      }
  }
  \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
  import java.io.IOException;
  import java.util.Arrays;
  
  public class Test {
  
      public static void main(String[] args) throws IOException {
  
          String[] words = {"aaa","zhangsan","vvvv","d"};
          LengthComparator comparator = new LengthComparator();
  
          Arrays.sort(words,comparator);
  
          for(String word: words){
              System.out.println(word);
          }
      }
  }
  
  ```

## 2.3 对象克隆

- 本节我们会讨论 Cloneable 接口，这个接口指示一个类提供了一个安全的 clone方法。由 于克隆并不太常见，而且有关的细节技术性很强，你可能只是想稍做了解， 等真正需要时再 深人学习。
- 要了解克隆的具体含义，先来回忆为一个包含对象引用的变量建立副本时会发生什么。 原变量和副本都是同一个对象的引用（见图 6-1 )。这说明， 任何一个变量改变都会影响另一 个变量。

```java
Employee original = new Employee("John Public", 50000); 
Employee copy = original; 
copy.raiseSalary(lO); // oops-also changed original 

```

![image-20190418081545252](https://ws4.sinaimg.cn/large/006tNc79ly1g26h113vcmj30r00we139.jpg)

- **如果希望 copy 是一个新对象，它的初始状态与 original 相同， 但是之后它们各自会有自 己不同的状态， 这种情况下就可以使用 clone 方法**

# 三 lambda 表达式

## 3.1 lambda 表达式的语法

- **lambda 表达式是一个可传递的代码块， 可以在以后执行一次或多次**

- 即使 lambda 表达式没有参数， 仍然要提供空括号，就像无参数方法一样：
  `0 -> { for (inti = 100;i >= 0;i ) System.out.println(i); }`

- 如果可以推导出一个 lambda 表达式的参数类型，则可以忽略其类型。例如：

  ```java
          Comparator<String> comp
                  = (first,second) // Same as (String first, String second)
                      -> first.length() - second.length();
  
  ```

- 在这里， 编译器可以推导出 first 和 second 必然是字符串，因为这个 lambda 表达式将赋 给一个字符串比较器。

- 如果方法只有一 参数， 而且这个参数的类型可以推导得出，那么甚至还可以省略小括号：

```java
ActionListener listener = event -> 
	System.out.println("The time is " + new Date()");
	 // Instead of (event) -> . .. or (ActionEvent event) -> ... 

```

- 无需指定 lambda 表达式的返回类型。lambda 表达式的返回类型总是会由上下文推导得 出。例如，下面的表达式
  `(String first, String second) -> first.length() - second.length()`

## 3.2 函数式接口(重点)

- 对于**只有一个抽象方法**的接口， 需要这种接口的对象时， 就可以提供一个 lambda 表达 式。这种接口称为**函数式接口** （functional interface)。
- **函数式接口解释: 只能有一个抽象方法,可以有其他的默认方法,静态方法,但是抽象方法只能有一个.**
- 对于只有一个抽象方法的接口， 需要这种接口的对象时， 就可以提供一个 lambda 表达 式。这种接口称为函数式接口 （functional interface)。
- java.util.function 包中有一个尤其有用的接口 Predicate:

```java
package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface Predicate<T> {
	// 抽象方法
    boolean test(T t);

    default Predicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    default Predicate<T> negate() {
        return (t) -> !test(t);
    }

    default Predicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    static <T> Predicate<T> isEqual(Object targetRef) {
        return (null == targetRef)
                ? Objects::isNull
                : object -> targetRef.equals(object);
    }
}
\

```

- ArrayList 类有一个 removelf 方法， 它的参数就是一个 Predicate。这个接口专门用来传递 lambda 表达式。例如，下面的语句将从一个数组列表删除所有 null 值：

  ```java
  list.removelf(e -> e == null);
  
  ```

## 3.3 方法引用

- 有时， 可能已经有现成的方法可以完成你想要传递到其他代码的某个动作。例如， 假设你希望只要出现一个定时器事件就打印这个事件对象。当然，为此也可以调用:
  `Timer t = new Timer(1000, event -> System.out.println(event)):`
- 但是，如果直接把 println 方法传递到 Timer 构造器就更好了。具体做法如下：
  `Timer t = new Timer(1000, System.out::println);`
- 表达式 `System.out::println`是一个**方法引用**（method reference), 它等价于 lambda 表达式`x 一> System.out.println(x)`
- 再来看一个例子， 假设你想对字符串排序， 而不考虑字母的大小写。可以传递以下方法 表达式：
  `Arrays.sort(strings，String::conpareToIgnoreCase)`
- 从这些例子可以看出，要用 :::操作符分隔方法名与对象或类名。主要有 3 种情况：
  - object::instanceMethod
  - Class::staticMethod
  - Class::instanceMethod
- 在前 2 种情况中，方法引用等价于提供方法参数的 lambda 表达式。前面已经提到， System.out::println 等价于 x -> System.out.println(x)。 类似地，Math::pow 等价于（x，y) -> Math.pow(x, y)。
- 对于第 3 种情况， 第 1 个参数会成为方法的目标。例如，String::compareToIgnoreCase 等 同于(x,y)-> x.compareToIgnoreCase(y)
- 可以在方法引用中使用 this 参数。例如，this::equals 等同于 x-> this.equals(x)。使用 super 也是合法的。下面的方法表达式
  `super::instanceMethod`
- 使用 super作为目标，会调用给定方法的超类版本。

## 3.4 构造器引用 (略过)

## 3.5 变量作用域

- 做一个总结: 在lambda中,不能改变代码块之外的变量值,也不能使用代码块之外会改变值的变量.
- 通常， 你可能希望能够在 lambda 表达式中访问外围方法或类中的变量。考虑下面这个 例子：

```java
public static void repeatMessage(String text, int delay) 
{ 
	ActionListener listener = event -> 
	{
		System.out.println(text);
		Toolkit.getDefaultToolkitO.beep();
	};
new Timer(delay, listener).start();
}

```

- 来看这样一个调用：
  `repeatMessage("Hello", 1000); // Prints Hello every 1 ,000 milliseconds`
- lambda 表达式有 3 个部分：
  -  一个代码块；
  - 参数;
  - 自由变量的值， 这是指非参数而且不在代码中定义的变量。

> 在我们的例子中， 这个 lambda 表达式有 1 个自由变量 text。表示 lambda 表达式的数据 结构必须存储自由变量的值，在这里就是字符串 “Hello”。我们说它被 lambda 表达式捕获

> 可以看到，lambda 表达式可以捕获外围作用域中变量的值。在 Java 中， 要确保所捕获 的值是明确定义的，这里有一个重要的限制。在 lambda 表达式中， 只能引用值不会改变的 变量

- l**ambda 表达式中捕获的变量必须实际上是最终变量 (effectivelyfinal)。 实际上的最终变量是指， 这个变量初始化之后就不会再为它赋新值。在这里，text 总是指示 同一个 String对象，所以捕获这个变量是合法的**

## 3.6 处理 lambda 表达式

使用 lambda 表达式的重点是延迟执行 (deferredexecution ) 毕竟， 如果想耍立即执行代 码，完全可以直接执行， 而无需把它包装在一个lambda 表达式中。之所以希望以后再执行 代码， 这有很多原因， 如：

- 在一个单独的线程中运行代码
- 多次运行代码；
- 在算法的适当位置运行代码（例如， 排序中的比较操作)；
- 发生某种情况时执行代码（如， 点击了一个按钮， 数据到达， 等等)；

> 如果设计你自己的接口，其中只有一个抽象方法， 可以用 @FunctionalInterface 注 解来标记这个接口。这样做有两个优点。如果你无意中增加了另一个非抽象方法， 编译 器会产生一个错误消息。 另外javadoc 页里会指出你的接口是一个函数式接口。 并不是必须使用注解根据定义，任何有一个抽象方法的接口都是函数式接口。不 过使用 @FunctionalInterface 注解确实是一个很好的做法。

> 最好使用表 6-1 或表 6-2 中的接口。 例如， 假设要编写一个方法来处理满足 某个特定条件的文件。 对此有一个遗留接口java.io.FileFilter, 不过最好使用标准的 Predicate , 只有一种情况下可以不这么做， 那就是你已经有很多有用的方法可以生 成 FileFilter 实例。

## 3.7 再谈 Comparator

> Comparator 接口包含很多方便的静态方法来创建比较器。这些方法可以用于 lambda 表 达式或方法引用。

> 静态 comparing 方法取一个“ 键提取器” 函数， 它将类型 T 映射为一个可比较的类型 (如 String)。对要比较的对象应用这个函数， 然后对返回的键完成比较。例如，假设有一个 Person 对象数组，可以如下按名字对这些对象排序：

```
Arrays.sort(people, Comparator.comparing(Person::getName));
```

> 与手动实现一个 Compamtor 相比， 这当然要容易得多。另外， 代码也更为清晰， 因为显 然我们都希望按人名来进行比较。

- 可以把比较器与 thenComparing 方法串起来。例如，

```java
        Arrays.sort(arr,
                    Comparator.comparing(People::getName,
                            (s, t) -> Integer.compare(s.length(), t.length()))
                            .thenComparing(People::getName)
                            .thenComparing(People::getAge)

                    );

```

> 另外，comparing 和 thenComparing 方法都有变体形式，可以避免 int、 long 或 double 值 的装箱。要完成前一个操作，还有一种更容易的做法：

```java
Arrays.sort(arr, Comparator.comparinglnt(p -> p.getName-length()));
```

```java
public class Test {

    public static void main(String[] args) throws IOException {

        // 对象数组
        People[] arr ={new People("zhangsan",12),
                        new People("lisi",17),
                        new People("lisi",15),
                        new People("wangwu",12),
                        new People("lisi",78),
                        new People("dabai",12),
                        new People("haha",30),
                        new People("lisi",1),};

        // 按照名字对对象排序
        Arrays.sort(arr,Comparator.comparing(People::getName));
        //System.out.println( Arrays.toString(arr));
        for (People p : arr){
            System.out.println(p.toString());
        }

        System.out.println("----------------------------------");
        // 如果名字相同,可以追加一个(多个)比较器,按照年龄排序
        Arrays.sort(arr, Comparator.comparing(People::getName).thenComparing(People::getAge));
        for (People p : arr){
            System.out.println(p.toString());
        }

        System.out.println("----------------------------------");
        // 可以为 comparing 和 thenComparing 方法提取的键指定一个 比较器。
        // 例如，可以如下根据人名长度完成排序,
        //  然后长度相同的按照名字排序.
        // 名字在相同的按照年龄排序.
/*        Arrays.sort(arr,
                    Comparator.comparing(People::getName,
                            (s, t) -> Integer.compare(s.length(), t.length()))
                            .thenComparing(People::getName)
                            .thenComparing(People::getAge)

                    );*/

        // 按照人名长度完成排序的另一种写法
        Arrays.sort(arr,Comparator.comparingInt(p->p.getName().length()));

        for (People p : arr){
            System.out.println(p.toString());
        }

    }
}


```

