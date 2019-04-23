

Kafka是一个开源的，分布式的，高吞吐量的消息系统。随着Kafka的版本迭代，日趋成熟。大家对它的使用也逐步从日志系统衍生到其他关键业务领域。特别是其超高吞吐量的特性，在互联网领域，使用越来越广泛，生态系统也越来的完善。同时，其设计思路也是其他消息中间件重要的设计参考。

Kafka原先的开发初衷是构建一个处理海量日志的框架，基于高吞吐量为第一原则，所以它对消息的可靠性以及消息的持久化机制考虑的并不是特别的完善。0.8版本后，陆续加入了一些复制、应答和故障转移等相关机制以后，才可以让我们在其他关键性业务中使用。

Kafka的运行架构如下图，各组件之间通过TCP协议通信：

![image-20190423075830765](https://ws2.sinaimg.cn/large/006tNc79ly1g2c8ml2o19j30eq09omxq.jpg)

#### **Topic：**

主题，或者说是一类消息。类似于RabbitMQ中的queue。可以理解为一个队列。

#### **Broker：**

一个Kafka服务称之为Broker。Kafka可以集群部署，每一个Kafka部署就是一个Broker。

#### **Producer & Consumer：**

生产者和消费者。一般消息系统都有生产者和消费者的概念。生产者产生消息，即把消息放入Topic中，而消费者则从Topic中获取消息处理。一个生产者可以向多个Topic发送消息，一个消费者也可以同时从几个Topic接收消息。同样的，一个Topic也可以被多个消费者来接收消息。

#### **Partition：**

分区，或者说分组。分组是Kafka提升吞吐量的一个关键设计。这样可以让消费者多线程并行接收消息。创建Topic时可指定Parition数量。一个Topic可以分为多个Partition，也可以只有一个Partition。每一个Partition是一个有序的，不可变的消息序列。每一个消息在各自的Partition中有唯一的ID。这些ID是有序的。称之为offset，offset在不同的Partition中是可以重复的，但是在一个Partition中是不可能重复的。越大的offset的消息是最新的。Kafka只保证在每个Partition中的消息是有序的，就会带来一个问题，即如果一个Consumer在不同的Partition中获取消息，那么消息的顺序也许是和Producer发送到Kafka中的消息的顺序是不一致的。这个在后续会讨论。

![image-20190423075855049](https://ws3.sinaimg.cn/large/006tNc79ly1g2c8n01kx4j30nu0f8jsz.jpg)

如果是多Partition，生产者在把消息放到Topic中时，可以决定放到哪一个Patition。这个可以使用简单的轮训方法，也可以使用一些Hash算法。

一个Topic的多个Partition可以分布式部署在不同的Server上，一个Kafka集群。配置项为：num.partitions，默认是1。每一个Partition也可以在Broker上复制多分，用来做容错。详细信息见下面创建Topic一节。

#### **Consumer Group：**

顾名思义，定义了一组消费者。一般来说消息中间件都有两种模式：队列模式和发布订阅模式。队列模式及每一个消息都会给其中一个消费者，而发布订阅模式则是每个消息都广播给所有的消费者。Kafka就是使用了Consumer Group来实现了这两种模式。

如果所有的消费者都是同一个Consumer Group的话，就是队列模式，每个消息都会负载均衡的分配到所有的消费者。

如果所有的消息者都在不同的Consumer Group的话，就是发布订阅模式，每个消费者都会得到这个消息。

下图是一个Topic，配置了4个Patition，分布在2个Broker上。由于有2个Consumer Group，Group A和Group B都可以得到P0-P3的所有消息，是一个订阅发布模式。两个Group中的Consumer则负载均衡的接收了这个Topic的消息。如果Group中的Consumer的总线程数量超过了Partition的数量，则会出现空闲状态。

![image-20190423075915345](https://ws1.sinaimg.cn/large/006tNc79ly1g2c8ncsdqvj30q60es0v2.jpg)

#### **Zookeeper：**

Kafka的运行依赖于Zookeeper。Topic、Consumer、Patition、Broker等注册信息都存储在ZooKeeper中。

 

#### **消息的持久化：**

Kafka可以通过配置时间和大小来持久化所有的消息，不管是否被消费（消费者收掉）。举例来说，如果消息保留被配置为1天，那么，消息就会在磁盘保留一天的时间，也就是说，一天以内，任意消费这个消息。一天以后，这个消息就会被删除。保留多少时间就取决于业务和磁盘的大小。

Kafka主要有两种方式：时间和大小。在Broker中的配置参数为：

log.retention.bytes：最多保留的文件字节大小。默认-1。

log.retention.hours：最多保留的时间，小时。优先级最低。默认168。

log.retention.minutes：最多保留的时间，分钟。如果为空，则看log.retention.hours。默认null。

log.retention.ms：最多保留的时间，毫秒。如果为空，则看log.retention.minutes。默认null。

#### 创建Topic：

通过命令创建topic：

**bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic haoxy1**

partitions：这个topic的partition的数量。

replication-factor：每个partition的副本个数。任意将每一个分区复制到n个broker上。

这个命令就是创建一个topic：haoxy1，只有1个partition，并且这个分区会部署在一个broker，但是具体哪个broker可以通过如下命令查看：

**bin/kafka-topics.sh --describ --zookeeper localhost:2181 --topic haoxy1**

展示如下：

![image-20190423075939346](https://ws1.sinaimg.cn/large/006tNc79ly1g2c8nrprgmj315k038t9t.jpg)

第一行的摘要信息。

第二行开始是详细信息，所以是缩进的格式，如果这个topic有10个Partition，那么就有10行。

Leader：每一个分区都有一个broker为Leader，它负责该分区内的所有读写操作，其他Leader被动的复制Leader broker。如果Leader broker 挂了，那么其他broker中的一个将自动成为该分区的新Leader。本例子只有1个复制，Leader的Partition在Broker1上面。

Replicas：副本在Broker1上面。

Isr：当前有效的副本在Broker1上面。

 

再来创建一个多副本的Topic：

**bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 5 --topic haoxy2**

如图：

![image-20190423075958270](https://ws4.sinaimg.cn/large/006tNc79ly1g2c8o3lmatj31du06wwkz.jpg)

因为我有3个Broker：0,1,2。每一个Partition都有2个Replicas。分别在2个Broker上。

----

