# 一 类,超类和子类

## 1.1 阻止继承：final 类和方法

- 有时候，可能希望阻止人们利用某个类定义子类。不允许扩展的类被称为 final 类。如果在定义类的时候使用了 final 修饰符就表明这个类是 final 类。
- **如果将一个类声明为 final， 只有其中的方法自动地成为 final, 而不包括域。**

## 1.2 强制类型转换

- 将一个类型强制转换成另外一个类型的过程被称为类型转.
- 进行类型转换的唯一原因是：在暂时忽视对象的实际类型之后，使用对象的全部功能.
- 将一个值存人变量时， 编译器将检查是否允许该操作。将一个子类的引用赋给一个超类 变量， 编译器是允许的。但将一个超类的引用赋给一个子类变量， 必须进行类型转换， 这样才能够通过运行时的检査。
- 综上所述：
  - 只能在继承层次内进行类型转换。
  - 在将超类转换成子类之前，应该使用 instanceof进行检查。

## 1.3 抽象类

- 为了提高程序的清晰度， 包含一个或多个抽象方法的类本身必须被声明为抽象的。
- 使用 abstract 关键字声明抽象类或者抽象方法.
- 除了抽象方法之外，抽象类还可以包含具体数据和具体方法。
- 扩展抽象类可以有两种选择:
  - 一种是在抽象类中定义部分抽象类方法或不定义抽象类方法，这样就必须将子类也标记为抽 象类；
  - 另一种是定义全部的抽象方法，这样一来，子类就不是抽象的了。
- 类即使不含抽象方法，也可以将类声明为抽象类。
- 抽象类不能被实例化。也就是说，如果将一个类声明为 abstract, 就不能创建这个类的对 象。
- 可以定义一个抽象类的对象变量， 但是它只能引用非抽象子类的对象。

## 1.4 受保护访问

- 在有些时候，人们希望超类中的某些方法允许被子类访问， 或允许子类的方法访 问超类的某个域。为此， 需要将这些方法或域声明为 protected。例如，如果将超类 Employee 中的 hireDay 声明为 proteced, 而不是私有的， Manager 中的方法就可以直接地访问它。
- Manager 类中的方法只能够访问 Manager 对象中的 hireDay 域， 而不能访问其他 Employee 对象中的这个域
-  Java 用于控制可见性的 4 个访问修饰符：
  - 仅对本类可见 private。
  - 对所有类可见 public
  - 对本包和所有子类可见 protected。
  - 对本包可见— —默认， 不需要修饰符。

# 二 Object： 所有类的超类

- 可以使用 Object 类型的变量引用任何类型的对象：
  `Object obj = new EmployeeC'Harry Hacker", 35000);`
- **在 Java 中，只有基本类型（primitive types) 不是对象， 例如，数值、 字符和布尔类型的 值都不是对象。**
- **所有的数组类型，不管是对象数组还是基本类型的数组都扩展了 Object 类。**

## 2.1 equals 方法

- Object 类中的 equals方法用于检测一个**对象是否等于另外一个对象**
- **经常需要检测两个对象状态的相等性，如果两个对象的状态相等， 就认为这两个对象 是相等的。例如， 如果两个雇员对象的姓名、 薪水和雇佣日期都一样， 就认为它们是相等的**
- Java语言规范要求 equals 方法具有下面的特性：
  - 自反性：对于任何非空引用 x, x.equals(x)应该返回 true.
  - 对称性: 对于任何引用 x 和 y, 当且仅当 y.equals(x) 返回 true, x.equals(y) 也应该返 回 true。
  - 传递性： 对于任何引用 x、 y 和 z, 如果 x.equals(y) 返回true， y.equals(z)返回 true, x.equals(z) 也应该返回 true。
  - 一致性： 如果 x 和 y引用的对象没有发生变化，反复调用 x.equals(y) 应该返回同样 的结果。
  - 对于任意非空引用 x, x.equals(null) 应该返回 false

## 2.2 hashCode 方法

- 散列码（ hash code) 是由对象导出的一个整型值。散列码是没有规律的。如果 x 和 y 是 两个不同的对象， x.hashCode( ) 与 y.hashCode( ) 基本上不会相同
- 由于 hashCode方法定义在 Object 类中， 因此每个对象都有一个默认的散列码，其值为 对象的存储地址。

## 2.3 toString 方法

- 用于返回表示对象值的字符串

## 2.4 API

#### java.lang.Object 1.0

- Class getClass( )
  返回包含对象信息的类对象。稍后会看到 Java 提供了类运行时的描述， 它的内容被封 装在 Class 类中。
- boolean equals(Object otherObject )
  比较两个对象是否相等， 如果两个对象指向同一块存储区域， 方法返回 true ; 否 则 方 法返回 false。在自定义的类中， 应该覆盖这个方法。
- String toString( )
  返冋描述该对象值的字符串。在自定义的类中， 应该覆盖这个方法。

#### java.lang.Class 1.0

- String getName( )
  返回这个类的名字。
- Class getSuperclass( )
  以 Class 对象的形式返回这个类的超类信息。

# 三 泛型数组列表:ArrayList (重点)

## 3.1 创建数组列表,添加元素

- 在 Java中， 解决动态调整数组大小的方法是使用 Java 中另外一个被称为 `ArrayList` 的类。它使用起来有点像数组，但在添加或删除元素时， 具有自动调节数组容量的 功能，而不需要为此编写任何代码。
- **ArrayList 是一个采用类型参数（type parameter) 的泛型类（generic class)**。为了指定数 组列表保存的元素对象类型，需要用一对尖括号将类名括起来加在后面
- 数组列表管理着对象引用的一个内部数组。最终， 数组的全部空间有可能被用尽。这就 显现出数组列表的操作魅力： 如果调用 add且内部数组已经满了，数组列表就将自动地创建 一个更大的数组，并将所有的对象从较小的数组中拷贝到较大的数组中。

```java
 public class Test {

    public static void main(String[] args) throws IOException {

        // 创建数组列表对象
        // 默认初始容量 : 10
        // private static final int DEFAULT_CAPACITY = 10;
        ArrayList<People> peopleList = new ArrayList<>();

        // 可以把初始容量传递给 ArrayList 构造器：
        //ArrayList<Employee> staff = new ArrayListo(lOO);

        // 改变数组列表的初始化容量,可以使用默认值.
        peopleList.ensureCapacity(20);

        //使用add 方法可以将元素添加到数组列表中。
        peopleList.add(new People("张三","24"));
        peopleList.add(new People("李四","26"));
        peopleList.add(new People("大白","4"));


        /**
         * 一旦能够确认数组列表的大小不再发生变化，就可以调用 trimToSize方法。
         * 这个方法将 存储区域的大小调整为当前元素数量所需要的存储空间数目。
         * 垃圾回收器将回收多余的存储 空间。 一旦整理了数组列表的大小，
         * 添加新元素就需要花时间再次移动存储块，所以应该在确 认不会添加任何元素时，
         * 再调用 trimToSize。
         */
        peopleList.trimToSize();

        // 返回数组列表中包含的实际元素数目。不是容量的大小.
        int size = peopleList.size();

        System.out.println(size);

        peopleList.forEach(item -> {
            System.out.println(item.toString()); // 必须重写toString方法
        });
    }
}

```

### java.util.ArrayList 1.2 API

```java
ArrayList( )
构造一个空数组列表。

ArrayList( int initialCapacity)
用指定容量构造一个空数组列表。 参数：initalCapacity 数组列表的最初容量 *

boolean add( E obj )
在数组列表的尾端添加一个元素。永远返回 true。 参数：obj 添加的元素

int size( )
返回存储在数组列表中的当前元素数量。（这个值将小于或等于数组列表的容量。 )
  
void ensureCapacity( int capacity)
确保数组列表在不重新分配存储空间的情况下就能够保存给定数量的元素。 参数：capacity 需要的存储容量

void trimToSize( )
将数组列表的存储容量削减到当前尺寸。  
```

## 3.2 访问数组列表元素

- 使用 get 和 set 方法实现访问或改变数组元素的操作，而不使用人们喜爱的 [ ]语法格式。

- 设置某个元素的值,1为元素的下标,参考数组.

  ```java
  peopleList.set(1, new People("小白","12") );
  
  ```

- 获取某个元素的值

  ```java
  People people = peopleList.get(1);
  
  ```

- 插入一个元素.

- 在下表为1的元素**之前**插入一个元素,之后的元素后移一位.

  ```java
  peopleList.add(1,new People("白","11"));
  
  ```

- 移除一个元素

- 移除下表为2的元素

  ```java
  peopleList.remove(2);
  
  ```

### API

```java
void set(int index，E obj)
设置数组列表指定位置的元素值， 这个操作将覆盖这个位置的原有内容。
参数：
index 位置（必须介于 0 ~ size()-l 之间）
obj 新的值


E get(int index)
获得指定位置的元素值。
参数： index 获得的元素位置（必须介于 0 ~ size()-l 之间）


void add(int index,E obj)
向后移动元素，以便插入元素。
参数： index 插入位置（必须介于 0 〜 size()-l 之间）
obj 新元素

E removednt index)
删除一个元素，并将后面的元素向前移动。被删除的元素由返回值返回。
参数：index 被删除的元素位置（必须介于 0 〜 size()-1之间）


```

# 四 对象包装器与自动装箱

> 有时， 需要将 int 这样的基本类型转换为对象。所有的基本类型都冇一个与之对应的类。 例如，Integer 类对应基本类型 int。通常， 这些类称为包装器 （ wrapper) 这些对象包装器类 拥有很明显的名字：Integer、Long、Float、Double、Short、Byte、Character、Void 和 Boolean (前 6 个类派生于公共的超类 Number)。对象包装器类是不可变的，即一旦构造了包装器，就不 允许更改包装在其中的值。同时， 对象包装器类还是 final, 因此不能定义它们的子类。

- 将一个int值赋给一个Integer对象时,会自动装箱.
- 当将一个 Integer 对象赋给一个 int 值时， 将会自动地拆箱
- == 运算符也可以应用于对象包装器对象， 只不过检测的是对象是 否指向同一个存储区域， 因此，下面的比较通常不会成立：

```java
Integer a = 1000; 
Integer b = 1000; 
if (a == b) ... 

```

- **自动装箱规范要求 boolean、byte、char <=127，介于 -128 ~ 127 之间的 short 和 int 被包装到固定的对象中。例如，如果在前面的例子中将 a 和 b 初始化为 100，对它们 进行比较的结果一定成立**。

```
        Integer a = 127;
        Integer b = 127;
        System.out.println(b == a); //true

        Integer c= 128;
        Integer d = 128;
        System.out.println(c == d); //false

```

# 五 参数数量可变的方法

用户自己也可以定义可变参数的方法， 并将参数指定为任意类型， 甚至是基本类型。下 面是一个简单的示例：其功能为计算若干个数值的最大值

```java
public class Test {

    public static void main(String[] args) throws IOException {
        int num1 = add(1,2,34);
        System.out.println(num1);

        int num2 = add(4,3,1,4,2,1,4,-9);
        System.out.println(num2);
    }

    public  static int add(int... value){
        int count = 0;
        for (int i: value){
            count += i;
        }
        return count;
    }
}


```

# 六 继承的设计技巧

## 6.1 将公共操作和域放在超类

这就是为什么将姓名域放在 Person类中，而没有将它放在 Employee 和 Student 类中的原因。

## 6.2 不要使用受保护的域

有些程序员认为，将大多数的实例域定义为 protected是一个不错的主意，只有这样，子 类才能够在需要的时候直接访问它们。然而， protected 机制并不能够带来更好的保护，其原因主要有两点:

1. 第一，子类集合是无限制的， 任何一个人都能够由某个类派生一个子类，并 编写代码以直接访问 protected 的实例域， 从而破坏了封装性。
2. 第二， 在 Java 程序设计语言 中，在同一个包中的所有类都可以访问 proteced 域，而不管它是否为这个类的子类。

## 6.3 使用继承实现“ is-a” 关系

## 6.4 除非所有继承的方法都有意义， 否则不要使用继承

## 6.5 在覆盖方法时， 不要改变预期的行为

## 6.6 使用多态， 而非类型信息

## 6.7 不要过多地使用反射

----

