# ByteBuffer

当我们进行数据传输的时候，往往需要使用到缓冲区，常用的缓冲区就是JDK NIO类库提供的java.nio.Buffer。

实际上，7种基础类型（Boolean除外）都有自己的缓冲区实现，对于NIO编程而言，我们主要使用的是ByteBuffer。从功能角度而言，ByteBuffer完全可以满足NIO编程的需要，但是由于NIO编程的复杂性，ByteBuffer也有其局限性，它的主要缺点如下:

1. ByteBuffer长度固定，一旦分配完成，它的容量不能动态扩展和收缩，当需要编码的POJO对象大于ByteBuffer的容量时，会发生索引越界异常；
2. ByteBuffer只有一个标识位置的指针position，读写的时候需要手工调用flip()和rewind()等，使用者必须小心谨慎地处理这些API，否则很容易导致程序处理失败；
3. ByteBuffer的API功能有限，一些高级和实用的特性它不支持，需要使用者自己编程实现。

# ByteBuf

为了弥补这些不足，Netty提供了自己的ByteBuffer实现——ByteBuf。

网络数据的基本单位总是字节。Java NIO 提供了ByteBuffer 作为它的字节容器，但是这个类使用起来过于复杂，而且也有些繁琐。

Netty 的ByteBuffer 替代品是ByteBuf，一个强大的实现，既解决了JDK API 的局限性，又为网络应用程序的开发者提供了更好的API。在本章中我们将会说明和JDK 的ByteBuffer 相比，ByteBuf 的卓越功能性和灵活性。这
也将有助于更好地理解Netty 数据处理的一般方式。

# 继承关系图

![image-20190419105251589](https://ws2.sinaimg.cn/large/006tNc79ly1g27r6re3hej31480q0n91.jpg)

# ByteBuf的优点

Netty 的数据处理API 通过两个组件暴露——abstract class ByteBuf 和interface ByteBufHolder。
下面是一些ByteBuf API 的优点：

1. 它可以被用户自定义的缓冲区类型扩展；
2. 通过内置的复合缓冲区类型实现了透明的零拷贝；
3. 容量可以按需增长（类似于JDK 的StringBuilder）；
4. 在读和写这两种模式之间切换不需要调用ByteBuffer 的flip()方法；
5. 读和写使用了不同的索引；
6. 支持方法的链式调用；
7. 支持引用计数；
8. 支持池化。

其他类可用于管理ByteBuf 实例的分配，以及执行各种针对于数据容器本身和它所持有的数据的操作。我们将在仔细研究ByteBuf 和ByteBufHolder 时探讨这些特性。

# ByteBuf动态扩容

通常情况下，当我们对ByteBuffer进行put操作的时候，如果缓冲区剩余可写空间不够，就会发生BufferOverflowException异常。为了避免发生这个问题，通常在进行put操作的时候会对剩余可用空间进行校验，如果剩余空间不足，需要重新创建一个新的ByteBuffer，并将之前的ByteBuffer复制到新创建的ByteBuffer中，最后释放老的ByteBuffer，代码示例如下。

```java
    public ByteBuffer put(ByteBuffer src) {

        if (src instanceof HeapByteBuffer) {
            if (src == this)
                throw new IllegalArgumentException();
            HeapByteBuffer sb = (HeapByteBuffer)src;
            int n = sb.remaining();
            if (n > remaining())
                throw new BufferOverflowException();
            System.arraycopy(sb.hb, sb.ix(sb.position()),
                             hb, ix(position()), n);
            sb.position(sb.position() + n);
            position(position() + n);
        } else if (src.isDirect()) {
            int n = src.remaining();
            if (n > remaining())
                throw new BufferOverflowException();
            src.get(hb, ix(position()), n);
            position(position() + n);
        } else {
            super.put(src);
        }
        return this;



    }
```

# ByteBuf的两种索引

因为所有的网络通信都涉及字节序列的移动，所以高效易用的数据结构明显是必不可少的。Netty 的ByteBuf 实现满足并超越了这些需求。让我们首先来看看它是如何通过使用不同的索引来简化对它所包含的数据的访问的吧。

ByteBuf 维护了两个不同的索引：一个用于读取，一个用于写入。当你从ByteBuf 读取时，它的readerIndex 将会被递增已经被读取的字节数。同样地，当你写入ByteBuf 时，它的writerIndex 也会被递增。图5-1 展示了一个空ByteBuf 的布局结构和状态（一个读索引和写索引都设置为0 的16 字节ByteBuf）。

![image-20190419105334878](https://ws1.sinaimg.cn/large/006tNc79ly1g27r7i5jubj31100b4wgs.jpg)

可以看到，正常情况下，一个ByteBuf被两个索引分成三部分。

![image-20190419105358857](https://ws1.sinaimg.cn/large/006tNc79ly1g27r7x6pdqj319y0g8n2p.jpg)

**readerIndex 达到和writerIndex 位于同一位置，表示我们到达"可以读取的"数据的末尾**。就如同试图读取超出数组末尾的数据一样，试图读取超出该点的数据将会触发一个IndexOutOfBoundsException。

名称以read 或者write 开头的ByteBuf 方法，将会推进其对应的索引，而名称以set 或者get 开头的操作则不会。后面的这些方法将在作为一个参数传入的一个相对索引上执行操作。

## ByteBuf的三种缓存区类型

和ByteBuffer 一样，ByteBuf也是一个缓存区类，它有三种缓存区类型：

### 堆缓存

最常用的ByteBuf 模式是将数据存储在JVM 的堆空间中，可以被jvm自动回收。这种模式被称为支撑数组（backing array），它能在没有使用池化的情况下提供快速的分配和释放。这种方式，如代码清单5-1 所示，非常适合于有遗留的数据需要处理的情况。

```java
    ByteBuf heapBuf = ...;
    //检查ByteBuf 是否有一个支撑数组
    if (heapBuf.hasArray()) {
        //如果有，则获取对该数组的引用
        byte[] array = heapBuf.array();
        //计算第一个字节的偏移量。
        int offset = heapBuf.arrayOffset() + heapBuf.readerIndex();
        //获得可读字节数
        int length = heapBuf.readableBytes();
        //使用数组、偏移量和长度作为参数调用你的方法
        handleArray(array, offset, length);
    }
```

当hasArray()方法返回false 时，尝试访问支撑数组将触发一个UnsupportedOperationException。这个模式类似于JDK 的ByteBuffer 的用法。

堆缓存区的缺点在于如果进行Socket的I/O读写，需要额外进行一次内存复制，将堆内存对应的缓冲区复制到内核的channel中，性能会有一定的下降。

### 直接缓存区

#### 直接缓存区和非直接缓存区的区别

我们先来了解一下什么是直接缓存区：
我们知道java的ByteBuffer类型就有直接和非直接缓存区这两种类型。

1. 非直接缓冲区：通过 ByteBuffer的allocate() 方法分配缓冲区，将缓冲区建立在 JVM 的内存中。

   ![image-20190419105543771](https://ws3.sinaimg.cn/large/006tNc79ly1g27r9qt5qdj311u0pmwnl.jpg)

2. 直接缓冲区：通过 ByteBuffer的allocateDirect() 方法分配直接缓冲区，将缓冲区建立在物理内存中，不再对其进行复制，可以提高效率。虽然直接缓冲区使JVM可以进行高效的I/o操作，但它使用的内存是操作系统分配的，绕过了JVM堆栈，建立和销毁比堆栈上的缓冲区要更大的开销。

   ![image-20190419105607948](https://ws1.sinaimg.cn/large/006tNc79ly1g27ra5y0o7j318q0u0487.jpg)

他们的区别如下：

1. 字节缓冲区要么是直接的，要么是非直接的。如果为直接字节缓冲区，则 Java 虚拟机会尽最大努力直接在此缓冲区上执行本机 I/O 操作。也就是说，在每次调用基础操作系统的一个本机 I/O 操作之前（或之后）
2. **直接缓冲区的内容可以驻留在常规的垃圾回收堆之外**，因此，它们对应用程序的内存需求量造成的影响可能并不明显。所以，建议将直接缓冲区主要分配给那些易受基础系统的本机 I/O 操作影响的大型、持久的缓冲区。一般情况下，最好仅在直接缓冲区能在程序性能方面带来明显好处时分配它们。

#### ByteBuf 直接缓存区的使用

我们直接看代码：

```java
    ByteBuf directBuf = ...;
    //检查ByteBuf 是否由数组支撑。如果不是，则这是一个直接缓冲区
    if (!directBuf.hasArray()) {
        //获取可读字节数
        int length = directBuf.readableBytes();
        //分配一个新的数组来保存具有该长度的字节数据
        byte[] array = new byte[length];
        //将字节复制到该数组
        directBuf.getBytes(directBuf.readerIndex(), array);
        //使用数组、偏移量和长度作为参数调用你的方法
        handleArray(array, 0, length);
    }
```

### 复合缓冲区

第三种也是最后一种模式使用的是复合缓冲区，它为多个ByteBuf 提供一个聚合视图。在这里你可以根据需要添加或者删除ByteBuf 实例，这是一个JDK 的ByteBuffer 没有的特性。

Netty 通过一个ByteBuf 子类——CompositeByteBuf——实现了这个模式，它提供了一个将多个缓冲区表示为单个合并缓冲区的虚拟表示。

为了举例说明，让我们考虑一下一个由两部分——头部和主体——组成的将通过HTTP 协议传输的消息。这两部分由应用程序的不同模块产生，将会在消息被发送的时候组装。该应用程序可以选择为多个消息重用相同的消息主体。当这种情况发生时，对于每个消息都将会创建一个新的头部。

因为我们不想为每个消息都重新分配这两个缓冲区，所以使用CompositeByteBuf 是一个完美的选择。它在消除了没必要的复制的同时，暴露了通用的ByteBuf API。图5-2 展示了生成的消息布局。

![image-20190419105641093](https://ws4.sinaimg.cn/large/006tNc79ly1g27raqrbwyj319k0dwmzo.jpg)

代码清单5-3 展示了如何通过使用JDK 的ByteBuffer 来实现这一需求。创建了一个包含两个ByteBuffer 的数组用来保存这些消息组件，同时创建了第三个ByteBuffer 用来保存所有这些数据的副本。

```java
    // Use an array to hold the message parts
    ByteBuffer[] message = new ByteBuffer[] { header, body };
    // Create a new ByteBuffer and use copy to merge the header and body
    ByteBuffer message2 =
    ByteBuffer.allocate(header.remaining() + body.remaining());
    message2.put(header);
    message2.put(body);
    message 2.flip();
```

分配和复制操作，以及伴随着对数组管理的需要，使得这个版本的实现效率低下而且笨拙。
代码清单5-4 展示了一个使用了CompositeByteBuf 的版本。

```java
    CompositeByteBuf messageBuf = Unpooled.compositeBuffer();
    //将ByteBuf 实例追加到CompositeByteBuf
    ByteBuf headerBuf = ...; // can be backing or direct
    ByteBuf bodyBuf = ...; // can be backing or direct
    messageBuf.addComponents(headerBuf, bodyBuf);
    .....
    //删除位于索引位置为 0（第一个组件）的ByteBuf
    messageBuf.removeComponent(0); // remove the header
    //循环遍历所有的ByteBuf 实例
    for (ByteBuf buf : messageBuf) {
        System.out.println(buf.toString());
    }
```

#### 复合缓存区的使用

CompositeByteBuf 可能不支持访问其支撑数组，因此访问CompositeByteBuf 中的数据类似于（访问）直接缓冲区的模式，如代码清单5-5 所示。

```java
CompositeByteBuf compBuf = Unpooled.compositeBuffer();
//获得可读字节数
int length = compBuf.readableBytes();
//分配一个具有可读字节数长度的新数组
byte[] array = new byte[length];
//将字节读到该数组中
compBuf.getBytes(compBuf.readerIndex(), array);
//使用偏移量和长度作为参数使用该数组
handleArray(array, 0, array.length);
```

### 总结

经验表明：ByteBuf的最佳实践是在I/O通信线程的读写缓冲区使用DirectByteBuf，后端业务消息的编解码模块使用HeapByteBuf

# 字节级操作

ByteBuf 提供了许多超出基本读、写操作的方法用于修改它的数据。在接下来的章节中，我们将会讨论这些中最重要的部分。

## 通过索引访问数据

如同在普通的Java 字节数组中一样，ByteBuf 的索引是从零开始的：第一个字节的索引是0，最后一个字节的索引总是capacity() - 1。代码清单5-6 表明，对存储机制的封装使得遍历ByteBuf 的内容非常简单。

```java
ByteBuf buffer = ...;
    for (int i = 0; i < buffer.capacity(); i++) {
        byte b = buffer.getByte(i);
        System.out.println((char)b);
    }
```

需要注意的是，使用那些需要一个索引值参数的方法来访问数据既不会改变readerIndex 也不会改变writerIndex。如果有需要，也可以通过调用readerIndex(index)或者writerIndex(index)来手动移动这两者。

## 通过数据反查索引

在ByteBuf中有多种可以用来确定指定值的索引的方法。最简单的是使用indexOf()方法。较复杂的查找可以通过那些需要一个ByteBufProcessor作为参数的方法达成。这个接口只定义了一个方法：

boolean process(byte value)

它将检查输入值是否是正在查找的值。

ByteBufProcessor针对一些常见的值定义了许多便利的枚举。假设你的应用程序需要和所谓的包含有以NULL结尾的内容的Flash套接字，可以调用:
forEach Byte(ByteBufProcessor.FIND_NUL)
如代码清单展示了一个查找回车符（r）的索引的例子。：



```
    ByteBuf buffer = ...;
    int index = buffer.forEachByte(ByteBufProcessor.FIND_CR);
```

## 常规读/写操作

正如我们所提到过的，有两种类别的读/写操作：

1. get()和set()操作，**从给定的索引开始，并且保持索引不变**；
2. read()和write()操作，**从给定的索引开始，并且会根据已经访问过的字节数对索引进行调整**。

### get()和set()操作

表5-1 列举了最常用的get()方法。完整列表请参考对应的API 文档。

![image-20190419105812244](https://ws1.sinaimg.cn/large/006tNc79ly1g27rcbr13wj317i0liauf.jpg)

这里面getBytes方法我们需要强调一下，比如buf.getBytes(buf.readerIndex(), array);表示将从buf实例的readerIndex为起点的数据传入指定的目的地（一个数组中）。

![image-20190419105827312](https://ws3.sinaimg.cn/large/006tNc79ly1g27rcky4sbj31800dqald.jpg)

### read()和write()操作

现在，让我们研究一下read()操作，其作用于当前的readerIndex 或writerIndex。这些方法将用于从ByteBuf 中读取数据，如同它是一个流。表5-3 展示了最常用的方法。

![image-20190419105848243](https://ws2.sinaimg.cn/large/006tNc79ly1g27rcy50arj31940rs7wh.jpg)

几乎每个read()方法都有对应的write()方法，用于将数据追加到ByteBuf 中。注意，表5-4 中所列出的这些方法的参数是需要写入的值，而不是索引值

![image-20190419105903042](https://ws3.sinaimg.cn/large/006tNc79ly1g27rd7nyacj317w0ii7rh.jpg)

## 删除已读字节

正如我们之前看过的这张图：

![image-20190419105918279](https://ws1.sinaimg.cn/large/006tNc79ly1g27rdgml0hj31ac0fygr5.jpg)

在上图中标记为可丢弃字节的分段包含了已经被读过的字节。通过调用discardReadBytes()方法，可以丢弃它们并回收空间。这个分段的初始大小为0，存储在readerIndex 中，会随着read 操作的执行而增加（get*操作不会移动readerIndex）。

上图展示了下图中所展示的缓冲区上调用discardReadBytes()方法后的结果。可以看到，可丢弃字节分段中的空间已经变为可写的了。注意，在调用discardReadBytes()之后，对可写分段的内容并没有任何的保证。

![image-20190419105937529](https://ws2.sinaimg.cn/large/006tNc79ly1g27rdsu9pwj31ac0fe78o.jpg)

虽然你可能会倾向于频繁地调用discardReadBytes()方法以确保可写分段的最大化，**但是请注意，这将极有可能会导致内存复制，因为可读字节（图中标记为CONTENT 的部分）必须被移动到缓冲区的开始位置**。我们建议只在有真正需要的时候才这样做，例如，当内存非常宝贵的时候。

## 读取可读字节

ByteBuf 的可读字节分段存储了实际数据。新分配的、包装的或者复制的缓冲区的默认的readerIndex 值为0。任何名称以read 或者skip 开头的操作都将检索或者跳过位于当前readerIndex 的数据，并且将它增加已读字节数。
以下代码清单展示了如何读取所有可以读的字节。

```java
    ByteBuf buffer = ...;
    while (buffer.isReadable()) {
        System.out.println(buffer.readByte());
    }
```

## 写数据

可写字节分段是指一个拥有未定义内容的、写入就绪的内存区域。新分配的缓冲区的writerIndex 的默认值为0。任何名称以write 开头的操作都将从当前的writerIndex 处开始写数据，并将它增加已经写入的字节数。如果尝试往目标写入超过目标容量的数据，将会引发一个IndexOutOfBoundException。

以下代码清单是一个用随机整数值填充缓冲区，直到它空间不足为止的例子。writeableBytes()方法在这里被用来确定该缓冲区中是否还有足够的空间。

```java
    // Fills the writable bytes of a buffer with random integers.
    ByteBuf buffer = ...;
    //因为一个int为四个字节
    while (buffer.writableBytes() >= 4) {
        buffer.writeInt(random.nextInt());
    }
```

## 手动设置索引

JDK 的InputStream 定义了mark(int readlimit)和reset()方法，这些方法分别被用来将流中的当前位置标记为指定的值，以及将流重置到该位置。

同样，可以通过调用markReaderIndex()、markWriterIndex()、resetWriterIndex()和resetReaderIndex()来标记和重置ByteBuf 的readerIndex 和writerIndex。这些和InputStream 上的调用类似，只是没有readlimit 参数来指定标记什么时候失效。

也可以通过调用readerIndex(int)或者writerIndex(int)来将索引移动到指定位置。试图将任何一个索引设置到一个无效的位置都将导致一个IndexOutOfBoundsException。可以通过调用clear()方法来将readerIndex 和writerIndex 都设置为0。注意，这并不会清除内存中的内容。

![image-20190419110017005](https://ws4.sinaimg.cn/large/006tNc79ly1g27reh5u2bj319w0iyn1h.jpg)

**调用clear()比调用discardReadBytes()轻量得多，因为它将只是重置索引而不会复制任何的内存**。

## 复制指向缓存区的指针

派生缓冲区为ByteBuf 提供了以专门的方式来呈现该ByteBuf内容的视图。这类视图可以通过以下方法被创建的：

1. duplicate()；
2. slice()；获取调用者的子缓冲区，且与原缓冲区共享缓冲区
3. slice(int, int)；获取调用者的子缓冲区，且与原缓冲区共享缓冲区
4. Unpooled.unmodifiableBuffer(…)；
5. order(ByteOrder)；
6. readSlice(int)。

**每个这些方法都将返回一个新的ByteBuf 实例，它具有自己的读索引、写索引和标记索引。**其内部存储和JDK 的ByteBuffer一样也是共享的。这使得派生缓冲区的创建成本是很低廉的，**但是这也意味着，如果你修改了它的内容，也同时修改了其对应的源实例，所以要小心**。

```java
Charset utf8 = Charset.forName("UTF-8");
//创建一个ByteBuf  "Netty in Action"
ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);

//创建该ByteBuf 从索引0 开始到索引15结束的一个新切片
ByteBuf sliced = buf.slice(0, 15);
System.out.println(sliced.toString(utf8));
//更新索引0 处的字节
buf.setByte(0, (byte)'J');
//将会成功，因为数据是共享的，对其中一个所做的更改对另外一个也是可见的
assert buf.getByte(0) == sliced.getByte(0);
```
## 复制缓存区的内容

如果需要一个现有缓冲区的真实副本，请使用copy()或者copy(int, int)方法。不同于派生缓冲区，由这个调用所返回的ByteBuf 拥有独立的数据副本。

```java
Charset utf8 = Charset.forName("UTF-8");
ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
ByteBuf copy = buf.copy(0, 15);
System.out.println(copy.toString(utf8));
buf.setByte(0, (byte) 'J');
//将会成功，因为数据不是共享的
assert buf.getByte(0) != copy.getByte(0);
```

如果我们不修改原始ByteBuf 的切片或者副本，这两种场景是相同的。只要有可能，我们尽量使用slice()方法来避免复制内存的开销。

# ByteBufHolder 接口

我们经常发现，除了实际的数据负载之外，我们还需要存储各种属性值。HTTP 响应便是一个很好的例子，除了表示为字节的内容，还包括状态码、cookie 等。为了处理这种常见的用例，Netty 提供了ByteBufHolder,它主要就是封装了一个ByteBuf对象，以及对这个对象的一些操作api。现在假如我们要构造一个HTTP响应的对象，那么就可以在继承ByteBufHolder的基础上在拓展其他的比如状态码、cookie等字段，达到自己的目的。

它常用的API如下：

![image-20190419110159083](https://ws4.sinaimg.cn/large/006tNc79ly1g27rg8ujosj30x4058q7i.jpg)

# ByteBuf分配机制

在这一节中，我们将描述管理ByteBuf 实例的不同方式。

## 按需分配：ByteBufAllocator 接口

为了降低分配和释放内存的开销，Netty 通过interface ByteBufAllocator 实现了（ByteBuf 的）**池化**，它可以用来分配我们所描述过的任意类型的ByteBuf 实例。

![image-20190419110232945](https://ws2.sinaimg.cn/large/006tNc79ly1g27rgutrdgj30vg0fcaoh.jpg)

关于ioBuffer，默认地，当所运行的环境具有sun.misc.Unsafe 支持时，返回基于直接内存存储的ByteBuf，否则返回基于堆内存存储的ByteBuf；当指定使用PreferHeapByteBufAllocator 时，则只会返回基于堆内存存储的ByteBuf。

我们可以通过Channel（每个都可以有一个不同的ByteBufAllocator 实例）或者绑定到ChannelHandler 的ChannelHandlerContext 获取一个到ByteBufAllocator 的引用。代码清单5-14 说明了这两种方法。

获取一个到ByteBufAllocator 的引用：

```java
//从Channel 获取一个到ByteBufAllocator 的引用
Channel channel = ...;
ByteBufAllocator allocator = channel.alloc();
....
//从ChannelHandlerContext 获取一个到ByteBufAllocator 的引用
ChannelHandlerContext ctx = ...;
ByteBufAllocator allocator2 = ctx.alloc();
...
```

Netty提供了两种ByteBufAllocator的实现：PooledByteBufAllocator和UnpooledByteBufAllocator。前者池化了ByteBuf的实例以提高性能并最大限度地减少内存碎片。此实现使用了一种称为jemalloc的已被大量现代操作系统所采用的高效方法来分配内存。后者的实现不池化ByteBuf实例，并且在每次它被调用时都会返回一个新的实例。

Netty默认使用了PooledByteBufAllocator

## Unpooled 缓冲区

可能某些情况下，你未能获取一个到ByteBufAllocator 的引用。对于这种情况，Netty 提供了一个简单的称为Unpooled 的工具类，它提供了静态的辅助方法来创建未池化的ByteBuf实例。表5-8 列举了这些中最重要的方法。![image-20190419110305901](https://ws4.sinaimg.cn/large/006tNc79ly1g27rheos3bj310m0cudo5.jpg)

Unpooled 类还使得ByteBuf 同样可用于那些并不需要Netty 的其他组件的非网络项目，使得其能得益于高性能的可扩展的缓冲区API。

# ByteBufUtil 类

ByteBufUtil 提供了用于操作ByteBuf 的静态的辅助方法。因为这个API 是通用的，并且和池化无关，所以这些方法已然在分配类的外部实现。

这些静态方法中最有价值的可能就是hexdump()方法，它以十六进制的表示形式打印ByteBuf 的内容。这在各种情况下都很有用，例如，出于调试的目的记录ByteBuf 的内容。十六进制的表示通常会提供一个比字节值的直接表示形式更加有用的日志条目，此外，十六进制的版本还可以很容易地转换回实际的字节表示。

另一个有用的方法是boolean equals(ByteBuf, ByteBuf)，它被用来判断两个ByteBuf实例的相等性。如果你实现自己的ByteBuf 子类，你可能会发现ByteBufUtil 的其他有用方法。

# 引用计数

引用计数是一种通过在某个对象所持有的资源不再被其他对象引用时释放该对象所持有的资源来优化内存使用和性能的技术。Netty 在第4 版中为ByteBuf 和ByteBufHolder 引入了引用计数技术，它们都实现了interface ReferenceCounted。

引用计数背后的想法并不是特别的复杂；它主要涉及跟踪到某个特定对象的活动引用的数量。一个ReferenceCounted 实现的实例将通常以活动的引用计数为1 作为开始。只要引用计数大于0，就能保证对象不会被释放。当活动引用的数量减少到0 时，该实例就会被释放。注意，虽然释放的确切语义可能是特定于实现的，但是至少已经释放的对象应该不可再用了。

引用计数对于池化实现（如PooledByteBufAllocator）来说是至关重要的，它降低了内存分配的开销。代码清单5-15 展示了相关的示例。

```java
//从Channel 获取ByteBufAllocator
Channel channel = ...;
ByteBufAllocator allocator = channel.alloc();
....
//从ByteBufAllocator分配一个ByteBuf
ByteBuf buffer = allocator.directBuffer();
//检查引用计数是否为预期的1
assert buffer.refCnt() == 1;
...
//减少到该对象的活动引用。当减少到0 时，该对象被释放，并且该方法返回true
ByteBuf buffer = ...;
boolean released = buffer.release();
```

试图访问一个已经被释放的引用计数的对象，将会导致一个IllegalReferenceCountException。

注意，一个特定的（ReferenceCounted 的实现）类，可以用它自己的独特方式来定义它的引用计数规则。例如，我们可以设想一个类，其release()方法的实现总是将引用计数设为零，而不用关心它的当前值，从而一次性地使所有的活动引用都失效。

**谁负责释放release呢** 一般来说，是由最后访问（引用计数）对象的那一方来负责将它释放。