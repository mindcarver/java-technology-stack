## 1. Channel

```
Channel是Netty的核心概念之一，它是Netty网络通信的主体，由它负责同对端进行网络通信、注册和数据操作等功能。
```

### 1.1 工作原理

![image-20190419130202801](https://ws3.sinaimg.cn/large/006tNc79ly1g27ux6nh96j312y0f8jt6.jpg)

如上图所示：

- 一旦用户端连接成功，将新建一个channel同该用户端进行绑定
- channel从EventLoopGroup获得一个EventLoop，并注册到该EventLoop，channel生命周期内都和该EventLoop在一起（注册时获得selectionKey）
- channel同用户端进行网络连接、关闭和读写，生成相对应的event（改变selectinKey信息），触发eventloop调度线程进行执行
- 如果是读事件，执行线程调度pipeline来处理用户业务逻辑

### 1.2 状态转换

![image-20190419130229466](https://ws3.sinaimg.cn/large/006tNc79ly1g27uxmz8wwj30nw0i2q5c.jpg)

如上图所示，Channel包含注册、活跃、非活跃和非注册状态，在一般情况下是从注册->活跃->非活跃->非注册,但用户可以从eventloop取消和重注册channel，因此在此情况下活跃->非注册->注册

### 1.3 线程

```
多个channel可以注册到一个eventloop上，所有的操作都是顺序执行的，eventloop会依据channel的事件调用channel的方法进行相关操作，每个channel的操作和处理在eventloop中都是顺序的，如下图：
```

![image-20190419130258958](https://ws3.sinaimg.cn/large/006tNc79ly1g27uy57o5pj313009g0uh.jpg)

## 2. ChannelPipeline和ChannelHandler

```
ChannelPipeline和ChannelHandler用于channel事件的拦截和处理，Netty使用类似责任链的模式来设计ChannelPipeline和ChannelHandler

ChannelPipeline相当于ChannelHandler的容器，channel事件消息在ChannelPipeline中流动和传播，相应的事件能够被ChannelHandler拦截处理、传递、忽略或者终止，如下图所示：
```

![image-20190419130316650](https://ws4.sinaimg.cn/large/006tNc79ly1g27uyg00svj30wi0eydgp.jpg)

### 2.1 INBOUD和OUTBOUND事件

```
inbound:当发生某个I/O操作时由IO线程流向用户业务处理线程的事件，如链路建立、链路关闭或者读完成等
outbound:由用户线程或者代码发起的IO操作事件
```

### 2.2 ChannelHandlerContext

```
每个ChannelHandler 被添加到ChannelPipeline 后，都会创建一个ChannelHandlerContext 并与之创建的ChannelHandler 关联绑定。如下图：
```

![image-20190419130335566](https://ws2.sinaimg.cn/large/006tNc79ly1g27uys8zvfj31280gedmi.jpg)

```
ChannelHandler通过ChannelHandlerContext来操作channel和channelpipeline
```

### 2.3 ChannelHandler

```
ChannelHandler负责I/O事件或者I/O操作进行拦截和处理，用户可以通过ChannelHandlerAdapter来选择性的实现自己感兴趣的事件拦截和处理。
```

> 由于Channel只负责实际的I/O操作，因此数据的编解码和实际处理都需要通过ChannelHandler进行处理。

### 2.4 注意

> ChannelPipeline是线程安全的，多个业务线程可以并发的操作ChannelPipeline；ChannelHandler不是线程安全的，用户需要自己保重ChannelHandler的线程安全

## 3. ChannelFuture与ChannelPromise

```
在Netty中，所有的I/O操作都是异步的，因此调用一个I/O操作后，将继续当前线程的执行，但I/O操作的结果怎么获得？——ChannelFuture。
```

![image-20190419130358036](https://ws2.sinaimg.cn/large/006tNc79ly1g27uz68re3j30n20lmwfd.jpg)

> 如上图，当前线程A异步发起I/O操作后，不阻塞继续执行相关操作，当IO线程B完成后，通过回调执行A设置的回调方法。

```
回调方法通过监听的形式实现:ChannelFutureListener。
```

**ChannelPromise是ChannelFuture的扩展，允许设置I/O操作的结果，使ChannelFutureListener可以执行相关操作**