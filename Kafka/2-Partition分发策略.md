kafka版本0.8.2.1

Java客户端版本0.9.0.0

 

为了更好的实现负载均衡和消息的顺序性，Kafka Producer可以通过分发策略发送给指定的Partition。Kafka保证在partition中的消息是有序的。Kafka Java客户端有默认的Partitioner。实现如下：

```java
public int partition(ProducerRecord<byte[], byte[]> record, Cluster cluster) {
        List partitions = cluster.partitionsForTopic(record.topic());
        int numPartitions = partitions.size();
        if(record.partition() != null) {
            if(record.partition().intValue() >= 0 && record.partition().intValue() < numPartitions) {
                return record.partition().intValue();
            } else {
                throw new IllegalArgumentException("Invalid partition given with record: " + record.partition() + " is not in the range [0..." + numPartitions + "].");
            }
        } else if(record.key() == null) {
            int nextValue = this.counter.getAndIncrement();
            List availablePartitions = cluster.availablePartitionsForTopic(record.topic());
            if(availablePartitions.size() > 0) {
                int part = Utils.abs(nextValue) % availablePartitions.size();
                return ((PartitionInfo)availablePartitions.get(part)).partition();
            } else {
                return Utils.abs(nextValue) % numPartitions;
            }
        } else {
            return Utils.abs(Utils.murmur2((byte[])record.key())) % numPartitions;
        }
    }
```

从源码可以看出，首先获取topic的所有Patition，如果客户端不指定Patition，也没有指定Key的话，使用自增长的数字取余数的方式实现指定的Partition。这样Kafka将平均的向Partition中生产数据。测试代码如下：

Producer：

```java
String topic = "haoxy1";

            int i = 0;

            Properties props = new Properties();
            props.put("bootstrap.servers", "10.23.22.237:9092,10.23.22.238:9092,10.23.22.239:9092");
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);

            System.out.println("partitions count " + producer.partitionsFor(topic));
            while(true) {
                String msg = "test"+i++;

                ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>(topic, msg);
                producer.send(producerRecord);
                System.out.println("send " + msg);
                Thread.sleep(5000);
            }
```

Consumer：

```java
String topic = "haoxy1";

            Properties props = new Properties();
            props.put("zookeeper.connect", "10.23.22.237:2181,10.23.22.238:2181,10.23.22.239:2181");
            props.put("group.id", "cg.nick");
            props.put("consumer.id", "c.nick");

            Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
            topicCountMap.put(topic, 3);
            ConsumerConfig consumerConfig = new ConsumerConfig(props);
            ConsumerConnector consumer = Consumer.createJavaConsumerConnector(consumerConfig);
            Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
            List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
            ExecutorService executor = Executors.newFixedThreadPool(3);
            for (final KafkaStream stream : streams) {
                executor.submit(new Runnable() {
                    public void run() {
                        ConsumerIterator<byte[], byte[]> it = stream.iterator();
                        while (it.hasNext()) {
                            MessageAndMetadata<byte[], byte[]> mm = it.next();
                            System.out.println(String.format("partition = %s, offset = %d, key = %s, value = %s", mm.partition(), mm.offset(), mm.key(), new String(mm.message())));
                        }
                    }
                });
            }
```

从测试结果结果看出，是平均分配的：

```java
partition = 1, offset = 416, key = null, value = test9
partition = 0, offset = 386, key = null, value = test10
partition = 2, offset = 454, key = null, value = test11
partition = 1, offset = 417, key = null, value = test12
partition = 0, offset = 387, key = null, value = test13
partition = 2, offset = 455, key = null, value = test14
partition = 1, offset = 418, key = null, value = test15
partition = 0, offset = 388, key = null, value = test16
```

如果想要控制发送的partition，则有两种方式，一种是指定partition，另一种就是根据Key自己写算法。继承Partitioner接口，实现其partition方法。并且配置启动参数

```
props.put("partitioner.class","TestPartitioner")。
```

比如需要实现

key=’aaa’ 的都进partition 0

key=’bbb’ 的都进partition 1

key=’bbb’ 的都进partition 2

```java
public class TestPartitioner implements Partitioner {
    public int partition(String s, Object key, byte[] bytes, Object o1, byte[] bytes1, Cluster cluster) {
        if (key.toString().equals("aaa"))
            return 0;
        else if (key.toString().equals("bbb"))
            return 1;
        else if (key.toString().equals("ccc"))
            return 2;
        else return 0;
    }

    public void close() {

    }

    public void configure(Map<String, ?> map) {

    }
}
```

测试结果：

```java
partition = 0, offset = 438, key = aaa, value = test32
partition = 1, offset = 448, key = bbb, value = test33
partition = 2, offset = 486, key = ccc, value = test34
partition = 0, offset = 439, key = aaa, value = test35
partition = 1, offset = 449, key = bbb, value = test36
partition = 2, offset = 487, key = ccc, value = test37
partition = 0, offset = 440, key = aaa, value = test38
partition = 1, offset = 450, key = bbb, value = test39
partition = 2, offset = 488, key = ccc, value = test40
partition = 0, offset = 441, key = aaa, value = test41
partition = 1, offset = 451, key = bbb, value = test42
partition = 2, offset = 489, key = ccc, value = test43
partition = 0, offset = 442, key = aaa, value = test44
```

如果你使用的不是Java的客户端，是javaapi下面的Producer的话，自定义的分区类需要实现kafka.producer.Partitioner，并且有构造函数。

```java
public class TestPartitioner implements Partitioner {
    public TestPartitioner (VerifiableProperties props) {

    }
    public int partition(Object o, int i) {
        if (o.toString().equals("aaa"))
            return 0;
        else if (o.toString().equals("bbb"))
            return 1;
        else if (o.toString().equals("ccc"))
            return 2;
        else return 0;
    }
}
```

