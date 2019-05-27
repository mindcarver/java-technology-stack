# Netty启动服务端源码分析

netty编写的服务端代码如下：源自官网

```java
 public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new DiscardServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            //绑定端口号并且接受连接
            ChannelFuture f = b.bind(port).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
```

我们可以大致对整个netty服务端启动流程做个简单概括：

1. 创建服务端channel
2. 初始化服务端channel
3. 注册selector
4. 服务端绑定

---

首先我们找到整个流程的入口：bind（port）方法

```java
// 我们可以看到返回的是一个ChannelFuture (解释上说的是创建一个新的Channel并且绑定它)
public ChannelFuture bind(int inetPort) {
    return bind(new InetSocketAddress(inetPort));
}
```

继续跟着bind方法走：

```java
 public ChannelFuture bind(SocketAddress localAddress) {
        validate();
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        return doBind(localAddress);
    }
```

可以看到核心方法就是dobind()，以后看到这种do开头的，基本就是我们要寻找的方法，继续跟入：

```java
private ChannelFuture doBind(final SocketAddress localAddress) {
    	// 初始化并注册Channel
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        if (regFuture.isDone()) {
            // At this point we know that the registration was complete and successful.
            ChannelPromise promise = channel.newPromise();
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // Registration future is almost always fulfilled already, but just in case it's not.
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    } else {
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        promise.registered();

                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }
```

那么整个服务端启动的代码就是在这一部分，接下来将会按之前分的4个步骤进行分析：

----

### 1.创建服务端channel

我们在上面的`dobind`方法中，首先就是看到的`initAndRegister`,创建服务端channel也就是从这里开始的，跟入代码：

```java
 final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            // channel工厂创建channel ----1
            channel = channelFactory.newChannel();
            // 初始化服务端channel ----2 作为第二部分讲解
            init(channel);
        } catch (Throwable t) {
            // 新通道可以为null，比如遇到异常SocketException(打开太多的文件)
            if (channel != null) {
            	// 当出现异常并且channel不为null的时候，立刻执行此方法关闭通道（从而不触发任何注册到通道上的事件）
                channel.unsafe().closeForcibly();
                // 由于该通道尚未注册，我们需要强制使用GlobalEventExecutor
                return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
            }
            // 由于该通道尚未注册，我们需要强制使用GlobalEventExecutor
            return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
        }
		
        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    i.e. It's safe to attempt bind() or connect() now:
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         because register(), bind(), and connect() are all bound to the same thread.

        return regFuture;
    }
```

进入到InitAndRegister方法之后，首先就是使用channelFactory创建newChannel，接下来跟入：

```java
public interface ChannelFactory<T extends Channel> {
	// 创建一个channel
    T newChannel();
}
```

这个接口是用来创建新的channel的，但是这个接口已经废除，我们接着根据IDEA的左边箭头提示，进入到`ReflectiveChannelFactory`,这是`通过反射调用默认构造函数`来创建channel的类

```java
public class ReflectiveChannelFactory<T extends Channel> implements ChannelFactory<T> {
    private final Class<? extends T> clazz;

    public ReflectiveChannelFactory(Class<? extends T> clazz) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        this.clazz = clazz;
    }
    @Override
    public T newChannel() {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class " + clazz, t);
        }
    }
}
```

我们可能会有一个疑惑，我们在bind方法的时候没有传入T啊，那这个T从哪里来的呢？其实回到之前服务端代码：

```java
b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
```

在创建`ServerBootstrap`的时候已经已经对channel的类型进行了设置，也就是`NioServerSocketChannel`,

所以我们最终创建出来的`channel`也就是`NioServerSocketChannel`,那么既然是通过反射调用构造函数来创建的，那么我们必须要知道`NioServerSocketChannel`在构造函数中做了哪些事情，这样也就明白了反射怎么创建的，我们进入到`NioServerSocketChannel`：

```java
// 无参构造函数
public NioServerSocketChannel() {
    this(newSocket(DEFAULT_SELECTOR_PROVIDER));
}
// 根据给定的ServerSocketChannel返回一个NioServerSocketChannel
public NioServerSocketChannel(ServerSocketChannel channel) {
    super(null, channel, SelectionKey.OP_ACCEPT);
    config = new NioServerSocketChannelConfig(this, javaChannel().socket());
}
```

上面无参构造函数会调用`this(newSocket(DEFAULT_SELECTOR_PROVIDER))`,我们需要明白`newSocket(DEFAULT_SELECTOR_PROVIDER)`是怎么返回一个`ServerSocketChannel`的，进入代码：

```java
private static ServerSocketChannel newSocket(SelectorProvider provider) {
    try {
        // 通过SelectorProvider提供一个SocketChannel
        return provider.openServerSocketChannel();
    } catch (IOException e) {
        throw new ChannelException(
            "Failed to open a server socket.", e);
    }
}
```

这里通过`newSocket`方法提返回`ServerSocketChannel`,通过selectorProvider.openServerSocketChannel创建一条服务端channel,之后我们还是回到上面的`NioServerSocketChannel`方法，里面一共做了两件事：

```java
super(null, channel, SelectionKey.OP_ACCEPT);----1
config = new NioServerSocketChannelConfig(this, javaChannel().socket());----2
```

先跟入到super方法中,注意我们传入的第二个参数是`ServerSocketChannel类型`,第三个参数表示`readInterestOp`,即是前面层层传入的 `SelectionKey.OP_ACCEPT`。

```java
protected AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
    super(parent, ch, readInterestOp);
}
protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent); -----1
        this.ch = ch;
        this.readInterestOp = readInterestOp;
        try {
            // 设置该channel为非阻塞模式
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                ch.close();
            } catch (IOException e2) {
                ....
            }
            ....
        }
    }
```

super方法主要就是设置`NioServerSocketChannel`的一些属性：主要体现在`AbstractChannel类`的此方法中：

```java
protected AbstractChannel(Channel parent) {
    	// 设置父Channel
        this.parent = parent;
    	// 设置ChannelId,channel的唯一标识
        id = newId();
    	// 用来表示不安全操作
        unsafe = newUnsafe();
    	// 设置ChannelPipeline
        pipeline = newChannelPipeline();
    }
```

接下来就是设置ServerSocketChannelConfig，其顶层接口是`channelConfig`,官方给出的解释：

> A set of configuration properties of a Channel.

config = new NioServerSocketChannelConfig(this, javaChannel().socket());----2，我们查看一下`javaChannel().socket()`,其实此方法就是通过jdk来创建`ServerSocket`对象 。关于`pipeline`，后面会有详细介绍。到此整个`channel`就创建完成了。总结：通过工厂反射创建`NioServerSocketChannel`,并在构造过程中初始化它的各种属性，包括`id`,`unsafe`,`pipline`等等。并且我们一共接触到以下几大组件，这也是我们后续需要详细了解的内容：

1. Channel 
2. ChannelConfig
3. Unsafe
4. Pipeline
5. ChannelId

接下来就是初始化`channel`.

-----

### 2.初始化服务端channel 

再次回到`initAndRegister`类的`initAndRegister()`方法，我们跟入到`init(channel)`方法中，打开`ServerbootStrap`里面的init方法：

```java
@Override
    void init(Channel channel) throws Exception {
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            setChannelOptions(channel, options, logger);
        }

        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }

        ChannelPipeline p = channel.pipeline();

        final EventLoopGroup currentChildGroup = childGroup;
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(0));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(0));
        }

        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(final Channel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }
```

这么一大长串的代码，我们切分分析，主要分为以下几个 步骤：

1. setChannelOptions，channelAttrs

   ```java
   final Map<ChannelOption<?>, Object> options = options0();
   synchronized (options) {
       setChannelOptions(channel, options, logger);
   }
   
   final Map<AttributeKey<?>, Object> attrs = attrs0();
   synchronized (attrs) {
       for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
           @SuppressWarnings("unchecked")
           AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
           channel.attr(key).set(e.getValue());
       }
   }
   ```

   这里我们可以看到channel设置了options0和attrs0，将其注入到channel中，关于channelOption和`Attributekey`将在后面分析。

2. set ChildOptions,ChildAttrs

   ```java
    ChannelPipeline p = channel.pipeline();
   
           final EventLoopGroup currentChildGroup = childGroup;
           final ChannelHandler currentChildHandler = childHandler;
           final Entry<ChannelOption<?>, Object>[] currentChildOptions;
           final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
           synchronized (childOptions) {
               currentChildOptions = childOptions.entrySet().toArray(newOptionArray(0));
           }
           synchronized (childAttrs) {
               currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(0));
           }
   ```

   这里主要做的就是设置新进来的channel的options和attrs。

3. config handler(配置服务pipeline)

   ```java
   p.addLast(new ChannelInitializer<Channel>() {
               @Override
               public void initChannel(final Channel ch) throws Exception {
                   final ChannelPipeline pipeline = ch.pipeline();
                   ChannelHandler handler = config.handler();
                   if (handler != null) {
                       pipeline.addLast(handler);
                   }
   
                   ch.eventLoop().execute(new Runnable() {
                       @Override
                       public void run() {
                           pipeline.addLast(new ServerBootstrapAcceptor(
                                   ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                       }
                   });
               }
           });
   ```

   这里主要配置pipline,先准备好channelconfig,channelhandler,以及之前的options以及attrs，然后配置到pipline中，最后`p.addLast`向pipline中加入了一个`ServerBootstrapAcceptor`,它主要用来接收新的请求并把请求转给某个事件循环器。到此初始化服务端channel就完成了。

   -----

### 3.register channel到某个对象中

还是回到`AbstractBootstrap`中，找到`initAndRegister`方法，

   ```java
ChannelFuture regFuture = config().group().register(channel);
   ```

我们跟入到 register 中，进入到EventLoopGroup的register方法，再选择SingleThreadEventLoop的register方法：

```java
@Override
public ChannelFuture register(final ChannelPromise promise) {
    ObjectUtil.checkNotNull(promise, "promise");
    promise.channel().unsafe().register(this, promise);
    return promise;
}
```

这里又出现了一个新的词`channelPromise`, 先不讲，我们这里的this 指的就是eventLoop,继续跟入到register方法：

```java
// 属于channel类
void register(EventLoop eventLoop, ChannelPromise promise);
// 属于AbstractChannel类
@Override
public final void register(EventLoop eventLoop, final ChannelPromise promise) {
    if (eventLoop == null) {
        throw new NullPointerException("eventLoop");
    }
    if (isRegistered()) {
        promise.setFailure(new IllegalStateException("registered to an event loop already"));
        return;
    }
    if (!isCompatible(eventLoop)) {
        promise.setFailure(
            new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
        return;
    }

    AbstractChannel.this.eventLoop = eventLoop;

    if (eventLoop.inEventLoop()) {
        register0(promise);
    } else {
        try {
            eventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    register0(promise);
                }
            });
        } catch (Throwable t) {
            logger.warn(
                "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                AbstractChannel.this, t);
            closeForcibly();
            closeFuture.setClosed();
            safeSetFailure(promise, t);
        }
    }
}
```





-------

整理下最后大致的流程：

> ```
> java.nio.channels.spi.SelectorProvider.provider()
> java.nio.channels.SocketChannel.open()
> io.netty.channel.socket.nio.NioSocketChannel.newSocket()
> io.netty.channel.socket.nio.NioSocketChannel.<init>()
> sun.reflect.GeneratedConstructorAccessor12.newInstance(Object[])
> sun.reflect.DelegatingConstructorAccessorImpl.newInstance(Object[])
> java.lang.reflect.Constructor.newInstance(Object[])
> java.lang.Class.newInstance()
> io.netty.bootstrap.AbstractBootstrap$BootstrapChannelFactory.newChannel()
> io.netty.bootstrap.AbstractBootstrap.initAndRegister()
> io.netty.bootstrap.Bootstrap.doConnect(SocketAddress, SocketAddress)
> io.netty.bootstrap.Bootstrap.connect(SocketAddress)
> ```

https://juejin.im/entry/5b0fb6e55188251553107468