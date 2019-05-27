## 简介

Zookeeper是一个开源的分布式的，为分布式应用提供协调服务的Apache项目。

特点

- Zookeeper：一个领导者（leader），多个跟随者（follower）组成的集群。
- Leader负责进行投票的发起和决议，更新系统状态
- Follower用于接收客户请求并向客户端返回结果，在选举Leader过程中参与投票
- 集群中只要有半数以上节点存活，Zookeeper集群就能正常服务。
- 全局数据一致：每个server保存一份相同的数据副本，client无论连接到哪个server，数据都是一致的。
- 更新请求顺序进行，来自同一个client的更新请求按其发送顺序依次执行。
- 数据更新原子性，一次数据更新要么成功，要么失败。
- 实时性，在一定时间范围内，client能读到最新数据。

## 数据结构

ZooKeeper数据模型的结构与Unix文件系统很类似，整体上可以看作是一棵树，每个节点称做一个ZNode。每一个ZNode默认能够存储1MB的数据，每个ZNode都可以通过其路径唯一标识

## 应用场景

提供的服务包括：统一命名服务、统一配置管理、统一集群管理、服务器节点动态上下线、软负载均衡等。

统一命名服务：

![image-20190421203943592](https://ws2.sinaimg.cn/large/006tNc79ly1g2aje1vhrhj31cm0oowmv.jpg)

---

统一配置管理：

![image-20190421204011081](https://ws3.sinaimg.cn/large/006tNc79ly1g2ajehiya9j31dw0ngako.jpg)

----

统一集群管理：

![image-20190421204032041](https://ws4.sinaimg.cn/large/006tNc79ly1g2ajev18urj31ek0myajl.jpg)

----

服务器动态上下线：

![image-20190421204100241](https://ws4.sinaimg.cn/large/006tNc79ly1g2ajfbznayj31e40q0gx1.jpg)

----

软负载均衡：

![image-20190421204126850](https://ws1.sinaimg.cn/large/006tNc79ly1g2ajfstxmmj315k0pc0xs.jpg)