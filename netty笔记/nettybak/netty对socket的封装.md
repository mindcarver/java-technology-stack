#netty启动

![image-20190427194004752](/Users/codecarver/Library/Application Support/typora-user-images/image-20190427194004752.png)

----

## netty基本组件

- nioeventLoop（重要，两种连接）
- channel（连接的封装）
- bytebuf
- Pipeline(业务逻辑处理链)
- channelHandler（业务逻辑封装）
- unsafe(跟channel相关的底层操作)

## netty服务端启动

 相见demo（）

过程：

1. 创建服务端Channel

   ![image-20190427203740370](https://ws1.sinaimg.cn/large/006tNc79gy1g2hh249fdnj312c0ioqds.jpg)

   ![image-20190427204150388](https://ws2.sinaimg.cn/large/006tNc79gy1g2hh6bc798j311o0ok7og.jpg)

2. 初始化服务端Channel

3. 注册Selector

4. 端口绑定

## 服务端Channel的创建

问题：

1. 服务端的socket在哪里初始化
2. 在哪里accept连接

##服务端channel的初始化

![image-20190427204731368](https://ws3.sinaimg.cn/large/006tNc79gy1g2hhc0quxxj311y0o8wtq.jpg)

![image-20190427205101459](https://ws4.sinaimg.cn/large/006tNc79gy1g2hhft1d0yj31220nqh43.jpg)

-----

## 注册Selector

![image-20190427205516482](https://ws3.sinaimg.cn/large/006tNc79gy1g2hhkfsvrhj313k0qwe0q.jpg)

## 端口绑定

![image-20190427210920122](/Users/codecarver/Library/Application Support/typora-user-images/image-20190427210920122.png)

# NioEventLoop

关于NioEventLoop的三个面试题：

- 默认情况下，netty服务端起多少线程，何时启动
- netty如何解决jdk空轮训bug
- netty如何保证异步串行无锁化

---

## nio的创建

![image-20190427211654518](/Users/codecarver/Library/Application Support/typora-user-images/image-20190427211654518.png)

- SelectorProvider （用来维护selector）

## ThreadPerTaskThread

![image-20190428105802804](https://ws1.sinaimg.cn/large/006tNc79gy1g2i5x25h1sj315o0hsq9j.jpg)

## 创建nioeventloop线程

![image-20190428105915348](https://ws4.sinaimg.cn/large/006tNc79gy1g2i5y7w80gj30y00nkaft.jpg)

## 创建线程选择器

ChooserFactory.newChooser

![image-20190428110409924](/Users/codecarver/Library/Application Support/typora-user-images/image-20190428110409924.png)

![image-20190428110539633](/Users/codecarver/Library/Application Support/typora-user-images/image-20190428110539633.png)

![image-20190428110602773](https://ws3.sinaimg.cn/large/006tNc79gy1g2i65am57vj316o0pan92.jpg)

---

## nioeventLoop启动触发器

![image-20190428121421100](https://ws1.sinaimg.cn/large/006tNc79gy1g2i84ds5shj311s0hcwlk.jpg)

![image-20190428121536645](/Users/codecarver/Library/Application Support/typora-user-images/image-20190428121536645.png)

-----

## nioeventloop执行

![image-20190428124402531](/Users/codecarver/Library/Application Support/typora-user-images/image-20190428124402531.png)

---

## select方法执行逻辑

![image-20190428124504440](/Users/codecarver/Library/Application Support/typora-user-images/image-20190428124504440.png)

-----

## 检测IO事件

![image-20190428124610566](/Users/codecarver/Library/Application Support/typora-user-images/image-20190428124610566.png)



## reactor线程任务的执行

![image-20190428124711712](/Users/codecarver/Library/Application Support/typora-user-images/image-20190428124711712.png)