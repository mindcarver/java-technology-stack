Java中的final关键字非常重要，它可以应用于类、方法以及变量。这篇文章中我将带你看看什么是final关键字？将变量，方法和类声明为final代表了什么？使用final的好处是什么？最后也有一些使用final关键字的实例。final经常和static一起使用来声明常量，你也会看到final是如何改善应用性能的。

### final关键字的含义?

final在Java中是一个保留的关键字，可以声明成员变量、方法、类以及本地变量。一旦你将引用声明作final，你将不能改变这个引用了，编译器会检查代码，如果你试图将变量再次初始化的话，编译器会报编译错误。

### 什么是final变量？

凡是对成员变量或者本地变量(在方法中的或者代码块中的变量称为本地变量)声明为final的都叫作final变量。final变量经常和static关键字一起使用，作为常量。下面是final变量的例子：

```java
public static final String NAME = "tom";
NAME = new String("jom") 
```

final变量是只读的。

### 什么是final方法?

final也可以声明方法。方法前面加上final关键字，代表这个方法不可以被子类的方法重写。如果你认为一个方法的功能已经足够完整了，子类中不需要改变的话，你可以声明此方法为final。final方法比非final方法要快，因为在编译的时候已经静态绑定了，不需要在运行时再动态绑定。下面是final方法的例子：

```java
class PersonalLoan{
    public final String getName(){
        return "personal loan";
    }
}
 
class CheapPersonalLoan extends PersonalLoan{
    @Override
    public final String getName(){
        return "cheap personal loan"; //compilation error: overridden method is final
    }
```

### 什么是final类？

使用final来修饰的类叫作final类。final类通常功能是完整的，它们不能被继承。Java中有许多类是final的，譬如String, Interger以及其他包装类。下面是final类的实例：

```java
final class PersonalLoan{
 
    }
 
    class CheapPersonalLoan extends PersonalLoan{  //compilation error: cannot inherit from final class
 
}
```

### final关键字的好处

下面总结了一些使用final关键字的好处

1. final关键字提高了性能。JVM和Java应用都会缓存final变量。
2. final变量可以安全的在多线程环境下进行共享，而不需要额外的同步开销。
3. 使用final关键字，JVM会对方法、变量及类进行优化。

### 不可变类

创建不可变类要使用final关键字。不可变类是指它的对象一旦被创建了就不能被更改了。String是不可变类的代表。不可变类有很多好处，譬如它们的对象是只读的，可以在多线程环境下安全的共享，不用额外的同步开销等等。

### 关于final的重要知识点

1. final关键字可以用于成员变量、本地变量、方法以及类。

2. final成员变量必须在声明的时候初始化或者在构造器中初始化，否则就会报编译错误。

3. 你不能够对final变量再次赋值。

4. 本地变量必须在声明时赋值。

5. 在匿名类中所有变量都必须是final变量。

6. final方法不能被重写。

7. final类不能被继承。

8. final关键字不同于finally关键字，后者用于异常处理。

9. final关键字容易与finalize()方法搞混，后者是在Object类中定义的方法，是在垃圾回收之前被JVM调用的方法。

10. 接口中声明的所有变量本身是final的。

11. final和abstract这两个关键字是反相关的，final类就不可能是abstract的。

12. final方法在编译阶段绑定，称为静态绑定(static binding)。

13. 没有在声明时初始化final变量的称为空白final变量(blank final variable)，它们必须在构造器中初始化，或者调用this()初始化。不这么做的话，编译器会报错“final变量(变量名)需要进行初始化”。

14. 将类、方法、变量声明为final能够提高性能，这样JVM就有机会进行估计，然后优化。

15. 按照Java代码惯例，final变量就是常量，而且通常常量名要大写：

    ```java
    private final int COUNT = 10;
    ```

    1. 对于集合对象声明为final指的是引用不能被更改，但是你可以向其中增加，删除或者改变内容。譬如：

       ```java
       private final List Loans = new ArrayList();
       list.add(“home loan”);  //valid
       list.add("personal loan"); //valid
       loans = new Vector();  //not valid
       ```

-----

