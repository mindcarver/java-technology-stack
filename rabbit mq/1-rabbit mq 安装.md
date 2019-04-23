1. 检查有没有安装JAVA

> java -version

如果没有安装，则先安装java

![image-20190422214515993](https://ws2.sinaimg.cn/large/006tNc79ly1g2bqwkq0y5j312o058thf.jpg)

---

2. 官网下载 erlang 和 rabbitMQ

 下载erlang可以手动去官网下载：http://www.erlang.org/

  rabbitMQ官网：http://www.rabbitmq.com/

---

3. 安装erlang

   将下载好的otp_src_20.2.tar.gz文件copy到ubuntu中

   ```
    #tar zxvf otp_src_20.2.tar.gz（解压）
   
    #cd otp_src_20.2/ （进入到解压好的文件夹中）
   ```

   编译安装

   ```
   #./configure  --prefix=/opt/erlang（指定安装目录）
   
   #  make（编译）
   
   #  make install（安装）
   
   ```

   更新环境变量

   ```
   #vim /etc/profile
   ```

   在最后一行加上

   ```
   exportPATH=/opt/erlang/bin:$PATH
   ```

   保存退出后

   ```
   source/etc/profile
   ```

   命令行中输入erl看是否安装成功:

   ![image-20190422214753710](https://ws3.sinaimg.cn/large/006tNc79ly1g2bqz8iz9rj313y05qgtc.jpg)

------

4. 安装rabbitMQ

```
echo 'deb http://www.rabbitmq.com/debian/ testing main'| sudo tee /etc/apt/sources.list.d/rabbitmq.list

wget -O- https://www.rabbitmq.com/rabbitmq-release-signing-key.asc| sudo apt-key add -

sudo apt-get update

sudo apt-get install rabbitmq-server
```

通过上篇介绍的命令invoke-rc.drabbitmq-server stop/start/etc我们看见进程是启动的

![image-20190422214916535](https://ws2.sinaimg.cn/large/006tNc79ly1g2br0p9zycj31cx0u04qq.jpg)

打开管理页面 

> sudo rabbitmq-plugins enable rabbitmq_management

查看安装的插件 

> sudo rabbitmqctl list_users

查看用户 
sudo rabbitmqctl list_users

新增管理员用户 

> sudo rabbitmqctl add_user admin admin 
> sudo rabbitmqctl set_user_tags admin administrator

用刚设置的账户登录管理页面 
http://127.0.0.1:15672

![image-20190422215000815](https://ws4.sinaimg.cn/large/006tNc79ly1g2br1geq83j31hi0ts4jv.jpg)