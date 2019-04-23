两种TTL：如果两种都设置了，按照TTL小的那个处理

1.通过队列属性设置，通过队列发送出去的消息都遵循这个TTL；

2.通过对消息本身单独设置

第一种，一旦消息过期直接丢弃；第二种，即使消息过期，也不一定立刻丢弃，因为只有当消息被投递的时候，才能判断该消息是否过期。

**设置队列TTL：**

如果不设置TTL，则表示此消息不会过期，如果将TTL设置为0，则表示除非此时可以直接将消息投递到消费者，否则该消息立即被丢弃。

```java
                ConnectionFactory factory = new ConnectionFactory();
                factory.HostName = IP_ADDRESS;
                factory.Port = PORT;
                factory.UserName = USER_NAME;
                factory.Password = PASSWORD;
                con = factory.CreateConnection();
                channel = con.CreateModel();
                channel.ExchangeDeclare(EXCHANGE_NAME, "topic", true, false, null);
 
                Dictionary<string, object> agres = new Dictionary<string, object>();
                //消息TTL01.Queue设置
                agres.Add("x-message-ttl", 6000);
                //队列TTL设置：1800000ms
                agres.Add("x-expires", 1800000);
                channel.QueueDeclare(QUEUE_NAME, true, false, false, agres);
 
 
                channel.QueueBind(QUEUE_NAME, EXCHANGE_NAME, BINDING_KEY, null);//channel.ExchangeBind()
                string message = "Hello Word!";
                var body = Encoding.UTF8.GetBytes(message);
                var properties = channel.CreateBasicProperties();
                properties.Persistent = true;
```

**设置消息TTL：**

```java
     ConnectionFactory factory = new ConnectionFactory();
                factory.HostName = IP_ADDRESS;
                factory.Port = PORT;
                factory.UserName = USER_NAME;
                factory.Password = PASSWORD;
                con = factory.CreateConnection();
                channel = con.CreateModel();
                channel.ExchangeDeclare(EXCHANGE_NAME, "topic", true, false, null);
 
                //Dictionary<string, object> agres = new Dictionary<string, object>();
                ////消息TTL01.Queue设置
                //agres.Add("x-message-ttl", 6000);
                ////队列TTL设置：1800000ms
                //agres.Add("x-expires", 1800000);
                //channel.QueueDeclare(QUEUE_NAME, true, false, false, agres);
 
 
                channel.QueueBind(QUEUE_NAME, EXCHANGE_NAME, BINDING_KEY, null);//channel.ExchangeBind()
                string message = "Hello Word!";
                var body = Encoding.UTF8.GetBytes(message);
                var properties = channel.CreateBasicProperties();
                properties.Persistent = true;
 
                //消息TTL02.每条消息设置设置,如果两个都设置了，则按照TTL小的那个
                properties.Expiration = "6000";//TTL = 6000ms
                channel.BasicPublish(EXCHANGE_NAME, ROUTING_KEY, properties, body);
```

