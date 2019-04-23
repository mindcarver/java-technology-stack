## 1.什么是RPC
  RPC（Remote Procedure Call,远程过程调用），一般用来实现部署在不同机器上的系统之间的方法调用，使得程序能够像访问本地系统资源一样，通过网络传输去访问远端系统资源；对于客户端来说， 传输层使用什么协议，序列化、反序列化都是透明的。 
## 2. 了解 JAVA RMI
  RMI 全称是 remote method invocation – 远程方法调用，一种用于远程过程调用的应用程序编程接口，是纯 java 的网络分布式应用系统的核心解决方案之一。RMI 目前使用 Java 远程消息交换协议 JRMP（Java Remote Messageing Protocol）进行通信，由于 JRMP 是专为 Java对象制定的，是分布式应用系统的百分之百纯 java 解决方案,用 Java RMI 开发的应用系统可以部署在任何支持 JRE的平台上，缺点是，由于 JRMP 是专门为 java 对象指定的，因此 RMI 对于非 JAVA 语言开发的应用系统的支持不足，不能与非 JAVA 语言书写的对象进行通信。

## 3.RMI的基本框架
从设计角度上讲，JAVA采用的是三层结构模式来实现RMI。在整个体系结构中，有如下几个关键角色构成了通信双方：
  1. 客户端

     - 桩(StubObject)：远程对象在客户端上的代理；
     - 远程引用层(RemoteReference Layer)：解析并执行远程引用协议；
     -  传输层(Transport)：发送调用、传递远程方法参数、接收远程方法执行结果。

  2. 服务端

     - 骨架(Skeleton)：读取客户端传递的方法参数，调用服务器方的实际对象方法，并接收方法执行后的返回值；
     - 远程引用层(Remote ReferenceLayer)：处理远程引用语法之后向骨架发送远程方法调用；
     - 传输层(Transport)：监听客户端的入站连接，接收并转发调用到远程引用层。

  3. 注册表(Registry)：以URL形式注册远程对象，并向客户端回复对远程对象的引用。

     ![QQ图片20180926195710](https://ws4.sinaimg.cn/large/006tNc79ly1g28zeexpajj30dr0c3t92.jpg)

  **在实际的应用中，客户端并没有真正的和服务端直接对话来进行远程调用，而是通过本地JVM环境下的*桩对象*来进行的。**远程调用过程：
        1）客户端从远程服务器的注册表中查询并获取远程对象引用。当进行远程调用时，客户端首先会与桩对象(Stub Object)进行对话，而这个桩对象将远程方法所需的参数进行序列化后，传递给它下层的远程引用层(RRL)；
        
        2）桩对象与远程对象具有相同的接口和方法列表，当客户端调用远程对象时，实际上是由相应的桩对象代理完成的。远程引用层在将桩的本地引用转换为服务器上对象的远程引用后，再将调用传递给传输层(Transport)，由传输层通过TCP协议发送调用；
​        
        3）在服务器端，传输层监听入站连接，它一旦接收到客户端远程调用后，就将这个引用转发给其上层的远程引用层；
        
        4）服务器端的远程引用层将客户端发送的远程应用转换为本地虚拟机的引用后，再将请求传递给骨架(Skeleton)；
        
        5）骨架读取参数，又将请求传递给服务器，最后由服务器进行实际的方法调用。结果返回过程：
        	a:如果远程方法调用后有返回值，则服务器将这些结果又沿着“骨架->远程引用层->传输层”向下传递；
        
      	 	b:客户端的传输层接收到返回值后，又沿着“传输层->远程引用层->桩”向上传递，然后由桩来反序列化这些返回值，并将最终的结果传递给客户端程序。

![QQ图片20180926200444](https://ws3.sinaimg.cn/large/006tNc79ly1g28zepezrxj30ot0ex0we.jpg)

  从技术的角度上讲，有如下几个主要类或接口扮演着上述三层模型中的关键角色：
  1. 注册表：java.rmi.**Naming**和java.rmi.**Registry**；
  2. 骨架：java.rmi.remote.**Skeleton**     
  3. 桩：java.rmi.server.**RemoteStub**
  4. 远程引用层：java.rmi.server.**RemoteRef**和sun.rmi.transport.**Endpoint**；
  5. 传输层：sun.rmi.transport.**Transport**
![RegistryImpl](https://ws1.sinaimg.cn/large/006tNc79ly1g28zeyq85lj30s50nfaay.jpg)
![UnicastServerRef](https://ws2.sinaimg.cn/large/006tNc79ly1g28zf3a3ehj30nj0wo3zp.jpg)

## 4.RMI开发流程
作为一般的RMI应用，JAVA为我们隐藏了其中的处理细节，而让开发者有更多的精力和时间花在实际的应用中。开发RMI的步骤如下所述： 
1.服务端：
        1）定义Remote子接口，在其内部定义要发布的远程方法，并且这些方法都要Throws RemoteException；
        2）定义远程对象的实现类
              a. 继承UnicastRemoteObject或Activatable，并同时实现Remote子接口； 

  2. 客户端
           1）**定义用于接收远程对象的Remote子接口，只需实现java.rmi.Remote接口即可。但要求必须与服务器端对等的Remote子接口保持一致，即有相同的接口名称、包路径和方法列表等。**如果接口不一致，那么会发生代理对象转换异常错误：java.lang.ClassCastException: com.sun.proxy.$Proxy0 cannot be cast to com.gupaoedu.rmi.IHelloService at com.gupaoedu.rmi.ClientDemo.main(ClientDemo.java:19)     

          2）通过符合JRMP规范的URL字符串在注册表中获取并强转成Remote子接口对象；
          3）调用这个Remote子接口对象中的某个方法就是为一次远程方法调用行为。


## 5. 基于JAVA RMI实践
  服务端：
  ```java
  /*远程对象接口*/
public interface IHelloService extends Remote {

    /*发布远程对象*/
    String sayHello(String msg) throws RemoteException;
}
  ```
   ```java
  /*远程对象实现类*/
public class HelloServiceImpl extends UnicastRemoteObject implements IHelloService{

    protected HelloServiceImpl() throws RemoteException {
       // super();
    }

    @Override
    public String sayHello(String msg) throws RemoteException{
        return "Hello,"+msg;
    }
}
   ```
  ```java
  public class Server {

    public static void main(String[] args) {
        try {
            //发布一个远程对象
            IHelloService helloService=new HelloServiceImpl();
            /*
             *  在服务器的指定端口(默认为1099)上启动RMI注册表。
             *  必不可缺的一步，缺少注册表创建，则无法绑定对象到远程注册表上。
             */
            LocateRegistry.createRegistry(1099);

            /*
            * 将指定的远程对象绑定到注册表上
            * 注册中心 key - value
            * */
            Naming.rebind("rmi://127.0.0.1/Hello",helloService);
            System.out.println("服务启动成功");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }
}
  ```
  客户端：
  ```java
  public interface IHelloService extends Remote {
    String sayHello(String msg) throws RemoteException;
}
  ```
  ```java
 public class ClientDemo {
    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        IHelloService helloService=
                (IHelloService)Naming.lookup("rmi://127.0.0.1/Hello");
        System.out.println(helloService.sayHello("Mic"));
    }
}
  ```
  注意：远程对象必须实现 UnicastRemoteObject，这样才能保证客户端访问获得远程对象时，该远程对象会把自身的一个拷贝以 Socket 形式传输给客户端，客户端获得的拷贝称为
“stub” ， 而 服 务 器 端 本 身 已 经 存 在 的 远 程 对 象 成 为“skeleton”，此客户端的 stub 是客户端的一个代理，用于与服务器端进行通信，而 skeleton 是服务端的一个代理，用于接收客户端的请求之后调用远程方法来响应客户端的请求。


## 6.JAVA RMI 源码分析
  远程对象发布
![1538784055](https://ws2.sinaimg.cn/large/006tNc79ly1g28zjsw7xbj30ii0brtcg.jpg)
  远程引用层
![1538784457](https://ws1.sinaimg.cn/large/006tNc79ly1g28zjykgg2j30g00a0q5z.jpg)
  **源码解读：**
  发布远程对象：从上面类图可以知道会发布两个，一个是 RegistryImpl、另外一个是我们自
己写的RMI实现类对象；首先进入HelloServiceImpl的构造函数，调用了父类UnicastRemoteObject 的构造方法，一步步的跟踪进入到了UnicastRemoteObject的私有方法exportObject（），这里做了一个判传进来的远程对象是否是UnicastRemoteObject的子类，如果是的话，将会把UnicastServerRef对象传递给ref(类型为remoteRef,obj为UnicastRemoteObject类型，UnicastRemoteObject继承自remoteServer,remoteServer继承自RemoteObject，所以将Obj强转成UnicastRemoteObject可以得到ref) .如果不是UnicastRemoteObject的子类，那么直接调用UnicastServerRef的exportObject方法。
  **发布对象源码分析:**

```java
//发布对象
IHelloService helloService=new HelloServiceImpl();
```
```java
//调用父类构造函数
 public HelloServiceImpl() throws RemoteException {
        super();
    }
```
```java
//父类构造函数调用链
  protected UnicastRemoteObject() throws RemoteException
    {
        this(0);
    }
    protected UnicastRemoteObject(int port) throws RemoteException
    {
    //默认的端口号为0，this指的就是HelloServiceImpl
        this.port = port;
        exportObject((Remote) this, port);
    }
    public static Remote exportObject(Remote obj, int port)
        throws RemoteException
    {
    //创建了UnicastServerRef对象，对象内引用了LiveRef(tcp通信)
        return exportObject(obj, new UnicastServerRef(port));
    }
```
```java
 private static Remote exportObject(Remote obj, UnicastServerRef sref)
        throws RemoteException
    {
        // 
        if (obj instanceof UnicastRemoteObject) {
            ((UnicastRemoteObject) obj).ref = sref;
        }
        return sref.exportObject(obj, null, false);
    }
```
```java
 public Remote exportObject(Remote var1, Object var2, boolean var3) throws RemoteException {
		//注意这边的var1(传过来的obj),var4（obj的class对象）
        Class var4 = var1.getClass();

        Remote var5;
        try {
        //拿到了obj的代理对象var5,其实就是发布对象的代理对象。
        //getClientRef提供的
            var5 = Util.createProxy(var4, this.getClientRef(), this.forceStubUse);
        } catch (IllegalArgumentException var7) {
            throw new ExportException("remote object implements illegal remote interface", var7);
        }

        if (var5 instanceof RemoteStub) {
            this.setSkeleton(var1);
        }

		//将实际要发布的对象包装成target对象，暴露在TCP端口上，等待客户端调用
        Target var6 = new Target(var1, this, var5, this.ref.getObjID(), var3);
        this.ref.exportObject(var6);
        this.hashToMethod_Map = (Map)hashToMethod_Maps.get(var4);
        return var5;
    }
```
  **服务端启动 Registry 服务**
LocateRegistry.createRegistry(1099);
可以发现服务端创建了一个 RegistryImpl 对象，这里做了一个判断，如果服务端指定的端口是 1099 并且系统开启了安全管理器，那么就可以在限定的权限集内绕过系统的安全校验。这里纯粹是为了提高效率,真正的逻辑在this.setup(newUnicastServerRef())这个方法里面.
```java
 public RegistryImpl(final int var1) throws RemoteException {
        this.bindings = new Hashtable(101);
        if (var1 == 1099 && System.getSecurityManager() != null) {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    public Void run() throws RemoteException {
                        LiveRef var1x = new LiveRef(RegistryImpl.id, var1);
                        RegistryImpl.this.setup(new UnicastServerRef(var1x, (var0) -> {
                            return RegistryImpl.registryFilter(var0);
                        }));
                        return null;
                    }
                }, (AccessControlContext)null, new SocketPermission("localhost:" + var1, "listen,accept"));
            } catch (PrivilegedActionException var3) {
                throw (RemoteException)var3.getException();
            }
        } else {
            LiveRef var2 = new LiveRef(id, var1);
            this.setup(new UnicastServerRef(var2, RegistryImpl::registryFilter));
        }

    }
    private void setup(UnicastServerRef var1) throws RemoteException {
        this.ref = var1;
        var1.exportObject(this, (Object)null, true);
    }
```
setup 方法将指向正在初始化的 RegistryImpl 对象的远程引用 ref(RemoteRef)赋值为传入的 UnicastServerRef 对象，这里涉及到向上转型，然后继续执行 UnicastServerRef 的
exportObject 方法:
```java
public Remote exportObject(Remote var1, Object var2, boolean var3) throws RemoteException {
        Class var4 = var1.getClass();

        Remote var5;
        try {
            var5 = Util.createProxy(var4, this.getClientRef(), this.forceStubUse);
        } catch (IllegalArgumentException var7) {
            throw new ExportException("remote object implements illegal remote interface", var7);
        }

        if (var5 instanceof RemoteStub) {
            this.setSkeleton(var1);
        }

        Target var6 = new Target(var1, this, var5, this.ref.getObjID(), var3);
        this.ref.exportObject(var6);
        this.hashToMethod_Map = (Map)hashToMethod_Maps.get(var4);
        return var5;
    }
```
【var1=RegistryImpl ; var 2 = null ; var3=true】
  进入 UnicastServerRef 的 exportObject()方法。可以看到，这里首先为传入的 RegistryImpl 创建一个代理，这个代理我们可以推断出就是后面服务于客户端的 RegistryImpl 的Stub（RegistryImpl_Stub）对象。然后将 UnicastServerRef
的 skel（skeleton）对象设置为当前 RegistryImpl 对象。最后用 skeleton、stub、UnicastServerRef 对象、id 和一个boolean 值构造了一个 Target 对象，也就是这个 Target 对象基本上包含了全部的信息，等待 TCP 调用。(this.ref.exportObject(var6);)调用UnicastServerRef 的ref（LiveRef）变量的 exportObject()方法。
  到上面为止，我们看到的都是一些变量的赋值和创建工作，还没有到连接层，这些引用对象将会被 Stub 和 Skeleton对象使用。接下来就是连接层上的了:追溯 LiveRef 的exportObject()方法
  LiveRef 与 TCP 通信的类:
  从this.ref.exportObject(var6)进入到了LiveRef
  ```java
   public void exportObject(Target var1) throws RemoteException {
        this.ep.exportObject(var1);
    }
  ```
  顺着上面代码进入到了TCPEndpoint的exportObject方法
  ```java
   public void exportObject(Target var1) throws RemoteException {
        this.transport.exportObject(var1);
    }
  ```
  这个方法做的事情就是将上面构造的Target 对象暴露出去。调用 TCPTransport 的 listen()方法，listen()方法创建了一个 ServerSocket，并且启动了一条线程等待客户端的 请求，接着调用父类Transport的exportObject()将 Target对象存放进ObjectTable 中。
  ```java
   public void exportObject(Target var1) throws RemoteException {
        synchronized(this) {
            this.listen();
            ++this.exportCount;
        }

        boolean var2 = false;
        boolean var12 = false;

        try {
            var12 = true;
            super.exportObject(var1);
            var2 = true;
            var12 = false;
        } finally {
            if (var12) {
                if (!var2) {
                    synchronized(this) {
                        this.decrementExportCount();
                    }
                }

            }
        }

        if (!var2) {
            synchronized(this) {
                this.decrementExportCount();
            }
        }

    }
  ```
  到这里，我们已经将 RegistryImpl 对象创建并且起了服务等待客户端的请求。
**客户端获取服务端 Registry 代理:**
```java
IHelloService helloService=
                (IHelloService)Naming.lookup("rmi://127.0.0.1/Hello");
```
进入到lookup方法：
```java
 public static Remote lookup(String name)
        throws NotBoundException,
            java.net.MalformedURLException,
            RemoteException
    {
        ParsedNamingURL parsed = parseURL(name);
        Registry registry = getRegistry(parsed);

        if (parsed.name == null)
            return registry;
        return registry.lookup(parsed.name);
    }
```
  可以看出一个很明显的方法就是getRegistry,接着跟着代码进入到LocateRegistry的getRegistry方法中，这个方法做的事情是通过传入的 host和 port 构造 RemoteRef 对象，并创建了一个本地代理。这个代理对象其实是 RegistryImpl_Stub 对象。这样客户端
便 有 了 服 务 端 的 RegistryImpl 的 代 理 （ 取 决 于ignoreStubClasses 变量）。但注意此时这个代理其实还没有和服务端的 RegistryImpl 对象关联，毕竟是两个 VM 上
面的对象，这里我们也可以猜测，代理和远程的Registry对象之间是通过socket消息来完成的。
```java
public static Registry getRegistry(String host, int port,
                                       RMIClientSocketFactory csf)
        throws RemoteException
    {
        Registry registry = null;
		//获取仓库地址
        if (port <= 0)
            port = Registry.REGISTRY_PORT;

        if (host == null || host.length() == 0) {
            try {
                host = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                host = "";
            }
        }
        //与TCP通信的类
        LiveRef liveRef =
            new LiveRef(new ObjID(ObjID.REGISTRY_ID),
                        new TCPEndpoint(host, port, csf, null),
                        false);
        RemoteRef ref =
            (csf == null) ? new UnicastRef(liveRef) : new UnicastRef2(liveRef);
		//创建远程代理类，引用LiveRef，好让动态代理时就能进行TCP通信
        return (Registry) Util.createProxy(RegistryImpl.class, ref, false);
```
 回到Naming 类，进入到lookup,找到实现类，RegistryImpl_Stub(客户端代理类), 调用 RegistryImpl_Stub 的 ref（RemoteRef）对象的newCall()方法，将 RegistryImpl_Stub 对象传了进去，不要忘了构造它的时候我们将服务器的主机端口等信息传了进去，也就是我们把服务器相关的信息也传进了 newCall()方法。newCall()方法做的事情简单来看就是建立了跟远程RegistryImpl 的 Skeleton 对象的连接。（不要忘了上面我们说到过服务端通过 TCPTransport 的 exportObject()方法等待着客户端的请求）
  ```java
  public RemoteCall newCall(RemoteObject var1, Operation[] var2, int var3, long var4) throws RemoteException {
        clientRefLog.log(Log.BRIEF, "get connection");
        Connection var6 = this.ref.getChannel().newConnection();

        try {
            clientRefLog.log(Log.VERBOSE, "create call context");
            if (clientCallLog.isLoggable(Log.VERBOSE)) {
                this.logClientCall(var1, var2[var3]);
            }

            StreamRemoteCall var7 = new StreamRemoteCall(var6, this.ref.getObjID(), var3, var4);

            try {
                this.marshalCustomCallData(var7.getOutputStream());
            } catch (IOException var9) {
                throw new MarshalException("error marshaling custom call data");
            }

            return var7;
        } catch (RemoteException var10) {
            this.ref.getChannel().free(var6, false);
            throw var10;
        }
    }
  ```
  连接建立之后自然就是发送请求了。我们知道客户端终究只是拥有 Registry 对象的代理，而不是真正地位于服务端的 Registry 对象本身，他们位于不同的虚拟机实例之中，无法直接调用。必然是通过消息进行交互的。看看this.ref.invoke() 这 里 做 了 什 么 ？ 容 易 追溯 到StreamRemoteCall 的 executeCall()方法。看似本地调用，但其实很容易从代码中看出来是通过 tcp 连接发送消息到服务端。由服务端解析并且处理调用。进入到unicastref的invoke方法：
  ```java
   public void invoke(RemoteCall var1) throws Exception {
        try {
            clientRefLog.log(Log.VERBOSE, "execute call");
            var1.executeCall();
        } catch (RemoteException var3) {
            clientRefLog.log(Log.BRIEF, "exception: ", var3);
            this.free(var1, false);
            throw var3;
        } catch (Error var4) {
            clientRefLog.log(Log.BRIEF, "error: ", var4);
            this.free(var1, false);
            throw var4;
        } catch (RuntimeException var5) {
            clientRefLog.log(Log.BRIEF, "exception: ", var5);
            this.free(var1, false);
            throw var5;
        } catch (Exception var6) {
            clientRefLog.log(Log.BRIEF, "exception: ", var6);
            this.free(var1, true);
            throw var6;
        }
    }
  ```
  ```java
   public void executeCall() throws Exception {
        DGCAckHandler var2 = null;

        byte var1;
        try {
            if (this.out != null) {
                var2 = this.out.getDGCAckHandler();
            }

            this.releaseOutputStream();
            DataInputStream var3 = new DataInputStream(this.conn.getInputStream());
            byte var4 = var3.readByte();
            if (var4 != 81) {
                if (Transport.transportLog.isLoggable(Log.BRIEF)) {
                    Transport.transportLog.log(Log.BRIEF, "transport return code invalid: " + var4);
                }

                throw new UnmarshalException("Transport return code invalid");
            }

            this.getInputStream();
            var1 = this.in.readByte();
            this.in.readID();
        } catch (UnmarshalException var11) {
            throw var11;
        } catch (IOException var12) {
            throw new UnmarshalException("Error unmarshaling return header", var12);
        } finally {
            if (var2 != null) {
                var2.release();
            }

        }

        switch(var1) {
        case 1:
            return;
        case 2:
            Object var14;
            try {
                var14 = this.in.readObject();
            } catch (Exception var10) {
                throw new UnmarshalException("Error unmarshaling return", var10);
            }

            if (!(var14 instanceof Exception)) {
                throw new UnmarshalException("Return type not Exception");
            } else {
                this.exceptionReceivedFromServer((Exception)var14);
            }
        default:
            if (Transport.transportLog.isLoggable(Log.BRIEF)) {
                Transport.transportLog.log(Log.BRIEF, "return code invalid: " + var1);
            }

            throw new UnmarshalException("Return code invalid");
        }
    }
  ```
  至此，我们已经将客户端的服务查询请求发出了。服务端接收客户端的服务查询请求并返回给客户端结果这里我们继续跟踪 server 端代码的服务发布代码，一步步往上面翻。从RegistryImpl的this.setup开始进入到exportObject
  ```java
   public Remote exportObject(Remote var1, Object var2, boolean var3) throws RemoteException {
        Class var4 = var1.getClass();

        Remote var5;
        try {
            var5 = Util.createProxy(var4, this.getClientRef(), this.forceStubUse);
        } catch (IllegalArgumentException var7) {
            throw new ExportException("remote object implements illegal remote interface", var7);
        }

        if (var5 instanceof RemoteStub) {
            this.setSkeleton(var1);
        }

        Target var6 = new Target(var1, this, var5, this.ref.getObjID(), var3);
        this.ref.exportObject(var6);
        this.hashToMethod_Map = (Map)hashToMethod_Maps.get(var4);
        return var5;
    }
  ```
  从this.ref.exportObject(var6)进入到LiveRef.class:
  ```java
  public void exportObject(Target var1) throws RemoteException {
        this.ep.exportObject(var1);
    }
  ```
  点击exportObject进入到Endpoint.class->TCPEndpoint.class->TCPTransport.class
  ```java
    public void exportObject(Target var1) throws RemoteException {
        this.transport.exportObject(var1);
    }
  ```
  ```java
   public void exportObject(Target var1) throws RemoteException {
        synchronized(this) {
            this.listen();
            ++this.exportCount;
        }

        boolean var2 = false;
        boolean var12 = false;

        try {
            var12 = true;
            super.exportObject(var1);
            var2 = true;
            var12 = false;
        } finally {
            if (var12) {
                if (!var2) {
                    synchronized(this) {
                        this.decrementExportCount();
                    }
                }

            }
        }

        if (!var2) {
            synchronized(this) {
                this.decrementExportCount();
            }
        }

    }
  ```
  在 TCP 协议层发起 socket 监听，并采用多线程循环接收请求：TCPTransport.AcceptLoop(this.server)
  ```java
   private void listen() throws RemoteException {
        assert Thread.holdsLock(this);

        TCPEndpoint var1 = this.getEndpoint();
        int var2 = var1.getPort();
        if (this.server == null) {
            if (tcpLog.isLoggable(Log.BRIEF)) {
                tcpLog.log(Log.BRIEF, "(port " + var2 + ") create server socket");
            }

            try {
                this.server = var1.newServerSocket();
                Thread var3 = (Thread)AccessController.doPrivileged(new NewThreadAction(new TCPTransport.AcceptLoop(this.server), "TCP Accept-" + var2, true));
                var3.start();
            } catch (BindException var4) {
                throw new ExportException("Port already in use: " + var2, var4);
            } catch (IOException var5) {
                throw new ExportException("Listen failed on port: " + var2, var5);
            }
        } else {
            SecurityManager var6 = System.getSecurityManager();
            if (var6 != null) {
                var6.checkListen(var2);
            }
        }

    }
  ```
  new NewThreadAction(new TCPTransport.AcceptLoop(this.server), "TCP Accept-" + var2, true) ,由这句话的AcceptLoop进入到TCPTransport.class的this.executeAcceptLoop()方法，继续通过线程池来处理 socket 接收到的请求，
 ```java
  private void executeAcceptLoop() {
            if (TCPTransport.tcpLog.isLoggable(Log.BRIEF)) {
                TCPTransport.tcpLog.log(Log.BRIEF, "listening on port " + TCPTransport.this.getEndpoint().getPort());
            }

            while(true) {
                Socket var1 = null;

                try {
                    var1 = this.serverSocket.accept();
                    InetAddress var16 = var1.getInetAddress();
                    String var3 = var16 != null ? var16.getHostAddress() : "0.0.0.0";

                    try {
                        TCPTransport.connectionThreadPool.execute(TCPTransport.this.new ConnectionHandler(var1, var3));
                    } catch (RejectedExecutionException var11) {
                        TCPTransport.closeSocket(var1);
                        TCPTransport.tcpLog.log(Log.BRIEF, "rejected connection from " + var3);
                    }
                } catch (Throwable var15) {
                    Throwable var2 = var15;

                    try {
                        if (this.serverSocket.isClosed()) {
                            return;
                        }

                        try {
                            if (TCPTransport.tcpLog.isLoggable(Level.WARNING)) {
                                TCPTransport.tcpLog.log(Level.WARNING, "accept loop for " + this.serverSocket + " throws", var2);
                            }
                        } catch (Throwable var13) {
                            ;
                        }
                    } finally {
                        if (var1 != null) {
                            TCPTransport.closeSocket(var1);
                        }

                    }

                    if (!(var15 instanceof SecurityException)) {
                        try {
                            TCPEndpoint.shedConnectionCaches();
                        } catch (Throwable var12) {
                            ;
                        }
                    }

                    if (!(var15 instanceof Exception) && !(var15 instanceof OutOfMemoryError) && !(var15 instanceof NoClassDefFoundError)) {
                        if (var15 instanceof Error) {
                            throw (Error)var15;
                        }

                        throw new UndeclaredThrowableException(var15);
                    }

                    if (!this.continueAfterAcceptFailure(var15)) {
                        return;
                    }
                }
            }
        }
 ```
  再之后进入到run方法：
  ```java
  public void run() {
            Thread var1 = Thread.currentThread();
            String var2 = var1.getName();

            try {
                var1.setName("RMI TCP Connection(" + TCPTransport.connectionCount.incrementAndGet() + ")-" + this.remoteHost);
                AccessController.doPrivileged(() -> {
                    this.run0();
                    return null;
                }, TCPTransport.NOPERMS_ACC);
            } finally {
                var1.setName(var2);
            }

        }
  ```
  之后会走到 TCPTransport.this.handleMessages(var14, true);然后再执行到case 80，执行servicecall方法。
  ```java
  public boolean serviceCall(final RemoteCall var1) {
        try {
            ObjID var39;
            try {
                var39 = ObjID.read(var1.getInputStream());
            } catch (IOException var33) {
                throw new MarshalException("unable to read objID", var33);
            }

            Transport var40 = var39.equals(dgcID) ? null : this;
            Target var5 = ObjectTable.getTarget(new ObjectEndpoint(var39, var40));
            final Remote var37;
            if (var5 != null && (var37 = var5.getImpl()) != null) {
                final Dispatcher var6 = var5.getDispatcher();
                var5.incrementCallCount();

                boolean var8;
                try {
                    transportLog.log(Log.VERBOSE, "call dispatcher");
                    final AccessControlContext var7 = var5.getAccessControlContext();
                    ClassLoader var41 = var5.getContextClassLoader();
                    ClassLoader var9 = Thread.currentThread().getContextClassLoader();

                    try {
                        setContextClassLoader(var41);
                        currentTransport.set(this);

                        try {
                            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                                public Void run() throws IOException {
                                    Transport.this.checkAcceptPermission(var7);
                                    var6.dispatch(var37, var1);
                                    return null;
                                }
                            }, var7);
                            return true;
                        } catch (PrivilegedActionException var31) {
                            throw (IOException)var31.getException();
                        }
                    } finally {
                        setContextClassLoader(var9);
                        currentTransport.set((Object)null);
                    }
                } catch (IOException var34) {
                    transportLog.log(Log.BRIEF, "exception thrown by dispatcher: ", var34);
                    var8 = false;
                } finally {
                    var5.decrementCallCount();
                }

                return var8;
            }

            throw new NoSuchObjectException("no such object in table");
        } catch (RemoteException var36) {
            RemoteException var2 = var36;
            if (UnicastServerRef.callLog.isLoggable(Log.BRIEF)) {
                String var3 = "";

                try {
                    var3 = "[" + RemoteServer.getClientHost() + "] ";
                } catch (ServerNotActiveException var30) {
                    ;
                }

                String var4 = var3 + "exception: ";
                UnicastServerRef.callLog.log(Log.BRIEF, var4, var36);
            }

            try {
                ObjectOutput var38 = var1.getResultStream(false);
                UnicastServerRef.clearStackTraces(var2);
                var38.writeObject(var2);
                var1.releaseOutputStream();
            } catch (IOException var29) {
                transportLog.log(Log.BRIEF, "exception thrown marshalling exception: ", var29);
                return false;
            }
        }

        return true;
    }
  ```
  到ObjectTable.getTarget()为止做的事情是从 socket 流中获取 ObjId，并通过 ObjId 和 Transport 对象获取 Target 对象，这里的 Target 对象已经是服务端的对象。再借由 Target的派发器 Dispatcher，传入参数服务实现和请求对象RemoteCall，将请求派发给服务端那个真正提供服务的RegistryImpl 的 lookUp()方法，这就是 Skeleton 移交给具体实现的过程了，Skeleton 负责底层的操作.所以客户端通过IHelloService helloService=(IHelloService)Naming.lookup("rmi://127.0.0.1/Hello");先会创建一个 RegistryImpl_Stub 的代理类，通过这个代理类进行 socket 网络请求，将 lookup 发送到服务端，服务端通过接收到请求以后，通过服务端的RegistryImpl_Stub
（Skeleton），执行 RegistryImpl 的 lookUp。而服务端的RegistryImpl 返回的就是服务端的 HeloServiceImpl 的实现类。
  客 户 端 获 取 通 过 lookUp() 查 询 获 得 的 客 户 端HelloServiceImpl 的 Stub 对象客 户 端 通 过 Lookup 查 询 获 得 的 是 客 户 端HelloServiceImpl 的 Stub 对象（这一块我们看不到，因为这块由 Skeleton 为我们屏蔽了），然后后续的处理仍然是通过 HelloServiceImpl_Stub 代理对象通过 socket 网络请求到服务端，通过服务端的
HelloServiceImpl_Stub(Skeleton) 进行代理，将请求通过Dispatcher 转发到对应的服务端方法获得结果以后再次通过 socket 把结果返回到客户端；
  RMI 做了什么
根据上面的源码阅读，实际上我们看到的应该是有两个代理类，一个是 RegistryImpl 的 代 理 类 和 我 们HelloServiceImpl 的代理类。
​               

## 5.RPC框架原理
  一定要说明，在 RMI Client 实施正式的 RMI 调用前，它必须通过 LocateRegistry 或者 Naming 方式到 RMI 注册表寻找要调用的 RMI 注册信息。找到 RMI 事务注册信息后，
Client 会从 RMI 注册表获取这个 RMI Remote Service 的Stub 信息。这个过程成功后，RMI Client 才能开始正式的调用过程。另外要说明的是 RMI Client 正式调用过程，也不是由 RMIClient 直接访问 Remote Service，而是由客户端获取的Stub 作为 RMI Client 的代理访问 Remote Service 的代理Skeleton，如上图所示的顺序。也就是说真实的请求调用是
在 Stub-Skeleton 之间进行的。Registry 并不参与具体的 Stub-Skeleton 的调用过程，只负责记录“哪个服务名”使用哪一个 Stub，并在 RemoteClient 询问它时将这个 Stub 拿给 Client（如果没有就会报错）。


## 7.实现自己的RPC框架
server端：
```java
public class GpHelloImpl implements IGpHello{
    @Override
    public String sayHello(String msg) {
        return "Hello , "+msg;
    }
}
```
```java
public interface IGpHello {

    String sayHello(String msg);
}
```
```java
*
 * 处理socket请求
 */
public class ProcessorHandler implements Runnable{

    private Socket socket;
    private Object service; //服务端发布的服务

    public ProcessorHandler(Socket socket, Object service) {
        this.socket = socket;
        this.service = service;
    }

    @Override
    public void run() {
        //处理请求
        ObjectInputStream inputStream=null;
        try {
            //获取客户端的输入流
            inputStream=new ObjectInputStream(socket.getInputStream());
            //反序列化远程传输的对象RpcRequest
            RpcRequest request=(RpcRequest) inputStream.readObject();
            Object result=invoke(request); //通过反射去调用本地的方法

            //通过输出流讲结果输出给客户端
            ObjectOutputStream outputStream=new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(result);
            outputStream.flush();
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(inputStream!=null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private Object invoke(RpcRequest request) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        //一下均为反射操作，目的是通过反射调用服务
        Object[] args=request.getParameters();
        Class<?>[] types=new Class[args.length];
        for(int i=0;i<args.length;i++){
            types[i]=args[i].getClass();
        }
        Method method=service.getClass().getMethod(request.getMethodName(),types);
        return method.invoke(service,args);
    }
}

```
```java
* 传输对象
 */
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = -9100893052391757993L;
    private String className;
    private String methodName;
    private Object[] parameters;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}
```
```java
 *
 * 用于发布一个远程服务
 */
public class RpcServer {
    //创建一个线程池
    private static final ExecutorService executorService=Executors.newCachedThreadPool();

    public void publisher(final Object service,int port){
        ServerSocket serverSocket=null;
        try{
            serverSocket=new ServerSocket(port);  //启动一个服务监听

            while(true){ //循环监听
                Socket socket=serverSocket.accept(); //监听服务
                //通过线程池去处理请求
                executorService.execute(new ProcessorHandler(socket,service));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(serverSocket!=null){
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}

```
```java
public class ServerDemo {
    public static void main(String[] args) {
        IGpHello iGpHello=new GpHelloImpl();
        RpcServer rpcServer=new RpcServer();
        rpcServer.publisher(iGpHello,8888);
    }
}
```
客户端：
```java
public class ClientDemo {

    public static void main(String[] args) {
        RpcClientProxy rpcClientProxy=new RpcClientProxy();

        IGpHello hello=rpcClientProxy.clientProxy
                (IGpHello.class,"localhost",8888);
        System.out.println(hello.sayHello("mic"));
    }
}

```
```java
public interface IGpHello {

    String sayHello(String msg);
}
```
```java
public class RemoteInvocationHandler implements InvocationHandler {
    private String host;
    private int port;

    public RemoteInvocationHandler(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //组装请求
        RpcRequest request=new RpcRequest();
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameters(args);
        //通过tcp传输协议进行传输
        TCPTransport tcpTransport=new TCPTransport(this.host,this.port);
        //发送请求
        return tcpTransport.send(request);
    }
}
```
```java
public class RpcClientProxy {


    /**
     * 创建客户端的远程代理。通过远程代理进行访问
     * @param interfaceCls
     * @param host
     * @param port
     * @param <T>
     * @return
     */
    public <T> T clientProxy(final Class<T>
                                     interfaceCls,
                             final String host,final int port){
        //使用到了动态代理。
        return (T)Proxy.newProxyInstance(interfaceCls.getClassLoader(),
                new Class[]{interfaceCls},new RemoteInvocationHandler(host,port));
    }
}

```
```java
* 传输对象（用于组装请求的相关信息）
 */
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = -9100893052391757993L;
    private String className;
    private String methodName;
    private Object[] parameters;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}

```
```java
public class TCPTransport {

    private String host;

    private int port;

    public TCPTransport(String host, int port) {
        this.host = host;
        this.port = port;
    }

    //创建一个socket连接
    private Socket newSocket(){
        System.out.println("创建一个新的连接");
        Socket socket;
        try{
            socket=new Socket(host,port);
            return socket;
        }catch (Exception e){
            throw new RuntimeException("连接建立失败");
        }
    }

    public Object send(RpcRequest request){
        Socket socket=null;
        try {
            socket = newSocket();
            //获取输出流，将客户端需要调用的远程方法参数request发送给
            ObjectOutputStream outputStream=new ObjectOutputStream
                    (socket.getOutputStream());
            outputStream.writeObject(request);
            outputStream.flush();
            //获取输入流，得到服务端的返回结果
            ObjectInputStream inputStream=new ObjectInputStream
                    (socket.getInputStream());
            Object result=inputStream.readObject();
            inputStream.close();
            outputStream.close();
            return result;

        }catch (Exception e ){
            throw new RuntimeException("发起远程调用异常:",e);
        }finally {
            if(socket!=null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

-----

> https://blog.csdn.net/kingcat666/article/details/78578578