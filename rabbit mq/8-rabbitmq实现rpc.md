实现RPC用到了IBasicProperties的两个属性：

1.replyto：通常用来设置一个回调队列

2.correlationId：用来关联请求(request)和其调用RPC之后的回复(response)。

Server代码:

```java
using System;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;
using System.Text;
 
class RPCServer
{
    public static void Main()
    {
        var factory = new ConnectionFactory() { HostName = "localhost" };
        using (var connection = factory.CreateConnection())
        using (var channel = connection.CreateModel())
        {
            //创建RPC队列-队列名rpc_queue，非持久化，非排他，非自动删除
            channel.QueueDeclare(queue: "rpc_queue", durable: false, exclusive: false, autoDelete: false, arguments: null);
            //Qos 预期大小：0；预取数：1；非全局
            channel.BasicQos(0, 1, false);
            //创建消费者
            var consumer = new EventingBasicConsumer(channel);
            //消费消息：队列名rpc_queue，非自动反馈确认；消费者consumer
            channel.BasicConsume(queue: "rpc_queue", autoAck: false, consumer: consumer);
            Console.WriteLine(" [x] Awaiting RPC requests");
            //添加监听：异步执行程序
            consumer.Received += (model, ea) =>
            {
                //消息内容
                var body = ea.Body;
                //传来的属性
                IBasicProperties props = ea.BasicProperties;
                //RPC反馈的属性-channel创建
                IBasicProperties replyProps = channel.CreateBasicProperties();
                //RPC 相关标识
                replyProps.CorrelationId = props.CorrelationId;
 
                byte[] responseBytes = null;
                try
                {
                    //消息处理
                    responseBytes = ServerExe(ea.Body);
                }
                catch (Exception e)
                {
                    Console.WriteLine(" [.] " + e.Message);
                    responseBytes = Encoding.UTF8.GetBytes("");
                }
                finally
                {
                    //发送消息-服务端反馈给客户端的消息
                    channel.BasicPublish(exchange: "", routingKey: props.ReplyTo, basicProperties: replyProps, body: responseBytes);
                    //反馈确认--服务端接收的消息的反馈
                    channel.BasicAck(deliveryTag: ea.DeliveryTag, multiple: false);
                }
            };
 
            Console.WriteLine(" Press [enter] to exit.");
            Console.ReadLine();
        }
    }
 
 
    private static byte[] ServerExe(byte[] body)
    {
        string response = null;
        var message = Encoding.UTF8.GetString(body);
        int n = int.Parse(message);
        Console.WriteLine(" [.] fib({0})", message);
        response = fib(n).ToString();
        return Encoding.UTF8.GetBytes(response);
    }
 
    /// <summary>
    /// Assumes only valid positive integer input.
    /// Don't expect this one to work for big numbers, and it's probably the slowest recursive implementation possible.
    /// </summary>
    private static int fib(int n)
    {
        if (n == 0 || n == 1)
        {
            return n;
        }
        return fib(n - 1) + fib(n - 2);
    }
}
```

Client代码

```java
using System;
using System.Collections.Concurrent;
using System.Text;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;
 
public class RpcClient
{
    private readonly IConnection connection;
    private readonly IModel channel;
    private readonly string replyQueueName;
    private readonly EventingBasicConsumer consumer;
    private readonly BlockingCollection<string> respQueue = new BlockingCollection<string>();
    private readonly IBasicProperties props;
 
    public RpcClient()
    {
        var factory = new ConnectionFactory() { HostName = "localhost" };
 
        connection = factory.CreateConnection();
        channel = connection.CreateModel();
        replyQueueName = channel.QueueDeclare().QueueName;
        consumer = new EventingBasicConsumer(channel);
 
        props = channel.CreateBasicProperties();
        var correlationId = Guid.NewGuid().ToString();
        props.CorrelationId = correlationId;
        props.ReplyTo = replyQueueName;
        //接收RPC服务端反馈的信息
        consumer.Received += (model, ea) =>
        {
            var body = ea.Body;
            var response = Encoding.UTF8.GetString(body);
            if (ea.BasicProperties.CorrelationId == correlationId)
            {
                respQueue.Add(response);
            }
        };
    }
 
    public string Call(string message)
    {
        var messageBytes = Encoding.UTF8.GetBytes(message);
        channel.BasicPublish(
            exchange: "",
            routingKey: "rpc_queue",
            basicProperties: props,
            body: messageBytes);
 
        channel.BasicConsume(
            consumer: consumer,
            queue: replyQueueName,
            autoAck: true);
 
        return respQueue.Take(); ;
    }
 
    public void Close()
    {
        connection.Close();
    }
}
 
public class Rpc
{
    public static void Main()
    {
        var rpcClient = new RpcClient();
 
        Console.WriteLine(" [x] Requesting fib(30)");
        var response = rpcClient.Call("30");
 
        Console.WriteLine(" [.] Got '{0}'", response);
        rpcClient.Close();
    }
}
```

主要步骤：

![image-20190422221226527](https://ws1.sinaimg.cn/large/006tNc79ly1g2bros44ajj310c0ck0zh.jpg)

1.当客户端启动的时候，创建一个匿名的灰掉队列；

2.客户端为RPC请求设置两个属性，reploy和correlationId；

3.请求发送到rep_queue队列中

4.RPC服务端监听rpc_queue队列中的请求，当请求到来时，服务端会处理并把带有结果的消息发送给客户端，接收的队列就是relyTo设定的回调队列；

5.客户端监听回调队列，当有消息时，检查correlationId属性，如果匹配即是结果
