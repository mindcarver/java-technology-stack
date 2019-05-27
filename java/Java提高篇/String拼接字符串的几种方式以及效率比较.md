# String 拼接字符串，你用对了吗？

在日常开发中，我们不可避免的会用到字符串的拼接，可能不同的人会用到不同的方式，接下来就讲讲常见的一些。



## Stringbuilder 实现字符串拼接

<blockquote style=" border-left-color:yellow;color:white;background-color:black;width:30%">基本使用
</blockquote>

```java
 StringBuilder sb = new StringBuilder();
        sb.append("微信公众号:codecarver")
                .append("qq交流群:964790653")
                .append("关注微信公众号领取2000G互联网架构师资源");
        System.out.println(sb.toString());
```

<blockquote style=" border-left-color:yellow;color:white;background-color:black;width:30%">基本原理
</blockquote>

我们跟Stringbuilder进入源码部分：

```java
public StringBuilder() {
    super(16);
}
AbstractStringBuilder(int capacity) {
    value = new char[capacity];
}
```

我们可以发现StringBuilder是继承自AbstractStringBuilder的，而其内部封装的则是一个字符数组，当我们new一个StringBuilder的时候则会初始化字符数组的大小为16：

```java
// 这是StringBuilder维护的字符数组变量
char[] value;
// 表示字符数组已经使用的字符个数
int count;
```

再次跟入到append源码中：

```java
@Override
public StringBuilder append(String str) {
    super.append(str);
    return this;
}
public AbstractStringBuilder append(String str) {
    if (str == null)
        return appendNull();
    int len = str.length();
    ensureCapacityInternal(count + len);
    str.getChars(0, len, value, count);
    count += len;
    return this;
}
```

`append`方法主要做了如下几件事：

1. 判断`str`是否为 null，如果为 null  ，那么则会以'null'的形式展现

   ```java
   // 代码测试
   StringBuilder sb = new StringBuilder();
   String s;
   s = null;
   System.out.println(sb.append("Value: ").append(s));
   
   // 判断为null的源码
   private AbstractStringBuilder appendNull() {
       int c = count;
       ensureCapacityInternal(c + 4);
       final char[] value = this.value;
       value[c++] = 'n';
       value[c++] = 'u';
       value[c++] = 'l';
       value[c++] = 'l';
       count = c;
       return this;
   }
   
   ```

   <blockquote style=" border-left-color:red;color:white;background-color:black;width:30%">打印结果：<br>Value: null
   </blockquote>

2. 确保字符数组的长度足够，如果不够会进行扩展

3. 将字符拷贝到字符数组中

4. 更改已经使用的字符数count（count+length）

-----

## StringBuffer实现字符串拼接

<blockquote style=" border-left-color:yellow;color:white;background-color:black;width:30%">StringBuffer基本实现与原理
</blockquote>

```java
StringBuffer sb = new StringBuffer();
String s;
s = null;
System.out.println(sb.append("Value: ").append(s));
```

其实 `StringBuffer` 和 `StringBuilder` 类似，最大的区别也就是我们熟知 `StringBuffer` 是属于线程安全的类，主要来看一下 `append` 方法。

```java
@Override
public synchronized StringBuffer append(String str) {
    toStringCache = null;
    super.append(str);
    return this;
}
public AbstractStringBuilder append(String str) {
    if (str == null)
        return appendNull();
    int len = str.length();
    ensureCapacityInternal(count + len);
    str.getChars(0, len, value, count);
    count += len;
    return this;
}
```

可以注意到`append`方法通过 `synchronized` 来修饰，表明是同步方法，接下来的调用还是跟 `StringBuilder`是一样的。

---

## 通过 ‘+’  来进行字符串的连接

<blockquote style=" border-left-color:yellow;color:white;background-color:black;width:30%">'+'实现字符串连接的原理
</blockquote>

```java
String qq = "964790653";
String msg = "欢迎小伙伴加入群聊一起交流java";
String str = qq + "-" + msg;

```

接下来我们需要对这部分代码进行反编译，可以使用IDEA的插件jclasslib来看：

```java
NEW java/lang/StringBuilder
    DUP
    INVOKESPECIAL java/lang/StringBuilder.<init> ()V
    ALOAD 1
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    LDC "-"
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    ALOAD 2
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
    ASTORE 3
```

我们不难发现，字符串在拼接的过程中，`new`了一个`StringBuilder`来做`append`，原理就是使用了`Stringbuilder`

-----

## String.contact方式

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">contact实现字符串连接的原理
</blockquote>

```java
String qq = "964790653";
String msg = "欢迎小伙伴加入群聊一起交流java";
String str = qq.concat("-").concat(msg);
System.out.println(str);
```

进入到 `concat` 源码中去：

```java
public String concat(String str) {
    int otherLen = str.length();
    if (otherLen == 0) {
        return this;
    }
    int len = value.length;
    char buf[] = Arrays.copyOf(value, len + otherLen);
    str.getChars(buf, len);
    return new String(buf, true);
}
```

不难发现首先对传过来的字符串进行长度判断，如果等于0，直接返回之前的字符串，如果不等于0，先创建字符数组，长度为原先字符串长度加要连接的字符串长度，之后将数据拷贝到字符数组中，最后根据字符数组new出一个新的String。

----

## StringUtils.join

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">apache.commons.lang3.StringUtils.join
</blockquote>

```java
String qq = "964790653";
String msg = "欢迎小伙伴加入群聊一起交流java";
String str = StringUtils.join(qq, "-", msg);
System.out.println(str);
```

跟入到join源码中：

```java
public static <T> String join(final T... elements) {
    return join(elements, null);
}

public static String join(final Object[] array, final String separator) {
    if (array == null) {
        return null;
    }
    return join(array, separator, 0, array.length);
}

public static String join(final Object[] array, String separator, final int startIndex, final int endIndex) {
    if (array == null) {
        return null;
    }
    if (separator == null) {
        separator = EMPTY;
    }
    final int noOfItems = endIndex - startIndex;
    if (noOfItems <= 0) {
        return EMPTY;
    }

    // 核心实现
    final StringBuilder buf = new StringBuilder(noOfItems * 16);

    for (int i = startIndex; i < endIndex; i++) {
        if (i > startIndex) {
            buf.append(separator);
        }
        if (array[i] != null) {
            buf.append(array[i]);
        }
    }
    return buf.toString();
}

```

不难发现，最后还是会采用StringBuilder进行拼接字符串。

----

## Guava里面的joiner（待分析源碼）

```java
Joiner joiner = Joiner.on("; ").skipNulls();
String str = joiner.join("微信公众号：", null, "codecarver");
System.out.println(str);
```

---

## 效率比较

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">1:+
</blockquote>

```java
public class Test {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            list.add(String.valueOf(i));
        }

        String ss = "";
        long startTime = System.currentTimeMillis();
        for (String s : list) {
            ss += s;
        }
        System.out.println(System.currentTimeMillis() - startTime);
    }
}
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">10507
</blockquote>

-----



<blockquote style=" border-left-color:yellow;color:white;background-color:black;">2:StringBuilder
</blockquote>

```java
StringBuilder sb = new StringBuilder();
long startTime = System.currentTimeMillis();
for (String s : list) {
    sb.append(s);
}
System.out.println(System.currentTimeMillis() - startTime);
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">13
</blockquote>

----

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">3:StringBuffer
</blockquote>

```java
StringBuffer ss = new StringBuffer();
long startTime = System.currentTimeMillis();
for (String s : list) {
    ss.append(s);
}
System.out.println(System.currentTimeMillis() - startTime);
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">16
</blockquote>

---

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">4:conact
</blockquote>

```java
String ss = "";
long startTime = System.currentTimeMillis();
for (String s : list) {
    ss=ss.concat(s);
}
System.out.println(System.currentTimeMillis() - startTime);
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">7362
</blockquote>

---

<blockquote style=" border-left-color:yellow;color:white;background-color:black;">5:Stringutils.join
</blockquote>

```java
long startTime = System.currentTimeMillis();
StringUtils.join(list);
System.out.println(System.currentTimeMillis() - startTime);
```

<blockquote style=" border-left-color:red;color:white;background-color:black;">1014
</blockquote>

----

我们从以上的测试代码中得到如下结论：StringBuilder < StringBuffer < StringUtils.join <  conact <  +,

不难发现StringBuilder的效率和StringBuffer的效率比较明显，但是为什么+ 的效率这么差呢？我们之前不是分析过+的底层也是StringBuilder吗，为什么相差这么大？再次反编译代码去看：

```java
??
```

----

## 总结

