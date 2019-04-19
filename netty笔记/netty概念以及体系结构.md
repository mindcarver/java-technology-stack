

[TOC]

# 一：netty的概念

## 定义

> Netty 是一款异步的事件驱动的网络应用程序框架，支持快速地开发可维护的高性能的面向协议的服务器和客户端。我们可以很简单的使用Netty 构建应用程序，你不必是一名网络编程专家；而且Netty 比直接使用底层的Java API 容易得多，它推崇良好的设计实践，可以将你的应用程序逻辑和网络层解耦。

## Netty的特性总结

在我们开始首次深入地了解Netty 之前，请仔细审视表1-1 中所总结的关键特性。有些是技术性的，而其他的更多的则是关于架构或设计哲学的。

![image-20190419104247528](https://ws4.sinaimg.cn/large/006tNc79ly1g27qwc6lokj31640jywu2.jpg)

## Java NIO

在深入netty之前，我们先来简单说说NIO；我们都知道，它和以前的普通I/O相比的最大优势在于它是非阻塞的。

### 阻塞I/O

因为普通I/O的阻塞，以前我们设计并发只能像下图这样，为每个I/O分配一个线程：

![image-20190419104410296](https://ws4.sinaimg.cn/large/006tNc79ly1g27qxpjjnjj30q00bi75i.jpg)

这显然带来了一些问题：

1. 在任何时候都可能有大量的线程处于休眠状态，只是等待输入或者输出数据就绪，这可能算是一种资源浪费。
2. 需要为每个线程的调用栈都分配内存，其默认值大小区间为64 KB 到1 MB，具体取决于操作系统。
3. 即使Java 虚拟机（JVM）在物理上可以支持非常大数量的线程，但是远在到达该极限之前，上下文切换所带来的开销就会带来麻烦，例如，在达到10 000 个连接的时候。

### 非阻塞I/O

Java 对于非阻塞I/O 的支持是在2002 年引入的，位于JDK 1.4 的java.nio 包中。下图展示了**一个非阻塞设计**，其实际上消除了普通I/O的那些弊端。**选择器使得我们能够通过较少的线程便可监视许多连接上的事件**。

![image-20190419104447093](https://ws3.sinaimg.cn/large/006tNc79ly1g27qyd1772j317p0u0tcj.jpg)

class java.nio.channels.Selector 是Java 的非阻塞I/O 实现的关键。它使用了事件通知API以确定在一组非阻塞套接字中有哪些已经就绪能够进行I/O 相关的操作。因为可以在任何的时间检查任意的读操作或者写操作的完成状态，所以如上图 所示，一个单一的线程便可以处理多个并发的连接。

总体来看，与阻塞I/O 模型相比，这种模型提供了更好的资源管理：

1. 使用较少的线程便可以处理许多连接，因此也减少了内存管理和上下文切换所带来开销；
2. 当没有I/O 操作需要处理的时候，线程也可以被用于其他任务。尽管已经有许多直接使用Java NIO API 的应用程序被构建了，但是要做到如此正确和安全并不容易。特别是，在高负载下可靠和高效地处理和调度I/O 操作是一项繁琐而且容易出错的任务，最好留给高性能的网络编程专家——Netty。

# 二：netty核心组件

在本节中我将要讨论Netty 的主要构件块：

1. Channel —— 可以看做是Socket的抽象；
2. 回调；
3. ChannelFuture—— 异步通知；
4. 事件和ChannelHandler。
5. EventLoop —— 控制流、多线程处理、并发；
6. ChannelPipeline —— 提供了ChannelHandler 链的容器
7. 引导 —— Bootstrap和ServerBootstrap

这些构建块代表了不同类型的构造：资源、逻辑以及通知。你的应用程序将使用它们来访问网络以及流经网络的数据。

对于每个组件来说，我们都将提供一个基本的定义，并且在适当的情况下，还会提供一个简单的示例代码来说明它的用法。

## Channel

基本的I/O 操作（bind()、connect()、read()和write()）依赖于底层网络传输所提供的原语。在基于Java 的网络编程中，其基本的构造是class Socket。Netty 的Channel 接口所提供的API，大大地降低了直接使用Socket 类的复杂性。

Channel 是Java NIO 的一个基本构造。它代表一个到实体（如一个硬件设备、一个文件、一个网络套接字或者一个能够执行一个或者多个不同的I/O操作的程序组件）的开放连接，如读操作和写操作。

目前，可以把Channel 看作是传入（入站）或者传出（出站）数据的载体。因此，它可以被打开或者被关闭，连接或者断开连接。

## 回调

一个回调其实就是一个方法，一个指向已经被提供给另外一个方法的方法的引用。这使得后者可以在适当的时候调用前者。回调在广泛的编程场景中都有应用，而且也是在操作完成后通知相关方最常见的方式之一。

## ChannelFuture

Future 提供了另一种在操作完成时通知应用程序的方式。这个对象可以看作是一个异步操作的结果的占位符；它将在未来的某个时刻完成，并提供对其结果的访问。

JDK 预置了interface java.util.concurrent.Future，但是其所提供的实现，**只允许手动检查对应的操作是否已经完成，或者一直阻塞直到它完成**。这是非常繁琐的，所以Netty提供了它自己的实现——**ChannelFuture**，用于在执行异步操作的时候使用。

ChannelFuture提供了几种额外的方法，这些方法使得我们能够注册一个或者多个ChannelFutureListener实例。监听器的回调方法operationComplete()，将会在对应的操作完成时被调用。然后监听器可以判断该操作是成功地完成了还是出错了。如果是后者，我们可以检索产生的Throwable。简而言之，由ChannelFutureListener提供的通知机制消除了手动检查对应的操作是否完成的必要。

下面展示了一个异步地连接到远程节点,ChannelFuture 作为一个I/O 操作的一部分返回的例子。这里，connect()方法将会直接返回，而不会阻塞。

```java
Channel channel = ...;
ChannelFuture future = channel.connect(
new InetSocketAddress("192.168.0.1", 25));
```

下面的代码显示了如何利用ChannelFutureListener。首先，要连接到远程节点上。然后，要注册一个新的ChannelFutureListener 到对connect()方法的调用所返回的ChannelFuture 上。当该监听器被通知连接已经建立的时候，要检查对应的状态。如果该操作是成功的，那么将数据写到该Channel。否则，要从ChannelFuture 中检索对应的Throwable。

```java
Channel channel = ...;
// 连接远程节点
ChannelFuture future = channel.connect(
    new InetSocketAddress("192.168.0.1", 25));
//注册一个ChannelFutureListener，以便在操作完成时获得通知
future.addListener(new ChannelFutureListener() {
    @Override
        public void operationComplete(ChannelFuture future) {
        //状态判断
        if (future.isSuccess()){
            //如果操作是成功的，则创建一个ByteBuf 以持有数据
            ByteBuf buffer = Unpooled.copiedBuffer(
                "Hello",Charset.defaultCharset());
            //将数据异步地发送到远程节点。返回一个ChannelFuture
            ChannelFuture wf = future.channel()
                .writeAndFlush(buffer);
            ....
        } else {
            //如果发生错误，则访问描述原因的Throwable
            Throwable cause = future.cause();
            cause.printStackTrace();
        }
    }
});
```

如果你把ChannelFutureListener 看作是回调的一个更加精细的版本，那么你是对的。事实上，回调和Future 是相互补充的机制；它们相互结合，构成了Netty 本身的关键构件块之一。

## 事件和ChannelHandler

### 事件

Netty 使用不同的事件来通知我们状态的改变或者是操作的状态。这使得我们能够基于已经发生的事件来触发适当的动作。这些动作可能是：

1. 记录日志；
2. 数据转换；
3. 流控制；
4. 应用程序逻辑。

Netty 是一个网络编程框架，所以事件是按照它们与入站或出站数据流的相关性进行分类的。可能由入站数据或者相关的状态更改而触发的事件包括：

1. 连接已被激活或者连接失活；
2. 数据读取；
3. 用户事件；
4. 错误事件。

出站事件是未来将会触发的某个动作的操作结果，这些动作包括：

1. 打开或者关闭到远程节点的连接；
2. 将数据写到或者冲刷到套接字。

### ChannelHandler

从应用程序开发人员的角度来看，Netty 的主要组件是ChannelHandler，它充当了所有处理入站和出站数据的应用程序逻辑的容器。该组件实现了服务器对从客户端接收的数据的处理。每个事件都可以被分发给ChannelHandler 类中的某个用户实现的方法。这是一个很好的将事件驱动范式直接转换为应用程序构件块的例子。图1-3 展示了一个事件是如何被一个这样的ChannelHandler 链处理的。

![image-20190419104654234](https://ws3.sinaimg.cn/large/006tNc79ly1g27r0k7wxnj31aa0biacb.jpg)

Netty 的ChannelHandler 为处理器提供了基本的抽象，如图1-3 所示的那些。**你可以认为每个ChannelHandler 的实例都类似于一种为了响应特定事件而被执行的回调**。
下列代码就是一个handler的示例：

```java
@ChannelHandler.Sharable
public class EchoClientHandler extends
        SimpleChannelInboundHandler<ByteBuf> {
    //重写了channelActive()方法，其将在一个连接建立时被调用
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!",
                CharsetUtil.UTF_8));
    }
    //重写了channelRead0()方法。每当接收数据时，都会调用这个方法。
    //需要注意的是，由服务器发送的消息可能会被分块接收。
    // 也就是说，如果服务器发送了5 字节，那么不能保证这5 字节会被一次性接收。
    //即使是对于这么少量的数据，channelRead0()方法也可能
    // 会被调用两次，第一次使用一个持有3 字节的ByteBuf（Netty 的字节容器）
    // 第二次使用一个持有2 字节的ByteBuf。
    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        System.out.println(
                "Client received: " + in.toString(CharsetUtil.UTF_8));
    }
    //发生异常时被调用
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

**channelHandler的主要抽象方法都定义于ChannelHandlerAdapter类中，我们通过重写适当的方法，来控制整个生命周期的重要节点的逻辑。**

Netty 提供了大量预定义的可以开箱即用的ChannelHandler 实现，包括用于各种协议（如HTTP 和SSL/TLS）的ChannelHandler。在内部，ChannelHandler 自己也使用了事件和Future，使得它们也成为了你的应用程序将使用的相同抽象的消费者。

## ChannelPipeline

ChannelPipeline 提供了ChannelHandler 链的容器，并定义了用于在该链上传播入站和出站事件流的API。当Channel 被创建时，它会被自动地分配到它专属的ChannelPipeline。

ChannelHandler 安装到ChannelPipeline 中的过程如下所示：

```java
    ServerBootstrap b = new ServerBootstrap();
    //一个ChannelInitializer的实现被注册到了ServerBootstrap中①；
    b.group(group)
        .channel(NioServerSocketChannel.class)
        .localAddress(new InetSocketAddress(port))
        .childHandler(new ChannelInitializer<SocketChannel>() {
            //当ChannelInitializer.initChannel()方法被调用时ChannelInitializer将在
            //ChannelPipeline 中安装一组自定义的ChannelHandler  serverHandler；
            @Override
            public void initChannel(SocketChannel ch)
                    throws Exception {
                ch.pipeline().addLast(serverHandler);
            }
        });
```

## EventLoop

EventLoop 定义了Netty 的核心抽象，用于处理连接的生命周期中所发生的事件。图3-1
下图在高层次上说明了Channel、EventLoop、Thread 以及EventLoopGroup 之间的关系。

![image-20190419104751813](https://ws3.sinaimg.cn/large/006tNc79ly1g27r1kea0nj312x0u0ajh.jpg)

这些关系是：

1. 一个EventLoopGroup 包含一个或者多个EventLoop；
2. 一个EventLoop 在它的生命周期内只和一个Thread 绑定；所有由EventLoop 处理的I/O 事件都将在它专有的Thread 上被处理；
3. 一个Channel 在它的生命周期内只注册于一个EventLoop；一个EventLoop 可能会被分配给一个或多个Channel。

注意，在这种设计中，一个给定Channel 的I/O 操作都是由相同的Thread 执行的，实际
上消除了不同线程间对于同步的需要。

## Bootstrap和ServerBootstrap

Netty 的引导类为应用程序的网络层配置提供了容器，这涉及将一个进程绑定到某个指定的端口（ServerBootstrap），或者将一个进程连接到另一个运行在某个指定主机的指定端口上的进程（Bootstrap）。Netty提供两种类型的引导，一种用于客户端（简单地称为Bootstrap），而另一种（ServerBootstrap）用于服务器。无论你的应用程序使用哪种协议或者处理哪种类型的数据，唯一决定它使用哪种引导类的是它是作为一个客户端还是作为一个服务器。表3-1 比较了这两种类型的引导类。

![image-20190419104824396](https://ws1.sinaimg.cn/large/006tNc79ly1g27r24d91wj30xs048go7.jpg)

这两种类型的引导类之间的第一个区别已经讨论过了：ServerBootstrap 将绑定到一个端口，因为服务器必须要监听连接，而Bootstrap 则是由想要连接到远程节点的客户端应用程序所使用的。

第二个区别可能更加明显。引导一个客户端只需要一个EventLoopGroup，但是一个ServerBootstrap 则需要两个（也可以是同一个实例）。为什么呢？

因为服务器需要两组不同的Channel。第一组将只包含一个ServerChannel，代表服务器自身的已绑定到某个本地端口的正在监听的套接字。而第二组将包含所有已创建的用来处理传入客户端连接（对于每个服务器已经接受的连接都有一个）的Channel。图3-4 说明了这个模型，并且展示了为何需要两个不同的EventLoopGroup。

![image-20190419104843654](https://ws2.sinaimg.cn/large/006tNc79ly1g27r2gjt2nj30s80du7at.jpg)

与ServerChannel 相关联的EventLoopGroup 将分配一个负责为 传入连接请求 创建Channel 的EventLoop。一旦连接被接受，第二个EventLoopGroup 就会给它的Channel分配一个EventLoop。

# 三：一个简单的Netty服务端和客户端交互demo

## 客户端

### 自定义channelHandler

```java
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

@ChannelHandler.Sharable
public class EchoClientHandler extends
        SimpleChannelInboundHandler<ByteBuf> {
    //重写了channelActive()方法，其将在一个连接建立时被调用
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!",
                CharsetUtil.UTF_8));
    }
    //重写了channelRead0()方法。每当接收数据时，都会调用这个方法。
    //需要注意的是，由服务器发送的消息可能会被分块接收。
    // 也就是说，如果服务器发送了5 字节，那么不能保证这5 字节会被一次性接收。
    //即使是对于这么少量的数据，channelRead0()方法也可能
    // 会被调用两次，第一次使用一个持有3 字节的ByteBuf（Netty 的字节容器）
    // 第二次使用一个持有2 字节的ByteBuf。
    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        System.out.println(
                "Client received: " + in.toString(CharsetUtil.UTF_8));
    }
    //发生异常时被调用
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

### 客户端实例

```java
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class EchoClient {
    private final String host;
    private final int port;
    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    public void start() throws Exception {
        //定义EventLoop
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //Bootstrap类包提供包含丰富API的帮助类，能够非常方便的实现典型的服务器端和客户端通道初始化功能。
            Bootstrap b = new Bootstrap();
            //绑定EventLoop
            b.group(group)
                    //使用默认的channelFactory创建一个channel
                    .channel(NioSocketChannel.class)
                    //定义远程地址
                    .remoteAddress(new InetSocketAddress(host, port))
                    //绑定自定义的EchoClientHandler到ChannelPipeline上
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new EchoClientHandler());
                        }
                    });
            //同步式的链接
            ChannelFuture f = b.connect().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
    public static void main(String[] args) throws Exception {
        new EchoClient("localhost", 8155).start();
    }
}


```

## 服务端

### 自定义channelHandler

```java
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

/**
 * 因为你的Echo 服务器会响应传入的消息，所以它需要实现ChannelInboundHandler 接口，用
 * 来定义响应入站事件的方法。
 */

//标示一个ChannelHandler 可以被多个Channel 安全地共享
@ChannelHandler.Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    //对于每个传入的消息都会被调用；
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //将消息记录到控制台
        ByteBuf in = (ByteBuf) msg;
        System.out.println(
                "Server received: " + in.toString(CharsetUtil.UTF_8));
        ctx.write(in);
    }
    //通知ChannelInboundHandler最后一次对channelRead()
    //的调用是当前批量读取中的最后一条消息；
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
    }
    //在读取操作期间，有异常抛出时会调用。
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

### 服务端实例

```java

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        //设置端口值（如果端口参数的格式不正确，则抛出一个NumberFormatException）
        int port = 8155;
        new EchoServer(port).start();
    }

    public void start() throws Exception {
        //定义EventLoop
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //与Bootstrap类包包含丰富的客户端API一样，ServerBootstrap能够非常方便的实现典型的服务端。
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    //指定所使用的NIO传输Channel
                    .channel(NioServerSocketChannel.class)
                    //使用指定的端口设置套接字地址
                    .localAddress(new InetSocketAddress(port))
                    //添加一个EchoServerHandler 到子Channel的ChannelPipeline
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    });
            //新建一个future实例,异步地绑定服务器；调用sync()方法阻塞等待直到绑定完成
            ChannelFuture f = b.bind().sync();
            //获取Channel 的CloseFuture，并且阻塞当前线程直到它完成
            //该应用程序将会阻塞等待直到服务器的Channel关闭（因为你在Channel 的CloseFuture 上调用了sync()方法）。
            f.channel().closeFuture().sync();
        } finally {
            //关闭EventLoopGroup，释放所有的资源
            group.shutdownGracefully().sync();
        }
    }
}

```

------

