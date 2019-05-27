Tomcat在各位JavaWeb从业者常常就是默认的开发环境，但是Tomcat的默认配置作为生产环境，尤其是内存和线程的配置，默认都很低，容易成为性能瓶颈.

幸好Tomcat还有很多的提升空间.下文介绍一下Tomcat优化,可以分为内存,线程,IO.

**一:Tomcat内存优化,启动时告诉JVM我要一块大内存(调优内存是最直接的方式)**

Windows 下的catalina.bat

Linux 下的catalina.sh 如:

```
JAVA_OPTS='-Xms256m -Xmx512m'
```

-Xms<size> JVM初始化堆的大小

-Xmx<size> JVM堆的最大值 实际参数大小根据服务器配置或者项目具体设置.

**二:Tomcat 线程优化** 在server.xml中 如:

```
<Connector port="80" protocol="HTTP/1.1" maxThreads="600" minSpareThreads="100" maxSpareThreads="500" acceptCount="700"
connectionTimeout="20000"  />
```

maxThreads="X" 表示最多同时处理X个连接

minSpareThreads="X" 初始化X个连接

maxSpareThreads="X" 表示如果最多可以有X个线程，一旦超过X个,则会关闭不在需要的线程

acceptCount="X" 当同时连接的人数达到maxThreads时,还可以排队,队列大小为X.超过X就不处理

**三:Tomcat IO优化**

1:同步阻塞IO（JAVA BIO） 同步并阻塞，服务器实现模式为一个连接一个线程(one connection one thread 想想都觉得恐怖,线程可是非常宝贵的资源)，当然可以通过线程池机制改善.

2:JAVA NIO:又分为同步非阻塞IO,异步阻塞IO 与BIO最大的区别one request one thread.可以复用同一个线程处理多个connection(多路复用).

3:,异步非阻塞IO(Java NIO2又叫AIO) 主要与NIO的区别主要是操作系统的底层区别.可以做个比喻:比作快递，NIO就是网购后要自己到官网查下快递是否已经到了(可能是多次)，然后自己去取快递；AIO就是快递员送货上门了(不用关注快递进度)。

BIO方式适用于连接数目比较小且固定的架构，这种方式对服务器资源要求比较高，并发局限于应用中，JDK1.4以前的唯一选择，但程序直观简单易理解.

NIO方式适用于连接数目多且连接比较短（轻操作）的架构，比如聊天服务器，并发局限于应用中，编程比较复杂，JDK1.4开始支持.

AIO方式使用于连接数目多且连接比较长（重操作）的架构，比如相册服务器，充分调用OS参与并发操作，编程比较复杂，JDK7开始支持.

在server.xml中

```
<Connector port="80" protocol="org.apache.coyote.http11.Http11NioProtocol" 
    connectionTimeout="20000" 
    URIEncoding="UTF-8" 
    useBodyEncodingForURI="true" 
    enableLookups="false" 
    redirectPort="8443" />
```

实现对Tomcat的IO切换.

**四:大杀器APR**

```
APR是从操作系统级别来解决异步的IO问题,大幅度的提高性能. (http://apr.apache.org/).
```

APR(Apache Portable Runtime)是一个高可移植库,它是Apache HTTP Server 2.x的核心.能更好地和其它本地web技术集成，总体上让Java更有效率作为一个高性能web服务器平台而不是简单作为后台容器.

在产品环境中，特别是直接使用Tomcat做WEB服务器的时候，应该使用Tomcat Native来提高其性能.如果不配APR，基本上300个线程狠快就会用满，以后的请求就只好等待.但是配上APR之后，并发的线程数量明显下降，从原来的300可能会马上下降到只有几十，新的请求会毫无阻塞的进来.

在局域网环境测，就算是400个并发，也是一瞬间就处理/传输完毕，但是在真实的Internet环境下，页面处理时间只占0.1%都不到，绝大部分时间都用来页面传输.如果不用APR，一个线程同一时间只能处理一个用户，势必会造成阻塞。所以生产环境下用apr是非常必要的.

安装Apache Tomcat Native Library，直接启动就支持apr(<http://tomcat.apache.org/native-doc/)它本身是基于APR的>. 具体安装方法可以参考其他博客和文章. 排除代码问题Tomcat优化到这个层次,可以应对大部分性能需求.