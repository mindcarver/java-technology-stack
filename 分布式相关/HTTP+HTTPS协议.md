[TOC]
## 1:HTTP协议
### 1.1 HTTP协议的组成
  通过fillder抓包工具去抓取请求，然后看到如下的请求数据和响应数据。分为两部分，一个是客户端的请求信息，一个是服务端的响应信息。

| request：                                                    |
| ------------------------------------------------------------ |
| POST https://re.csdn.net/csdnbi HTTP/1.1<br><font color="#FF0000">方法            url/uri                            协议的版本号 1.1</font><br/><br/>Host: re.csdn.net<br/><font color="#FF0000">请求的host地址</font>><br/><br/>Connection: keep-alive<br><font color="#FF0000">连接方式表示：长连接[keep-alive详细介绍](https://www.cnblogs.com/freefish12/p/5394876.html) </font><br/>Content-Length: 167 <br><font color="#FF0000">内容长度<br/><br/> </font>Accept: */*<br/><font color="#FF0000"> 告诉WEB服务器自己接受什么介质类型，*/* 表示任何类型，type/* 表示该类型下的所有子类型，type/sub-type。</font><br/>Origin: https://www.csdn.net<br/>User-Agent: Mozilla/5.0 (Windows NT 10.0;WOW64)<br/><font color="#FF0000"> 表明自己是哪种浏览器</font><br/>AppleWebKit/537.36 (KHTML, like Gecko)<br/>Chrome/66.0.3359.23 Safari/537.36<br/><br/>Content-Type: text/plain;charset=UTF-8<br/><font color="#FF0000"> WEB 服务器告诉浏览器自己响应的对象的类型。</font><br/>Referer: https://www.csdn.net/<br/><font color="#FF0000">浏览器向 WEB 服务器表明自己是从哪个 网页/URL 获得/点击 当前请求中的网址/URL。<br/> </font>Accept-Encoding: gzip, deflate, br<br/>Accept-Language: zh-CN,zh;q=0.9<br/>Cookie: uuid_tt_dd=10_19119862890-1514946902631-786149<br/>-----------------------------------------------------------------------------------------<br/>[{"headers":{"component":"enterprise","datatype":"re","version":"v1"},"body":"{\"re\":\"ref=-&<br/>mtp=4&mod=ad_popu_131&con=ad_content_2961%2Cad_order_731&uid=-&ck=-\"}"}]<br/>[关于请求头部分的详细介绍可以参考这篇文章](https://www.cnblogs.com/jiangxiaobo/p/5499488.html) |

response:  

```

HTTP/1.1 200 OK<br/> 协议版本号 响应状态码 状态码对应的原因
Server: openresty 
Date: Sun, 27 May 2018 12:08:44 GMT 
Transfer-Encoding: chunked 
Connection: keep-alive 
Keep-Alive: timeout=20 
Access-Control-Allow-Origin: https://www.csdn.net 
Access-Control-Allow-Methods: GET, POST, OPTIONS 
Access-Control-Allow-Credentials: true 
Access-Control-Allow-Headers: DNT,XCustomHeader,Keep-Alive,User-Agent,X-RequestedWith,If-Modified-Since,Cache-Control,ContentType,body
Http响应头分析参考这篇文章 :https://www.cnblogs.com/zxtceq/p/7154543.html 
```



### 1.1.1 URL
  URL(Uniform Resource Locator) 地址用于描述一个网络上的资源，基本格式:http://www.gupaoedu.com:80/java/index.html?name=mic#headschema://host[:port#]/path/.../?[url-params]#[ query-string]

|      scheme| 指定应用层使用的协议(例如： http, https, ftp) |
| ---- | ---- |
| host | HTTP 服务器的 IP 地址或者域名                                |
| port# | HTTP 服务器的默认端口是 80，这种情况下端口号可以省略，如果使用别的端口必须指明 |
| path | 访问资源的路径 |
| query-string | 查询字符串 |
| # | 片段标识符（使用片段标识符通常可标记出已获取资源中的子资源（文档内的某个位置）） |

### 1.1.2 URI 
  每个 web 服务器资源都有一个名字，这样客户端就可以根据这个名字来找到对应的资源，这个资源称之为（统一资源标识符）总的来说： URI 是用一个字符串来表示互联网上的某一个资源。而 URL表示资源的地点（互联网所在的位置）.

### 1.1.3 HTTP Method
  HTTP 发起的每个请求，都需要告诉告诉服务器要执行什么动作，那么这个动作就是前面报文中看到的【method】。 http 协议中提供了多个方法，不同方法的使用场景也也不一样

|GET|一般是用于客户端发送一个 URI 地址去获取服务端的资源（一般用于查询操作）|
|----|---|
|POST|一般用户客户端传输一个实体给到服务端，让服务端去保存（一般用于创建操作）|
|PUT|向服务器发送数据，一般用于更新数据的操作|
|HEAD|用于向服务端发起一个查询请求获取 head 信息，比如获取index.html 的有效性、最近更新时间等。|
|DELETE|客户端发起一个 Delete 请求要求服务端把某个数据删除（一般用于删除操作）|
|OPTIONS|查询指定 URI 支持的方法类型（get/post）|

### 1.1.4 HTTP 协议的特点
  HTTP 协议是无状态的，什么是无状态呢？就是说 HTTP 协议本身不会对请求和响应之间的通信状态做保存。
### 1.1.5 如何实现有状态的协议
  Http 协议中引入了 cookie 技术，用来解决 http 协议无状态的问题。通过在请求和响应报文中写入 Cookie 信息来控制客户端的状态； Cookie会根据从服务器端发送的响应报文内的一个叫做 Set-Cookie 的首部字段信息，通知客户端保存 Cookie。当下次客户端再往该服务器发送请求时，客户端会自动在请求报文中加入 Cookie 值后发送出去。在基于 tomcat 这类的 jsp/servlet 容器中，会提供 session 这样的机制来保存服务端的对象状态。那么整个状态协议的流程就是这样的：
![1537587378](https://ws3.sinaimg.cn/large/006tNc79ly1g28z9te6mwj30s90cgagy.jpg)

### 1.1.6 HTTP 协议的缺陷

1. 通信过程中是使用明文，内容可能会被窃听 
2. 不验证通信双方的身份 
3. 无法验证报文的完整性，报文可能被篡改 
### 1.1.7 HTTPS简介
  由于 HTTP 协议通信的不安全性，所以人们为了防止信息在传输过程中遭到泄漏或者篡改，就想出来对传输通道进行加密的方式 https。https 是一种加密的超文本传输协议，它与 HTTP 在协议差异在于对数据传输的过程中， https 对数据做了完全加密。由于 http 协议或者 https协议都是处于 TCP 传输层之上，同时网络协议又是一个分层的结构，所以在 tcp 协议层之上增加了一层SSL（Secure Socket Layer，安全层）或者 TLS（Transport Layer Security） 安全层传输协议组合使用用于构造加密通道；
![1537595331](https://ws4.sinaimg.cn/large/006tNc79ly1g28za37srej30q60cidic.jpg)

### 1.1.8 HTTPS 实现原理

1. 客户端发起请求(Client Hello 包) 
   - 三次握手，建立 TCP 连接 
   - 支持的协议版本(TLS/SSL) 
   - 客户端生成的随机数 client.random，后续用于生成“对话密钥 “
   - 客户端支持的加密算法 
   - sessionid，用于保持同一个会话（如果客户端与服务器费尽周折建立了一个 HTTPS 链接，刚建完就断了，也太可惜） 
2. 服务端收到请求，然后响应（Server Hello） 
   - 确认加密通道协议版本 
   - 服务端生成的随机数 server.random，后续用于生成“对话密钥” 
   - **确认使用的加密算法（用于后续的握手消息进行签名防止篡改）** 
   - 服务器证书（CA 机构颁发给服务端的证书） 
3. 客户端收到证书进行验证 
   - 验证证书是否是上级 CA 签发的, 在验证证书的时候，浏览器会调用系统的证书管理器接口对证书路径中的所有证书一级一级的进行验证，只有路径中所有的证书都是受信的，整个验证的结果才是受信 
   - 服务端返回的证书中会包含证书的有效期，可以通过失效日期来验证 证书是否过期 
   - 验证证书是否被吊销了 
   - 前面我们知道 CA 机构在签发证书的时候，都会使用自己的私钥对证书进行签名证书里的签名算法字段 sha256RSA 表示 CA 机构使用 sha256对证书进行摘要，然后使用 RSA 算法对摘要进行私钥签名，而我们也知道 RSA 算法中，使用私钥签名之后，只有公钥才能进行验签。 
   - 浏览器使用内置在操作系统上的CA机构的公钥对服务器的证书进行验签。确定这个证书是不是由正规的机构颁发。验签之后得知 CA 机构使用 sha256 进行证书摘要，然后客户端再使用sha256 对证书内容进行一次摘要，如果得到的值和服务端返回的证书验签之后的摘要相同，表示证书没有被修改过 
   - 验证通过后，就会显示绿色的安全字样 
   - 客户端生成随机数， 验证通过之后，客户端会生成一个随机数pre-master secret ， 客户端根据之前的 ： Client.random +sever.random + pre-master 生成对称密钥然后使用证书中的公钥进行加密， 同时利用前面协商好的加密算法，将握手消息取HASH 值，然后用“随机数加密“握手消息+握手消息 HASH 值（签名）”然后传递给服务器端；(**在这里之所以要取握手消息的 HASH值，主要是把握手消息做一个签名，用于验证握手消息在传输过程中没有被篡改过。** ) 
4. 服务端接收随机数 
   - 服务端收到客户端的加密数据以后，用自己的私钥对密文进行解密。然后得到 client.random/server.random/pre-master secret. ,再用随机数密码 解密 握手消息与 HASH 值，并与传过来的HASH 值做对比确认是否一致 
   - 然后用随机密码加密一段握手消息(握手消息+握手消息的HASH 值 )给客户端 
5. 客户端接收消息 
   - 客户端用随机数解密并计算握手消息的 HASH，如果与服务端发来的 HASH 一致，此时握手过程结束， 
   - 之后所有的通信数据将由之前交互过程中生成的 pre mastersecret / client.random/server.random 通过算法得出 sessionKey，作为后续交互过程中的对称密钥 

-----

