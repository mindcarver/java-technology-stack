# Zookeeper安装

## 本地模式安装部署

```
安装前准备
```

- 安装jdk
- 通过SecureCRT工具拷贝zookeeper到linux系统下
- 修改tar包权限：chmod u+x zookeeper-3.4.10.tar.gz
- 解压到指定目录： [luo@hadoop102 software]$ tar -zxvf zookeeper-3.4.10.tar.gz -C /opt/module/

```
配置修改
```

将/opt/module/zookeeper-3.4.10/conf这个路径下的zoo_sample.cfg修改为zoo.cfg；

进入zoo.cfg文件：`vim zoo.cfg`，修改dataDir路径为：`dataDir=/opt/module/zookeeper-3.4.10/zkData`

在/opt/module/zookeeper-3.4.10/这个目录上创建zkData文件夹：`mkdir zkData`

```
操作zookeeper
```

- 启动zookeeper：`[luo@hadoop102 zookeeper-3.4.10]$ bin/zkServer.sh start`
- 查看进程是否启动：`[luo@hadoop102 zookeeper-3.4.10]$ jps （4020 Jps，4001 QuorumPeerMain）`
- 查看状态：`[luo@hadoop102 zookeeper-3.4.10]$ bin/zkServer.sh status`
- 启动客户端：`[luo@hadoop102 zookeeper-3.4.10]$ bin/zkCli.sh`
- 退出客户端：`[zk: localhost:2181(CONNECTED) 0] quit`
- 停止zookeeper：`[luo@hadoop102 zookeeper-3.4.10]$ bin/zkServer.sh stop`

## 配置参数说明

zoo.cfg 文件中参数含义如下

- tickTime=2000：通信心跳数：Zookeeper服务器心跳时间，单位毫秒Zookeeper使用的基本时间，服务器之间或客户端与服务器之间维持心跳的时间间隔，也就是每个tickTime时间就会发送一个心跳，时间单位为毫秒。它用于心跳机制，并且设置最小的session超时时间为两倍心跳时间。(session的最小超时时间是2*tickTime)
- initLimit=10：LF初始通信时限：集群中的follower跟随者服务器(F)与leader领导者服务器(L)之间初始连接时能容忍的最多心跳数（tickTime的数量），用它来限定集群中的Zookeeper服务器连接到Leader的时限。投票选举新leader的初始化时间Follower在启动过程中，会从Leader同步所有最新数据，然后确定自己能够对外服务的起始状态。Leader允许F在initLimit时间内完成这个工作
- syncLimit=5：LF同步通信时限：集群中Leader与Follower之间的最大响应时间单位，假如响应超过syncLimit * tickTime，Leader认为Follwer死掉，从服务器列表中删除Follwer。在运行过程中，Leader负责与ZK集群中所有机器进行通信，例如通过一些心跳检测机制，来检测机器的存活状态。如果L发出心跳包在syncLimit之后，还没有从F那收到响应，那么就认为这个F已经不在线了
- `dataDir：数据文件目录+数据持久化路径`：保存内存数据库快照信息的位置，如果没有其他说明，更新的事务日志也保存到数据库。
- `clientPort=2181：客户端连接端口`：监听客户端连接的端口