# TCP通信协议+HTTP+HTTPS协议
[TOC]
## 1 ：Http请求在网络中的请求过程
&nbsp;&nbsp;用户访问一个网址www.gupaoedu.com,经过解析成IP地址，通过这个ip地址建立一个连接，通过连接然后进行数据传输，整个连接基于HTTP协议进行传输，传输的内容也就是我们平时所说的**HTTP请求报文**。到了**传输层**之后，传输层会给HTTP报文添加**TCP头**，TCP头代表当前是个TCP协议，到了**网络层**之后，会再增加一个**IP头**，到达**数据链路层**，会再增加一个**MAC地址（网卡地址）**，到达**物理层**之后，会把我们传输的数据转换成**比特流**进行传输，也就是最后转换成0101比特流进行传输

![1537360065](https://ws1.sinaimg.cn/large/006tNc79ly1g28zgmpykyj30mu0cxdip.jpg)，当目的主机收到一个以太网数据帧的时候，数据就开始从协议栈中由底部向上升，同时去掉各层协议加上的报文首部。每层协议盒都要去检查报文首部的协议标识，以确定接收数据的上层协议，此过程叫做分用。

![1537574166](https://ws2.sinaimg.cn/large/006tNc79ly1g28zgakizmj30ns0csacc.jpg)

### 1.1：为什么有了MAC层还要走IP层呢？

&nbsp;&nbsp;&nbsp;可以简单的把MAC地址比作身份证号，也就是知道了身份证号就知道了这个人是唯一的，但是并不知道他的具体位置，因此需要能确定他的位置，这也就是IP地址的作用，通过IP寻址可以定位，也就是说全球任意位置都可以找到。

## 2：深入了解TCP/IP和UDP/IP通信协议

###  2.1：什么是协议

&nbsp;&nbsp;协议相当于两个需要通过网络通信的程序达成的一种约定，它规定了报文的交换方式和包含的意义。比如（HTTP）为了解决在服务器之间传递超文本对象的问题，这些超文本对象在服务器中创建和存储，并由 Web 浏览器进行可视化，完成用户对远程内容的感知和体验.

### 2.2: 什么是IP协议

&nbsp;&nbsp;T C P 和 U D P 是两种最为著名的传输层协议，他们都是使用 I P 作为网络层协议。IP 协议提供了一组数据报文服务，每组分组报文都是由网络独立处理和分发，就像寄送快递包裹一样，为了实现这个功能，每个 IP 报文必须包含一个目的地址的字段；就像我们寄送快递都需要写明收件人信息，但是和我们寄送快递一样，也可能会出现包裹丢失问题，所以 IP 协议只是一个“尽力而为”的协议，在网络传输过程中，可能会发生报文丢失、报文顺序打乱，重复发送的情况。IP 协议层之上的传输层，提供了两种可以选择的协议，TCP、UDP。这两种协议都是建立在 IP 层所提供的服务基础上，根据应用程序的不同需求选择不同方式的传输；

### 2.3：TCP/IP 协议
&nbsp;&nbsp;TCP 协议能够检测和恢复 IP 层提供的主机到主机的通信中可能发生的报文丢失、重复及其他错误。 TCP 提供了一个可信赖的字节流通道，这样应用程序就不需要考虑这些问题。同时， TCP 协议是一种面向连接的协议，在使用 TCP进行通信之前，两个应用程序之间需要建立一个 TCP 连接，而这个连接又涉及到两台电脑需要完成握手消息的交换。

### 2.4：UDP/IP协议
&nbsp;&nbsp;UDP 协议不会对 IP 层产生的错误进行修复，而是简单的扩展了 IP 协议“尽力而为”的数据报文服务，使他能够在应用程序之间工作，而不是在主机之间工作，因此使用 UDP协议必须要考虑到报文丢失，顺序混乱的问题。

### 2.5：TCP是如何做到可靠传输的
#### 2.5.1 建立可靠的连接
&nbsp;&nbsp;由于 TCP 协议是一种可信的传输协议，所以在传输之前，需要通过三次握手建立一个连接，所谓的三次握手，就是在建立 TCP 链接时，需要客户端和服务端总共发送 3个包来确认连接的建立： ![1537574874](https://ws2.sinaimg.cn/large/006tNc79ly1g28zh0qvkbj30mw0g10u9.jpg)2.5.2 TCP 四次挥手协议

&nbsp;&nbsp;四次挥手表示 TCP 断开连接的时候,需要客户端和服务端总共发送 4 个包以确认连接的断开； 客户端或服务器均可主动发起挥手动作(因为 TCP 是一个全双工协议)，在socket 编程中，任何一方执行 close() 操作即可产生挥手操作： ![1537575120](https://ws2.sinaimg.cn/large/006tNc79ly1g28zhccjyrj30lb0h2jt3.jpg)
#### 2.5.3 为什么连接3次握手，关闭却是4次？
&nbsp;&nbsp;三次握手是因为因为当 Server 端收到 Client 端的 SYN 连接请求报文后，可以直接发送 SYN+ACK 报文。其中 ACK报文是用来应答的， SYN 报文是用来同步的。但是关闭连接时，当 Server 端收到 FIN 报文时，很可能并不会立即关闭 SOCKET（因为可能还有消息没处理完），所以只能先回复一个 ACK 报文，告诉 Client 端， "你发的 FIN 报文我收到了"。只有等到我 Server 端所有的报文都发送完了，我才能发送 FIN 报文，因此不能一起发送。故需要四步握手。


## 3：数据传输过程的流量控制和确认机制
&nbsp;&nbsp;建立可靠连接以后， 就开始进行数据传输了。 在通信过程中，最重要的是数据包，也就是协议传输的数据。如果数据的传送与接收过程当中出现收方来不及接收的情况，这时就需要对发方进行控制以免数据丢失。利用滑动窗口机制可以很方便的在 TCP 连接上实现对发送方的流量控制。 TCP 的窗口单位是字节，不是报文段，发送方的发送窗口不能超过接收方给出的接收窗口的数值。
### 3.1 滑动窗口协议
&nbsp;&nbsp;滑动窗口（Sliding window）是一种流量控制技术。早期的网络通信中，通信双方不会考虑网络的拥挤情况直接发送数据。由于大家不知道网络拥塞状况，同时发送数据，导致中间节点阻塞掉包，谁也发不了数据，所以就有了滑动窗口机制来解决此问题； 发送和接受方都会维护一个数据帧的序列，这个序列被称作窗口：![1537576589](https://ws2.sinaimg.cn/large/006tNc79ly1g28zhn7qhuj30kb06mmyn.jpg)
&nbsp;&nbsp;发送和接受方都会维护一个数据帧的序列，这个序列被称作窗口。发送方的窗口大小由接受方确定，目的在于控制发送速度，以免接受方的缓存不够大，而导致溢出，同时控制流量也可以避免网络拥塞。下面图中的4,5,6 号数据帧已经被发送出去，但是未收到关联的ACK， 7,8,9 帧则是等待发送。可以看出发送端的窗口大小为 6，这是由接受端告知的。此时如果发送端收到 4 号ACK，则窗口的左边缘向右收缩，窗口的右边缘则向右扩展，此时窗口就向前“滑动了”，即数据帧 10 也可以被发送.

### 3.2 发送窗口
&nbsp;&nbsp;就是发送端允许连续发送的幀的序号表。发送端可以不等待应答而连续发送的最大幀数称为发送窗口的尺寸。
### 3.3 接收窗口
&nbsp;&nbsp;接收方允许接收的幀的序号表，凡落在 接收窗口内的幀，接收方都必须处理，落在接收窗口外的幀被丢弃。接收方每次允许接收的幀数称为接收窗口的尺寸。
### 3.4 在线演示
&nbsp;&nbsp;[在线演示地址](https://media.pearsoncmg.com/aw/ecs_kurose_compnetwork_7/cw/content/interactiveanimations/selective-repeat-protocol/index.html)

## 4：基于java自身技术实现TCP/UDP通信
### 4.1 基于Socket
  客户端：
```java
public class ClientSocketDemo {
    public static void main(String[] args) throws IOException {
        Socket socket = null;
        try{
            socket = new Socket("127.0.0.1",8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            out.println("hello");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(socket != null){
                socket.close();
            }
        }
    }
}
```
  服务端：
```java
public class SeverSocketDemo {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;

        BufferedReader bufferedReader = null;
        try {
            serverSocket = new ServerSocket(8080);
            //等待客户端连接
            Socket socket = serverSocket.accept();
            //连接过来之后会收到一个数据流
            bufferedReader = new BufferedReader
                    (new InputStreamReader(socket.getInputStream()));

        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(serverSocket != null){
                serverSocket.close();
            }
        }
    }
}

```
---------
### 4.2 基于UDP
  客户端：
```java
public class UdpClientDemo {
    public static void main(String[] args) throws IOException {
        InetAddress address = InetAddress.getByName("localhost");
        byte[] sendData = "hello".getBytes();
        DatagramPacket  sendPacket = new DatagramPacket(sendData,sendData.length,address,8080);
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(sendPacket);
    }
}
```
  服务端：
```java
public class UdpServerDemo {
    public static void main(String[] args) throws IOException {
        //创建服务，接收一个数据包
        DatagramSocket datagramSocket = new DatagramSocket(8080);
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        datagramSocket.receive(receivePacket);
        System.out.println(new String(receiveData));
    }
}

```
-----

## 5：多任务处理以及优化

### 5.1 BIO通信的性能问题
![1537578442](https://ws2.sinaimg.cn/large/006tNc79ly1g28zhzyd1mj30nn0fu434.jpg)
  我们发现 TCP 响应服务器一次只能处理一个客户端请求， 当一个客户端向一个已经被其他客户端占用的服务器发送连接请求时， 虽然在连接建立后可以向服务端发送数据， 但是在服务端处理完之前的请求之前， 却不会对新的客户端做出响应， 这种类型的服务器称为“迭代服务器”。迭代服务器是按照顺序处理客户端请求， 也就是服务端必须要处理完前一个请求才能对下一个客户端的请求进行响应。 但是在实际应用中， 我们不能接收这样的处理方式。所以我们需要一种方法可以独立处理每一个连接， 并且他们之间不会相互干扰。 而 Java 提供的多线程技术刚好满足这个需求， 这个机制使得服务器能够方便处理多个客户端的请求。

### 5.2 TCP 通信过程
  我们发现上图 TCP 响应服务器一次只能处理一个客户端请求， 当一个客户端向一个已经被其他客户端占用的服务器发送连接请求时， 虽然在连接建立后可以向服务端发送数据， 但是在服务端处理完之前的请求之前， 却不会对新的客户端做出响应， 这种类型的服务器称为“迭代服务器”。迭代服务器是按照顺序处理客户端请求， 也就是服务端必须要处理完前一个请求才能对下一个客户端的请求进行响应。 但是在实际应用中， 我们不能接收这样的处理方式。所以我们需要一种方法可以独立处理每一个连接， 并且他们之间不会相互干扰。 而 Java 提供的多线程技术刚好满足这个需求， 这个机制使得服务器能够方便处理多个客户端的请求。
![1537578833](https://ws2.sinaimg.cn/large/006tNc79ly1g28zi5qjpbj30kg074t9j.jpg)
Socket 的接收缓冲区被 TCP 用来缓存网络上收到的数据，一直保存到应用进程读走为止。如果应用进程一直没有读取，那么 Buffer 满了以后，出现的情况是：通知对端 TCP协议中的窗口关闭，保证 TCP 接收缓冲区不会移除，保证了 TCP 是可靠传输的。如果对方无视窗口大小发出了超过窗口大小的数据，那么接收方会把这些数据丢弃。

### 5.3 如何提高非阻塞性能
  非阻塞要解决的就是 I/O 线程与 Socket 解耦的问题， 因此， 它引入了事件机制来达到解耦的目的。 我们可以认为NIO 底层中存在一个 I/O 调度线程， 它不断的扫描每个Socket 的缓冲区， 当发现写入缓冲区为空的时候， 它会产生一个 Socket 可写事件， 此时程序就可以把数据写入到 Socket 中。 如果一次写不完， 就等待下一次的可写事件通知； 反之， 当发现缓冲区里有数据的时候， 它会产生一个 Socket 可读事件， 程序收到这个通知事件就可以从Socket 读取数据了。将在后续讲解NIO。

----
>以上内容均来自于咕泡学院整理而来

