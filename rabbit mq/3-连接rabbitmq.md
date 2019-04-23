1.连接到RabbitMQ到代码清单

```java
                ConnectionFactory factory = new ConnectionFactory();
                factory.HostName = IP_ADDRESS;
                factory.Port = PORT;
                factory.UserName = USER_NAME;
                factory.Password = PASSWORD;
                IConnection con = factory.CreateConnection();
                IModel channel = con.CreateModel();
```

依次设置了factory的属性：IP地址、端口号、用户名、密码。

需要注意的是：Connection 可以创建多个Channel实例，但是Channel实例不能在线程之间共享，应用程序应该为每一个线程创建一个Channel。

2.channel和 con 都有一个Isopen属性  用来判断是否处于开启状态

```java
  //channel和 con 都有一个Isopen属性  用来判断是否处于开启状态
                bool channelisOpen = channel.IsOpen;
                bool conisOpen = con.IsOpen;
```

如果使用Channel的时候其已经处于关闭状态，程序会抛出一个异常。异常类型是：AlreadyClosedException