### 基于 Java API 初探 zookeeper 的使用：

先来简单看一下API的使用：

```java
public class ConnectionDemo {
 
    public static void main(String[] args) {
        try {
            final CountDownLatch countDownLatch=new CountDownLatch(1);
            ZooKeeper zooKeeper=
                    new ZooKeeper("192.168.254.135:2181," +
                            "192.168.254.136:2181,192.168.254.137:2181",
                            4000, new Watcher() {
                        @Override
                        public void process(WatchedEvent event) {
                            if(Event.KeeperState.SyncConnected==event.getState()){
                                //如果收到了服务端的响应事件，连接成功
                                countDownLatch.countDown();
                            }
                        }
                    });
            System.out.println(zooKeeper.getState());//CONNECTING
            countDownLatch.await();
            System.out.println(zooKeeper.getState());//CONNECTED
 
            //添加节点
            zooKeeper.create("/zk-wuzz","0".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
            Thread.sleep(1000);
            Stat stat=new Stat();
 
            //得到当前节点的值
            byte[] bytes=zooKeeper.getData("/zk-wuzz",null,stat);
            System.out.println(new String(bytes)); // 0
 
            //修改节点值
            zooKeeper.setData("/zk-wuzz","1".getBytes(),stat.getVersion());
 
            //得到当前节点的值
            byte[] bytes1=zooKeeper.getData("/zk-wuzz",null,stat);
            System.out.println(new String(bytes1)); // 1
 
            zooKeeper.delete("/zk-wuzz",stat.getVersion());
 
            zooKeeper.close();
 
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }
}
```

### 事件机制：

　　Watcher 监听机制是 Zookeeper 中非常重要的特性，我们基于 zookeeper 上创建的节点，可以对这些节点绑定监听事件，比如可以监听节点数据变更、节点删除、子节点状态变更等事件，通过这个事件机制，可以基于 zookeeper实现分布式锁、集群管理等功能。

　　watcher 特性：当数据发生变化的时候， zookeeper 会产生一个 watcher 事件，并且会发送到客户端。但是客户端只会收到一次通知。如果后续这个节点再次发生变化，那么之前设置 watcher 的客户端不会再次收到消息。（watcher 是一次性的操作）。 可以通过循环监听去达到永久监听效果。

如何注册事件机制：

　　通过这三个操作来绑定事件 ：getData、Exists、getChildren

　　如何触发事件？ 凡是事务类型的操作，都会触发监听事件。create /delete /setData,来看以下代码简单实现:

```java
public class WatcherDemo {
 
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        final CountDownLatch countDownLatch=new CountDownLatch(1);
        final ZooKeeper zooKeeper=
                 new ZooKeeper("192.168.254.135:2181," +
                         "192.168.254.136:2181,192.168.254.137:2181",
                        4000, new Watcher() {
                    @Override
                    public void process(WatchedEvent event) {
                        System.out.println("默认事件： "+event.getType());
                        if(Event.KeeperState.SyncConnected==event.getState()){
                            //如果收到了服务端的响应事件，连接成功
                            countDownLatch.countDown();
                        }
                    }
                });
        countDownLatch.await();
 
        zooKeeper.create("/zk-wuzz","1".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
 
 
        //exists  getdata getchildren
        //通过exists绑定事件
        Stat stat=zooKeeper.exists("/zk-wuzz", new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println(event.getType()+"->"+event.getPath());
                try {
                    //再一次去绑定事件 ,但是这个走的是默认事件
                    zooKeeper.exists(event.getPath(),true);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        //通过修改的事务类型操作来触发监听事件
        stat=zooKeeper.setData("/zk-wuzz","2".getBytes(),stat.getVersion());
 
        Thread.sleep(1000);
 
        zooKeeper.delete("/zk-wuzz",stat.getVersion());
 
        System.in.read();
    }
}

```

以上就是 Watcher 的简单实现操作。接下来浅析一下这个 Watcher 实现的流程。

### watcher 事件类型：

```java
//org.apache.zookeeper.Watcher.Event.EventTypepublic enum EventType {
         None (-1), // 客户端连接状态发生变化的时候 会受到none事件
         NodeCreated (1), // 节点创建事件
         NodeDeleted (2), // 节点删除事件
         NodeDataChanged (3), // 节点数据变化
         NodeChildrenChanged (4); // 子节点被创建 删除触发该事件
}
```

### 事件的实现原理：

　　client 端连接后会注册一个事件，然后客户端会保存这个事件，通过zkWatcherManager 保存客户端的事件注册，通知服务端 Watcher 为 true，然后服务端会通过WahcerManager 会绑定path对应的事件。如下图：

![image-20190422150740771](https://ws3.sinaimg.cn/large/006tNc79ly1g2bfetik1xj31000gkdjv.jpg)

接下去通过源码层面去熟悉一下这个 Watcher 的流程。由于我们demo 是通过exists 来注册事件，那么我们就通过 exists 来作为入口。

```java
// org.apache.zookeeper.ZooKeeperpublic Stat exists(final String path, Watcher watcher)
        throws KeeperException, InterruptedException
    {
        final String clientPath = path;
        PathUtils.validatePath(clientPath);
 
        // the watch contains the un-chroot path
        WatchRegistration wcb = null; // 存储事件
        if (watcher != null) { // 实例化监听事件
            wcb = new ExistsWatchRegistration(watcher, clientPath);
        }
 
        final String serverPath = prependChroot(clientPath);
 
        RequestHeader h = new RequestHeader();// 构建请求
        h.setType(ZooDefs.OpCode.exists);
        ExistsRequest request = new ExistsRequest(); // 构建请求
        request.setPath(serverPath);
        request.setWatch(watcher != null);
        SetDataResponse response = new SetDataResponse();//构建响应对象
       // 发送请求
        ReplyHeader r = cnxn.submitRequest(h, request, response, wcb);
        if (r.getErr() != 0) {
            if (r.getErr() == KeeperException.Code.NONODE.intValue()) {
                return null;
            }
            throw KeeperException.create(KeeperException.Code.get(r.getErr()),
                    clientPath);
        }
 
        return response.getStat().getCzxid() == -1 ? null : response.getStat();
}

```

接下去先看一下请求提交：

```java
public ReplyHeader submitRequest(RequestHeader h, Record request,
            Record response, WatchRegistration watchRegistration)
            throws InterruptedException {
        ReplyHeader r = new ReplyHeader();
       //组装 请求包packet
        Packet packet = queuePacket(h, r, request, response, null, null, null,
                    null, watchRegistration);
        synchronized (packet) {
            while (!packet.finished) {
                packet.wait();
            }
        }
        return r;
    }
```

queuePacket：

```java
Packet queuePacket(RequestHeader h, ReplyHeader r, Record request,
            Record response, AsyncCallback cb, String clientPath,
            String serverPath, Object ctx, WatchRegistration watchRegistration)
    {
        Packet packet = null;
 
        // Note that we do not generate the Xid for the packet yet. It is
        // generated later at send-time, by an implementation of ClientCnxnSocket::doIO(),
        // where the packet is actually sent.
        synchronized (outgoingQueue) {//将请求包加入队列
            packet = new Packet(h, r, request, response, watchRegistration);
            packet.cb = cb;
            packet.ctx = ctx;
            packet.clientPath = clientPath;
            packet.serverPath = serverPath;
            if (!state.isAlive() || closing) {
                conLossPacket(packet);
            } else {
                // If the client is asking to close the session then
                // mark as closing
                if (h.getType() == OpCode.closeSession) {
                    closing = true;
                }
                outgoingQueue.add(packet);
            }
        }
       // 通过ClientCnxnSocketNIO的 selector 多路复用去执行异步通信
        sendThread.getClientCnxnSocket().wakeupCnxn();
        return packet;
    }
```

看到这里，发现只是发送了数据，那哪里触发了对 outgoingQueue 队列的消息进行消费。再把组装的packeet 放入队列的时候用到的 cnxn.submitRequest(h, request, response, wcb);这个cnxn 是哪里来的呢？ 再看 zookeeper的构造函数，我们可以看到如下代码：

```java
public ZooKeeper(String connectString, int sessionTimeout, Watcher watcher,
            boolean canBeReadOnly)
        throws IOException
    {
        LOG.info("Initiating client connection, connectString=" + connectString
                + " sessionTimeout=" + sessionTimeout + " watcher=" + watcher);
        // 配置事件监听
        watchManager.defaultWatcher = watcher;
 
        ConnectStringParser connectStringParser = new ConnectStringParser(
                connectString);
        HostProvider hostProvider = new StaticHostProvider(
                connectStringParser.getServerAddresses());
       // 构造 cnxn ClientCnxn
        cnxn = new ClientCnxn(connectStringParser.getChrootPath(),
                hostProvider, sessionTimeout, this, watchManager,
                getClientCnxnSocket(), canBeReadOnly);
        cnxn.start();
    }
```

new ClientCnxn(connectStringParser.getChrootPath(),hostProvider, sessionTimeout, this, watchManager, getClientCnxnSocket(), canBeReadOnly); 创建 SendThread，eventThread两个线程并启动线程：

```java
public ClientCnxn(String chrootPath, HostProvider hostProvider, int sessionTimeout, ZooKeeper zooKeeper,
            ClientWatchManager watcher, ClientCnxnSocket clientCnxnSocket,
            long sessionId, byte[] sessionPasswd, boolean canBeReadOnly) {
        this.zooKeeper = zooKeeper;
        this.watcher = watcher;
        this.sessionId = sessionId;
        this.sessionPasswd = sessionPasswd;
        this.sessionTimeout = sessionTimeout;
        this.hostProvider = hostProvider;
        this.chrootPath = chrootPath;
 
        connectTimeout = sessionTimeout / hostProvider.size();
        readTimeout = sessionTimeout * 2 / 3;
        readOnly = canBeReadOnly;
 
        sendThread = new SendThread(clientCnxnSocket);
        eventThread = new EventThread();
 
    }
```

找到内部类 SendThread 的 run 方法：可以在方法内部找到一个方法   clientCnxnSocket.doTransport(to, pendingQueue, outgoingQueue, ClientCnxn.this); ZooKeeper通过获取ZOOKEEPER_CLIENT_CNXN_SOCKET变量构造了一个ClientCnxnSocket对象，默认情况下是ClientCnxnSocketNIO类。这个方法就是传输的核心方法。SendThread通过doTransport将Packet发送给Server,并通过readResponse获取结果，解析成一个Event，再将Event加入EventThread的队列中等待执行。EventThread通过processEvent消费队列中的Event事件。SendThread 的主要作用除了将Packet包发送给Server之外，还负责维持Client和Server之间的心跳，确保 session 存活。SendThread是一个线程类，因此我们进入其run()方法，看看他的启动流程。

```java
public void run() {
            clientCnxnSocket.introduce(this,sessionId);
            clientCnxnSocket.updateNow();
            clientCnxnSocket.updateLastSendAndHeard();
            int to;
            long lastPingRwServer = Time.currentElapsedTime();
            final int MAX_SEND_PING_INTERVAL = 10000; //10 seconds
            InetSocketAddress serverAddress = null;
            while (state.isAlive()) {
                try {
                    if (!clientCnxnSocket.isConnected()) {
                        if(!isFirstConnect){
                            try {
                                Thread.sleep(r.nextInt(1000));
                            } catch (InterruptedException e) {
                                LOG.warn("Unexpected exception", e);
                            }
                        }
                        // don't re-establish connection if we are closing
                        if (closing || !state.isAlive()) {
                            break;
                        }
                        if (rwServerAddress != null) {
                            serverAddress = rwServerAddress;
                            rwServerAddress = null;
                        } else {
                            serverAddress = hostProvider.next(1000);
                        }
                        // 启动连接
                        startConnect(serverAddress);
                        clientCnxnSocket.updateLastSendAndHeard();
                    }
 
                    if (state.isConnected()) {// 连接成功
                        // determine whether we need to send an AuthFailed event.
                        if (zooKeeperSaslClient != null) {
                            boolean sendAuthEvent = false;
                            if (zooKeeperSaslClient.getSaslState() == ZooKeeperSaslClient.SaslState.INITIAL) {
                                try {
                                    zooKeeperSaslClient.initialize(ClientCnxn.this);
                                } catch (SaslException e) {
                                   LOG.error("SASL authentication with Zookeeper Quorum member failed: " + e);
                                    state = States.AUTH_FAILED;
                                    sendAuthEvent = true;
                                }
                            }
                            KeeperState authState = zooKeeperSaslClient.getKeeperState();
                            if (authState != null) {
                                if (authState == KeeperState.AuthFailed) {
                                    // An authentication error occurred during authentication with the Zookeeper Server.
                                    state = States.AUTH_FAILED;
                                    sendAuthEvent = true;
                                } else {
                                    if (authState == KeeperState.SaslAuthenticated) {
                                        sendAuthEvent = true;
                                    }
                                }
                            }
 
                            if (sendAuthEvent == true) {
                                eventThread.queueEvent(new WatchedEvent(
                                      Watcher.Event.EventType.None,
                                      authState,null));
                            }
                        }
                        to = readTimeout - clientCnxnSocket.getIdleRecv();
                    } else {
                        to = connectTimeout - clientCnxnSocket.getIdleRecv();
                    }
                     
                    if (to <= 0) {
                        String warnInfo;
                        warnInfo = "Client session timed out, have not heard from server in "
                            + clientCnxnSocket.getIdleRecv()
                            + "ms"
                            + " for sessionid 0x"
                            + Long.toHexString(sessionId);
                        LOG.warn(warnInfo);
                        throw new SessionTimeoutException(warnInfo);
                    }
                    if (state.isConnected()) {
                        //1000(1 second) is to prevent race condition missing to send the second ping
                        //also make sure not to send too many pings when readTimeout is small
                        int timeToNextPing = readTimeout / 2 - clientCnxnSocket.getIdleSend() -
                                ((clientCnxnSocket.getIdleSend() > 1000) ? 1000 : 0);
                        //send a ping request either time is due or no packet sent out within MAX_SEND_PING_INTERVAL
                        if (timeToNextPing <= 0 || clientCnxnSocket.getIdleSend() > MAX_SEND_PING_INTERVAL) {// 发送心跳包
                            sendPing();
                            clientCnxnSocket.updateLastSend();
                        } else {
                            if (timeToNextPing < to) {
                                to = timeToNextPing;
                            }
                        }
                    }
 
                    // If we are in read-only mode, seek for read/write server
                    if (state == States.CONNECTEDREADONLY) {
                        long now = Time.currentElapsedTime();
                        int idlePingRwServer = (int) (now - lastPingRwServer);
                        if (idlePingRwServer >= pingRwTimeout) {
                            lastPingRwServer = now;
                            idlePingRwServer = 0;
                            pingRwTimeout =
                                Math.min(2*pingRwTimeout, maxPingRwTimeout);
                            pingRwServer();
                        }
                        to = Math.min(to, pingRwTimeout - idlePingRwServer);
                    }
                    // 将指令发送给Server
                    clientCnxnSocket.doTransport(to, pendingQueue, outgoingQueue, ClientCnxn.this);
                } catch (Throwable e) {
                    if (closing) {
                        if (LOG.isDebugEnabled()) {
                            // closing so this is expected
                            LOG.debug("An exception was thrown while closing send thread for session 0x"
                                    + Long.toHexString(getSessionId())
                                    + " : " + e.getMessage());
                        }
                        break;
                    } else {
                        // this is ugly, you have a better way speak up
                        if (e instanceof SessionExpiredException) {
                            LOG.info(e.getMessage() + ", closing socket connection");
                        } else if (e instanceof SessionTimeoutException) {
                            LOG.info(e.getMessage() + RETRY_CONN_MSG);
                        } else if (e instanceof EndOfStreamException) {
                            LOG.info(e.getMessage() + RETRY_CONN_MSG);
                        } else if (e instanceof RWServerFoundException) {
                            LOG.info(e.getMessage());
                        } else if (e instanceof SocketException) {
                            LOG.info("Socket error occurred: {}: {}", serverAddress, e.getMessage());
                        } else {
                            LOG.warn("Session 0x{} for server {}, unexpected error{}",
                                            Long.toHexString(getSessionId()),
                                            serverAddress,
                                            RETRY_CONN_MSG,
                                            e);
                        }
                        cleanup();
                        if (state.isAlive()) {
                            eventThread.queueEvent(new WatchedEvent(
                                    Event.EventType.None,
                                    Event.KeeperState.Disconnected,
                                    null));
                        }
                        clientCnxnSocket.updateNow();
                        clientCnxnSocket.updateLastSendAndHeard();
                    }
                }
            }
            cleanup();
            clientCnxnSocket.close();
            if (state.isAlive()) {
                eventThread.queueEvent(new WatchedEvent(Event.EventType.None,
                        Event.KeeperState.Disconnected, null));
            }
            ZooTrace.logTraceMessage(LOG, ZooTrace.getTextTraceLevel(),
                    "SendThread exited loop for session: 0x"
                           + Long.toHexString(getSessionId()));

```

我们看到在请求被组装放入到 outgoingQueue 的时候有如下代码

```java
synchronized (packet) {
            while (!packet.finished) {
                packet.wait();
            }
        }
```

这是在等待数据被包解析。 在doTransport 方法中的doIO发送socket信息之前，先从socket中获取返回数据，通过readResonse进行处理。

```java
void doIO(List<Packet> pendingQueue, LinkedList<Packet> outgoingQueue, ClientCnxn cnxn)
      throws InterruptedException, IOException {
        SocketChannel sock = (SocketChannel) sockKey.channel();
        if (sock == null) {
            throw new IOException("Socket is null!");
        }
        if (sockKey.isReadable()) {
            int rc = sock.read(incomingBuffer);
            if (rc < 0) {
                throw new EndOfStreamException(
                        "Unable to read additional data from server sessionid 0x"
                                + Long.toHexString(sessionId)
                                + ", likely server has closed socket");
            }
            if (!incomingBuffer.hasRemaining()) {
                incomingBuffer.flip();
                if (incomingBuffer == lenBuffer) {
                    recvCount++;
                    readLength();
                } else if (!initialized) {
                    readConnectResult();
                    enableRead();
                    if (findSendablePacket(outgoingQueue,
                            cnxn.sendThread.clientTunneledAuthenticationInProgress()) != null) {
                        // Since SASL authentication has completed (if client is configured to do so),
                        // outgoing packets waiting in the outgoingQueue can now be sent.
                        enableWrite();
                    }
                    lenBuffer.clear();
                    incomingBuffer = lenBuffer;
                    updateLastHeard();
                    initialized = true;
                } else {
                    // 读取返回信息
                    sendThread.readResponse(incomingBuffer);
                    lenBuffer.clear();
                    incomingBuffer = lenBuffer;
                    updateLastHeard();
                }
            }
        }
        if (sockKey.isWritable()) {
            synchronized(outgoingQueue) {
                Packet p = findSendablePacket(outgoingQueue,
                        cnxn.sendThread.clientTunneledAuthenticationInProgress());
 
                if (p != null) {
                    updateLastSend();
                    // If we already started writing p, p.bb will already exist
                    if (p.bb == null) {
                        if ((p.requestHeader != null) &&
                                (p.requestHeader.getType() != OpCode.ping) &&
                                (p.requestHeader.getType() != OpCode.auth)) {
                            p.requestHeader.setXid(cnxn.getXid());
                        }
                        p.createBB();
                    }
                    sock.write(p.bb);
                    if (!p.bb.hasRemaining()) {
                        sentCount++;
                        outgoingQueue.removeFirstOccurrence(p);
                        if (p.requestHeader != null
                                && p.requestHeader.getType() != OpCode.ping
                                && p.requestHeader.getType() != OpCode.auth) {
                            synchronized (pendingQueue) {
                                pendingQueue.add(p);
                            }
                        }
                    }
                }
                if (outgoingQueue.isEmpty()) {
                    // No more packets to send: turn off write interest flag.
                    // Will be turned on later by a later call to enableWrite(),
                    // from within ZooKeeperSaslClient (if client is configured
                    // to attempt SASL authentication), or in either doIO() or
                    // in doTransport() if not.
                    disableWrite();
                } else if (!initialized && p != null && !p.bb.hasRemaining()) {
                    // On initial connection, write the complete connect request
                    // packet, but then disable further writes until after
                    // receiving a successful connection response.  If the
                    // session is expired, then the server sends the expiration
                    // response and immediately closes its end of the socket.  If
                    // the client is simultaneously writing on its end, then the
                    // TCP stack may choose to abort with RST, in which case the
                    // client would never receive the session expired event.  See
                    // http://docs.oracle.com/javase/6/docs/technotes/guides/net/articles/connection_release.html
                    disableWrite();
                } else {
                    // Just in case
                    enableWrite();
                }
            }
        }
    }

```

在readReponse中,通过解析数据，我们可以得到WatchedEvent对象，并将其压入EventThread的消息队列，等待分发

```java
void readResponse(ByteBuffer incomingBuffer) throws IOException {
            ByteBufferInputStream bbis = new ByteBufferInputStream(
                    incomingBuffer);
            BinaryInputArchive bbia = BinaryInputArchive.getArchive(bbis);
            ReplyHeader replyHdr = new ReplyHeader();
 
            replyHdr.deserialize(bbia, "header");
            if (replyHdr.getXid() == -2) {
                // -2 is the xid for pings
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got ping response for sessionid: 0x"
                            + Long.toHexString(sessionId)
                            + " after "
                            + ((System.nanoTime() - lastPingSentNs) / 1000000)
                            + "ms");
                }
                return;
            }
            if (replyHdr.getXid() == -4) {
                // -4 is the xid for AuthPacket              
                if(replyHdr.getErr() == KeeperException.Code.AUTHFAILED.intValue()) {
                    state = States.AUTH_FAILED;                   
                    eventThread.queueEvent( new WatchedEvent(Watcher.Event.EventType.None,
                            Watcher.Event.KeeperState.AuthFailed, null) );                                     
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got auth sessionid:0x"
                            + Long.toHexString(sessionId));
                }
                return;
            }
            if (replyHdr.getXid() == -1) {
                // -1 means notification
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got notification sessionid:0x"
                        + Long.toHexString(sessionId));
                }
                WatcherEvent event = new WatcherEvent();<br>                // 序列化
                event.deserialize(bbia, "response");
 
                // convert from a server path to a client path
                if (chrootPath != null) {
                    String serverPath = event.getPath();
                    if(serverPath.compareTo(chrootPath)==0)
                        event.setPath("/");
                    else if (serverPath.length() > chrootPath.length())
                        event.setPath(serverPath.substring(chrootPath.length()));
                    else {
                        LOG.warn("Got server path " + event.getPath()
                                + " which is too short for chroot path "
                                + chrootPath);
                    }
                }
 
                WatchedEvent we = new WatchedEvent(event);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got " + we + " for sessionid 0x"
                            + Long.toHexString(sessionId));
                }
                // 压入队列
                eventThread.queueEvent( we );
                return;
            }
 
            // If SASL authentication is currently in progress, construct and
            // send a response packet immediately, rather than queuing a
            // response as with other packets.
            if (clientTunneledAuthenticationInProgress()) {
                GetSASLRequest request = new GetSASLRequest();
                request.deserialize(bbia,"token");
                zooKeeperSaslClient.respondToServer(request.getToken(),
                  ClientCnxn.this);
                return;
            }
 
            Packet packet;
            synchronized (pendingQueue) {
                if (pendingQueue.size() == 0) {
                    throw new IOException("Nothing in the queue, but got "
                            + replyHdr.getXid());
                }
                packet = pendingQueue.remove();
            }
            /*
             * Since requests are processed in order, we better get a response
             * to the first request!
             */
            try {
                if (packet.requestHeader.getXid() != replyHdr.getXid()) {
                    packet.replyHeader.setErr(
                            KeeperException.Code.CONNECTIONLOSS.intValue());
                    throw new IOException("Xid out of order. Got Xid "
                            + replyHdr.getXid() + " with err " +
                            + replyHdr.getErr() +
                            " expected Xid "
                            + packet.requestHeader.getXid()
                            + " for a packet with details: "
                            + packet );
                }
 
                packet.replyHeader.setXid(replyHdr.getXid());
                packet.replyHeader.setErr(replyHdr.getErr());
                packet.replyHeader.setZxid(replyHdr.getZxid());
                if (replyHdr.getZxid() > 0) {
                    lastZxid = replyHdr.getZxid();
                }
                if (packet.response != null && replyHdr.getErr() == 0) {<br>                    // stat 信息返回
                    packet.response.deserialize(bbia, "response");
                }
 
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading reply sessionid:0x"
                            + Long.toHexString(sessionId) + ", packet:: " + packet);
                }
            } finally { //设置完
                finishPacket(packet);
            }
        }

```

在EventThread中通过processEvent对队列中的事件进行消费，并分发给不同的Watcher。设置完调用  finishPacket 进行Watcher 的注册：

```java
private void finishPacket(Packet p) {
        if (p.watchRegistration != null) {
            // 注册 事件注册到 zkwatchemanager 中
            p.watchRegistration.register(p.replyHeader.getErr());
        }
 
        if (p.cb == null) {
            synchronized (p) {
                p.finished = true;
                p.notifyAll();
            }
        } else {
            p.finished = true;
            eventThread.queuePacket(p);
        }
    }

```

watchRegistration，熟悉吗？在组装请求的时候，我们初始化了这个对象，把 watchRegistration 子 类 里 面 的Watcher 实 例 放 到 ZKWatchManager 的existsWatches 中存储起来。调用 ZKWatchManager 进行register ：

```java
public void register(int rc) {
            if (shouldAddWatch(rc)) {
                Map<String, Set<Watcher>> watches = getWatches(rc);
                synchronized(watches) {
                    Set<Watcher> watchers = watches.get(clientPath);
                    if (watchers == null) {
                        watchers = new HashSet<Watcher>();
                        watches.put(clientPath, watchers);
                    }
                    watchers.add(watcher);
                }
            }
        }

```

看看ZKWatchManager哪里调用注册：

```java
class ExistsWatchRegistration extends WatchRegistration {
       public ExistsWatchRegistration(Watcher watcher, String clientPath) {
           super(watcher, clientPath);
       }
 
       @Override
       protected Map<String, Set<Watcher>> getWatches(int rc) {
          //就是这里调用watchhManager的existWatchs这个Map里
           return rc == 0 ?  watchManager.dataWatches : watchManager.existWatches;
       }
 
       @Override
       protected boolean shouldAddWatch(int rc) {
           return rc == 0 || rc == KeeperException.Code.NONODE.intValue();
       }
   }

```

总 的 来 说 ， 当 使 用 ZooKeeper 构 造 方 法 或 者 使 用getData 、 exists 和 getChildren 三 个 接 口 来 向ZooKeeper 服务器注册 Watcher 的时候，首先将此消息传递给服务端，传递成功后，服务端会通知客户端，然后客户端将该路径和 Watcher 对应关系存储起来备用。

### 事件触发：

　　最后处理事件是我们 前边提到的 EventThread 线程去处理的，这里就不做详细分析了，最后执行任务的核心代码是:

```java
private void processEvent(Object event) {
          try {
              if (event instanceof WatcherSetEventPair) {
                  // each watcher will process the event
                  WatcherSetEventPair pair = (WatcherSetEventPair) event;<br>                  // 循环遍历   最后调用 process方法处理
                  for (Watcher watcher : pair.watchers) {
                      try {
                          watcher.process(pair.event);
                      } catch (Throwable t) {
                          LOG.error("Error while calling watcher ", t);
                      }
                  }
              }
}
```

----

