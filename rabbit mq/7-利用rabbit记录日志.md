本例中，我们将添加一个功能 - 我们将只能订阅一部分消息。例如，我们只能将重要的错误消息引导到日志文件（以节省磁盘空间），同时仍然能够在控制台上打印所有日志消息。

C1代表记录日志到日志文件的消费者，C2代表在控制台打印出来日志的消费者，通过不同的routingKey绑定到不同的队列。

**一个Exchange可以通过同一个routingKey来bind多个队列**

**也可以只bind一个队列**

接下来我们介绍一个记录日志的sample：

![image-20190422221008620](https://ws3.sinaimg.cn/large/006tNc79ly1g2brmdx9hxj30q609q78r.jpg)

我们将使用这个模型我们的日志系统. 我们用direct类型的exchange.我们将提供日志severity作为路由的关键routing key. 这样接收脚本将能够选择严重性要收到.

日志记录发起方 ：

```java
using System;
using System.Linq;
using RabbitMQ.Client;
using System.Text;
 
class EmitLogDirect
{
    public static void Main(string[] args)
    {
        var factory = new ConnectionFactory() { HostName = "localhost" };
        using(var connection = factory.CreateConnection())
        using(var channel = connection.CreateModel())
        {
            channel.ExchangeDeclare(exchange: "direct_logs",
                                    type: "direct");
 
            var severity = (args.Length > 0) ? args[0] : "info";
            var message = (args.Length > 1)
                          ? string.Join(" ", args.Skip( 1 ).ToArray())
                          : "Hello World!";
            var body = Encoding.UTF8.GetBytes(message);
            channel.BasicPublish(exchange: "direct_logs",
                                 routingKey: severity,
                                 basicProperties: null,
                                 body: body);
            Console.WriteLine(" [x] Sent '{0}':'{1}'", severity, message);
        }
 
        Console.WriteLine(" Press [enter] to exit.");
        Console.ReadLine();
    }
}
```

日志记录接收方 :

```java
using System;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;
using System.Text;
 
class ReceiveLogsDirect
{
    public static void Main(string[] args)
    {
        var factory = new ConnectionFactory() { HostName = "localhost" };
        using(var connection = factory.CreateConnection())
        using(var channel = connection.CreateModel())
        {
            channel.ExchangeDeclare(exchange: "direct_logs",
                                    type: "direct");
            var queueName = channel.QueueDeclare().QueueName;
 
            if(args.Length < 1)
            {
                Console.Error.WriteLine("Usage: {0} [info] [warning] [error]",
                                        Environment.GetCommandLineArgs()[0]);
                Console.WriteLine(" Press [enter] to exit.");
                Console.ReadLine();
                Environment.ExitCode = 1;
                return;
            }
            foreach(var severity in args)
            {
                channel.QueueBind(queue: queueName,
                                  exchange: "direct_logs",
                                  routingKey: severity);
            }
            Console.WriteLine(" [*] Waiting for messages.");
            var consumer = new EventingBasicConsumer(channel);
            consumer.Received += (model, ea) =>
            {
                var body = ea.Body;
                var message = Encoding.UTF8.GetString(body);
                var routingKey = ea.RoutingKey;
                Console.WriteLine(" [x] Received '{0}':'{1}'",
                                  routingKey, message);
            };
            channel.BasicConsume(queue: queueName,
                                 autoAck: true,
                                 consumer: consumer);
 
            Console.WriteLine(" Press [enter] to exit.");
            Console.ReadLine();
        }
    }
}
```

