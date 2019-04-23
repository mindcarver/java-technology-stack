## 快速启动

　　Dubbo采用全Spring配置方式，透明化接入应用，对应用没有任何API侵入，只需用Spring加载Dubbo的配置即可，Dubbo基于Spring的Schema扩展进行加载。

　　如果不想使用Spring配置，而希望通过API的方式进行调用（不推荐），请参见：[API配置](http://www.cnblogs.com/ClassNotFoundException/p/6973361.html)

### 服务提供者

　　完整安装步骤，请参见：[示例提供者安装](http://www.cnblogs.com/ClassNotFoundException/p/6973394.html)

　　定义服务接口: (该接口需单独打包，在服务提供方和消费方共享)

　　Class ： DemoService.java

```java
package com.alibaba.dubbo.demo;
 
public interface DemoService {
 
    String sayHello(String name);
 
}
```

在服务提供方实现接口：(对服务消费方隐藏实现)

　　Class ： DemoServiceImpl.java

```java
package com.alibaba.dubbo.demo.provider;
 
import com.alibaba.dubbo.demo.DemoService;
 
public class DemoServiceImpl implements DemoService {
 
    public String sayHello(String name) {
        return "Hello " + name;
    }
 
}
```

用Spring配置声明暴露服务：

　　XML ： provider.xml

```java
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:dubbo="http://code.alibabatech.com/schema/dubbo"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
　　　　http://www.springframework.org/schema/beans/spring-beans.xsd
　　　　http://code.alibabatech.com/schema/dubbo
　　　　http://code.alibabatech.com/schema/dubbo/dubbo.xsd">
 
    <!-- 提供方应用信息，用于计算依赖关系 -->
    <dubbo:application name="hello-world-app"  />
 
    <!-- 使用multicast广播注册中心暴露服务地址 -->
    <dubbo:registry address="multicast://224.5.6.7:1234" />
 
    <!-- 用dubbo协议在20880端口暴露服务 -->
    <dubbo:protocol name="dubbo" port="20880" />
 
    <!-- 声明需要暴露的服务接口 -->
    <dubbo:service interface="com.alibaba.dubbo.demo.DemoService" ref="demoService" />
 
    <!-- 和本地bean一样实现服务 -->
    <bean id="demoService" class="com.alibaba.dubbo.demo.provider.DemoServiceImpl" />
 
</beans>
```

加载Spring配置：

　　Class ： Provider.java

```java
import org.springframework.context.support.ClassPathXmlApplicationContext;
 
public class Provider {
 
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"http://10.20.160.198/wiki/display/dubbo/provider.xml"});
        context.start();
 
        System.in.read(); // 按任意键退出
    }
 
}
```

### 服务消费者

　　完整安装步骤，请参见：[示例消费者安装](http://www.cnblogs.com/ClassNotFoundException/p/6973394.html)

　　通过Spring配置引用远程服务：

　　XML ： consumer.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:dubbo="http://code.alibabatech.com/schema/dubbo"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
　　　　http://www.springframework.org/schema/beans/spring-beans.xsd
　　　　http://code.alibabatech.com/schema/dubbo
　　　　http://code.alibabatech.com/schema/dubbo/dubbo.xsd">
 
    <!-- 消费方应用名，用于计算依赖关系，不是匹配条件，不要与提供方一样 -->
    <dubbo:application name="consumer-of-helloworld-app"  />
 
    <!-- 使用multicast广播注册中心暴露发现服务地址 -->
    <dubbo:registry address="multicast://224.5.6.7:1234" />
 
    <!-- 生成远程服务代理，可以和本地bean一样使用demoService -->
    <dubbo:reference id="demoService" interface="com.alibaba.dubbo.demo.DemoService" />
 
</beans>
```

加载Spring配置，并调用远程服务：(也可以使用IoC注入)

　　Class ： Consumer.java

```java
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.alibaba.dubbo.demo.DemoService;
 
public class Consumer {
 
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"http://10.20.160.198/wiki/display/dubbo/consumer.xml"});
        context.start();
 
        DemoService demoService = (DemoService)context.getBean("demoService"); // 获取远程服务代理
        String hello = demoService.sayHello("world"); // 执行远程方法
 
        System.out.println( hello ); // 显示调用结果
    }
 
}
```

### 依赖

### 必需依赖

- JDK1.5+

　　理论上Dubbo可以只依赖JDK，不依赖于任何三方库运行，只需配置使用JDK相关实现策略。

### 缺省依赖

通过mvn dependency:tree > dep.log命令分析，Dubbo缺省依赖以下三方库：

```
[INFO] +- com.alibaba:dubbo:jar:2.1.2:compile
[INFO] |  +- log4j:log4j:jar:1.2.16:compile 
[INFO] |  +- org.javassist:javassist:jar:3.15.0-GA:compile
[INFO] |  +- org.springframework:spring:jar:2.5.6.SEC03:compile
[INFO] |  +- commons-logging:commons-logging:jar:1.1.1:compile
[INFO] |  \- org.jboss.netty:netty:jar:3.2.5.Final:compile
```

这里所有依赖都是换照Dubbo缺省配置选的，这些缺省值是基于稳定性和性能考虑的。

- log4j.jar和commons-logging.jar日志输出包。
  - 可以直接去掉，dubbo本身的日志会自动切换为JDK的java.util.logging输出。
  - 但如果其它三方库比如spring.jar间接依赖commons-logging，则不能去掉。
- javassist.jar 字节码生成。
  - 如果<dubbo:provider proxy="jdk" />或<dubbo:consumer proxy="jdk" />，以及<dubbo:application compiler="jdk" />，则不需要。
- spring.jar 配置解析。
  - 如果用ServiceConfig和ReferenceConfig的API调用，则不需要。
- netty.jar 网络传输。
  - 如果<dubbo:protocol server="mina"/>或<dubbo:protocol server="grizzly"/>，则换成mina.jar或grizzly.jar。
  - 如果<protocol name="rmi"/>，则不需要。

### 可选依赖

以下依赖，在主动配置使用相应实现策略时用到，需自行加入依赖。

- mina: 1.1.7
- grizzly: 2.1.4
- httpclient: 4.1.2
- hessian_lite: 3.2.1-fixed
- xstream: 1.4.1
- fastjson: 1.1.8
- zookeeper: 3.3.3
- jedis: 2.0.0
- xmemcached: 1.3.6
- jfreechart: 1.0.13
- hessian: 4.0.7
- jetty: 6.1.26
- hibernate-validator: 4.2.0.Final
- zkclient: 0.1
- curator: 1.1.10
- cxf: 2.6.1
- thrift: 0.8.0

JEE:

- servlet: 2.5
- bsf: 3.1
- validation-api: 1.0.0.GA
- jcache: 0.4

-----

### 启动时检查

　　Dubbo缺省会在启动时检查依赖的服务是否可用，不可用时会抛出异常，阻止Spring初始化完成，以便上线时，能及早发现问题，默认check=true。

　　如果你的Spring容器是懒加载的，或者通过API编程延迟引用服务，请关闭check，否则服务临时不可用时，会抛出异常，拿到null引用，如果check=false，总是会返回引用，当服务恢复时，能自动连上。

可以通过check="false"关闭检查，比如，测试时，有些服务不关心，或者出现了循环依赖，必须有一方先启动。

关闭某个服务的启动时检查：(没有提供者时报错)

```
<dubbo:reference interface="com.foo.BarService" check="false" />
```

关闭所有服务的启动时检查：(没有提供者时报错)

```
<dubbo:consumer check="false" />
```

关闭注册中心启动时检查：(注册订阅失败时报错)

```
<dubbo:registry check="false" />
```

也可以用dubbo.properties配置：

```
dubbo.reference.com.foo.BarService.check=false
dubbo.reference.check=false
dubbo.consumer.check=false
dubbo.registry.check=false
```

也可以用-D参数：

```
java -Ddubbo.reference.com.foo.BarService.check=false
java -Ddubbo.reference.check=false
java -Ddubbo.consumer.check=false 
java -Ddubbo.registry.check=false
```

**注意区别**

- - dubbo.reference.check=false，强制改变所有reference的check值，就算配置中有声明，也会被覆盖。
  - dubbo.consumer.check=false，是设置check的缺省值，如果配置中有显式的声明，如：<dubbo:reference check="true"/>，不会受影响。
  - dubbo.registry.check=false，前面两个都是指订阅成功，但提供者列表是否为空是否报错，如果注册订阅失败时，也允许启动，需使用此选项，将在后台定时重试。

引用缺省是延迟初始化的，只有引用被注入到其它Bean，或被getBean()获取，才会初始化。
如果需要饥饿加载，即没有人引用也立即生成动态代理，可以配置：

```
<dubbo:reference interface="com.foo.BarService" init="true" />
```