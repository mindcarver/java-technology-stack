# Spring aop源码分析

## 一：spring aop 使用介绍

### 1.aop概念

AOP，**面向切面编程**，是一种编程思想，它允许程序员对**横切关注点或横切典型的职责分界线的行为（例如日志和事务管理）**进行模块化。**它将那些影响多个类的行为（多处可重用的业务逻辑代码）封装到可重用的模块中（切面）。**

AOP 和 IOC 是补充性的技术（**增强核心业务逻辑以外的功能**），它们都运用**模块化方式**解决企业应用程序开发中的复杂问题。在典型的面向对象开发方式中， 可能要将日志记录语句放在所有方法和 Java 类中才能实现日志功能。 在 AOP方式中， 可以反过来**将日志服务模块化**， 并**以声明的方式将它们应用到需要日志的组件上**。 当然， 优势就是 Java 类（指项目的业务逻辑代码）**不需要知道日志服务的存在，** **也不需要考虑相关的代码**。 所以， 用 Spring AOP 编写的应用程序代码是**松散耦合**的。

**AOP** 编程的常用场景有：Authentication权限认证、日志、Transctions Manager事务、懒加载、 Error Handler错误跟踪（异常捕获机制）、Cache缓存。

### 2.aop相关术语

-  连接点（Joinpoint）:**重复执行的代码**， 也叫做关注点代码；是指那些被拦截到的点。因为Spring只支持方法类型的连接点，所以在Spring中连接点指的就是被拦截到的方法，实际上连接点还可以是字段或者构造器
- 切面（aspect）：是切入点（JoinPoint拦截哪些方法）和通知（Advice具体增强的操作 ）的概念结合，所以对应的可以将这两点最终封装成切面类。详细的说“切面”应该是指：目标对象中的目标方法（连接点）的附加额外的增强操作（即核心业务逻辑之外的业务逻辑）的集合，以及何时，何地执行。
- 切入点（pointcut）：起到拦截的作用，要对哪些连接点进行拦截的定义。
- 通知（advice）：所谓通知指的就是指拦截到连接点之后要执行的代码，通知分为前置、后置、异常、最终、环绕通知五类。
- 目标对象：代理的目标对象。
- 织入（weave）：将切面应用到目标对象并导致代理对象创建的过程。

### 3.Spring对aop的支持

#### 基于接口配置

Spring1.2是基于接口实现的，我们先定义接口userService以及其实现类userServiceImpl

```java
public interface UserService {
    void printUser(String name, int age);
    User getUser();
}
public class UserServiceImpl implements UserService {
    @Override
    public void printUser(String name, int age) {
        System.out.println("executing method: name is:" + name + ", age is :" + age);
    }
    @Override
    public User getUser() {
        return new User("codecarver", 100);
    }
}
```

再来定义两个通知（advice）：

```java
// 前置通知
public class LogBeforeAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("prepare to excute method: " + method.getName());
    }
}

// 后置通知
public class LogAfterAdvice implements AfterReturningAdvice {
    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target)
            throws Throwable {
        System.out.println("end of method execution ");
    }
}
```

xml配置：

```xml

<bean id="userServiceImpl" class="com.codecarver.aoplecture.service.impl.UserServiceImpl"/>

<!--定义两个 advice-->
<bean id="logArgsAdvice" class="com.codecarver.aoplecture.aop_based_interface.LogBeforeAdvice"/>
<bean id="logResultAdvice" class="com.codecarver.aoplecture.aop_based_interface.LogAfterAdvice"/>

<bean id="userServiceProxy" class="org.springframework.aop.framework.ProxyFactoryBean">
  <!--代理的接口-->
  <property name="proxyInterfaces">
    <list>
      <value>com.codecarver.aoplecture.service.UserService</value>
    </list>
  </property>
  <!--代理的具体实现-->
  <property name="target" ref="userServiceImpl"/>

  <!--配置拦截器，这里可以配置 advice、advisor、interceptor-->
  <property name="interceptorNames">
    <list>
      <value>logArgsAdvice</value>
      <value>logResultAdvice</value>
    </list>
  </property>
</bean>

```

应用程序启动类：

```java
 public static void test_advice() {
        // 启动 Spring 的 IOC 容器
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring_advice.xml");

        // 这里需要userService的代理类：userServiceProxy
        UserService userService = (UserService) context.getBean("userServiceProxy");
        userService.printUser("codecarver", 100);
    }
```

输出：

> prepare to excute method: printUser
> executing method: name is:codecarver, age is :100
> end of method execution 

我们可以看到在方法执行的前后打印出了一句话，达到了我们拦截方法，增强方法的目的。但是这种方式有个问题，就是它会拦截所有的方法，那么我们如何指定某个方法被拦截呢，接下来就引出了advisor这个概念，advisor可以决定拦截哪些特定方法，主要就是看下xml的配置：

```xml-dtd
  <bean id="userServiceImpl" class="com.codecarver.aoplecture.service.impl.UserServiceImpl"/>
    <!--定义两个 advice-->
    <bean id="logBeforeAdvice" class="com.codecarver.aoplecture.aop_based_interface.LogBeforeAdvice"/>
    <bean id="logAfterAdvice" class="com.codecarver.aoplecture.aop_based_interface.LogAfterAdvice"/>

    <!--定义只拦截getUser方法的 advisor-->
    <bean id="logBeforeAdvisor" class="org.springframework.aop.support.NameMatchMethodPointcutAdvisor">
        <!--advisor 实例的内部会有一个 advice-->
        <property name="advice" ref="logBeforeAdvice" />
        <!--只拦截getUser方法-->
        <property name="mappedNames" value="getUser" />
    </bean>

    <bean id="logAfterAdvisor" class="org.springframework.aop.support.NameMatchMethodPointcutAdvisor">
        <!--advisor 实例的内部会有一个 advice-->
        <property name="advice" ref="logAfterAdvice" />
        <!--只拦截getUser方法-->
        <property name="mappedNames" value="getUser" />
    </bean>
    <bean id="userServiceProxy" class="org.springframework.aop.framework.ProxyFactoryBean">
        <!--代理的接口-->
        <property name="proxyInterfaces">
            <list>
                <value>com.codecarver.aoplecture.service.UserService</value>
            </list>
        </property>
        <!--代理的具体实现-->
        <property name="target" ref="userServiceImpl"/>

        <!--配置拦截器，这里可以配置 advice、advisor、interceptor-->
        <property name="interceptorNames">
            <list>
                <value>logBeforeAdvisor</value>
                <value>logAfterAdvisor</value>
            </list>
        </property>
    </bean>
```

通过advior,可以精确的控制哪一个通知应用到哪一个方法上，实现了更细粒度的控制。

但是上面还是存在一个问题，我们每次获取bean实例的时候都需要硬编码，我们可以通过autoproxy来自动注入。

看下修改后的XML文件：

```xml-dtd
  <bean id="userServiceImpl" class="com.codecarver.aoplecture.service.impl.UserServiceImpl"/>

    <!--定义两个 advice-->
    <bean id="logBeforeAdvice" class="com.codecarver.aoplecture.aop_based_interface.LogBeforeAdvice"/>
    <bean id="logAfterAdvice" class="com.codecarver.aoplecture.aop_based_interface.LogAfterAdvice"/>

    <bean class="org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator">
        <property name="interceptorNames">
            <list>
                <value>logBeforeAdvice</value>
                <value>logAfterAdvice</value>
            </list>
        </property>
        <!--*可拦截多个serviceImpl-->
        <property name="beanNames" value="*ServiceImpl" />
    </bean>
```

应用程序：

```java
 public static void test_beannameAutoProxy() {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring_beannameAutoProxy.xml");
        // 不再需要根据代理找 bean
        UserService userService = context.getBean(UserService.class);
        userService.getUser();
    }
```

不在需要通过硬编码去找代理类，十分方便。另外可以通过**BeanNameAutoProxyCreator**来拦截多个业务类，但是还有个更方便的就是**DefaultAdvisorAutoProxyCreator**，他们呢两者的区别就是：

> BeanNameAutoProxyCreator交由配置的advice来拦截处理
>
> DefaultAdvisorAutoProxyCreator让所有的advisor匹配方法，从而由advisor内部的advice来拦截处理

主要的变化就是XML的配置：

```xml-dtd
 <bean id="userServiceImpl" class="com.codecarver.aoplecture.service.impl.UserServiceImpl"/>

    <!--定义两个 advice-->
    <bean id="logBeforeAdvice" class="com.codecarver.aoplecture.aop_based_interface.LogBeforeAdvice"/>
    <bean id="logAfterAdvice" class="com.codecarver.aoplecture.aop_based_interface.LogAfterAdvice"/>

    <!--定义两个 advisor-->
    <!--记录 get* 方法的传参-->
    <bean id="logArgsAdvisor" class="org.springframework.aop.support.RegexpMethodPointcutAdvisor">
        <property name="advice" ref="logBeforeAdvice" />
        <property name="pattern" value="com.codecarver.aoplecture.service.*.get.*" />
    </bean>
    <!--记录 print* 的返回值-->
    <bean id="logResultAdvisor" class="org.springframework.aop.support.RegexpMethodPointcutAdvisor">
        <property name="advice" ref="logAfterAdvice" />
        <property name="pattern" value="com.codecarver.aoplecture.service.*.print.*" />
    </bean>

    <!--定义DefaultAdvisorAutoProxyCreator-->
    <bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator" />

```

我们可以自由的对任意业务类的任意方法的任意位置进行拦截，非常之灵活。

#### 基于注解@AspectJ配置

Spring 2.0 以后，引入了 @AspectJ 和 Schema-based 的两种配置方式，这里就介绍下@AspectJ怎么使用。

1. **开启** @AspectJ 的注解配置方式

   ```
   <aop:aspectj-autoproxy/>
   ```

   一旦开启了配置，所有使用 @Aspect 注解的 **bean** 都会被 Spring 当做**用来实现 AOP 的配置类**，也就是一个**Aspect**。

   ```java
   @Aspect
   public class testAspect{}
   ```

2. 配置**Pointcut**

   ```java
   @Pointcut("within(com.codecarver.aoplecture.service..*)")
   public void service(){}
   ```

   我们看到@Pointcut 中使用了 **within** 来正则匹配方法签名，指定所在类或者所在包下的方法，常用的还有：

   - execution：

     > ```java
     > @Pointcut("execution(* transfer(..))")
     > ```

   - @annotation

     > ```java
     > @Pointcut("execution(* transfer(..))")
     > ```

   - Bean(idOrNameOfBean)

     > ```java
     > @Pointcut("bean(*Service)")
     > ```

   有个比较好的方式就是定义个全局的切面类:

   ```java
   @Aspect
   public class GlobalAspect {
       // weblayer
       @Pointcut("within(com.codecarver.aoplecture.web..*)")
       public void WebLayer() {}
   
       // servicelayer
       @Pointcut("within(com.codecarver.aoplecture.service..*)")
       public void ServiceLayer() {}
   
       // daolayer
      @Pointcut("within(com.codecarver.aoplecture.dao..*)")
      public void inDataAccessLayer() {}
   
   }
   ```

   既然已经配置好了pointcut，知道了拦截哪些方法，那么接下来就要配置advice了。

3. 配置**advice**

   ```java
   @Aspect
   public class LogAfterAspect {
       @AfterReturning(pointcut = "com.codecarver.aoplecture.aop_based_aspectj.GlobalAspect.ServiceLayer()",
               returning = "result")
       public void logResult(Object result) {
           System.out.println("method executed");
       }
   }
   ```

到此为止程序已经可以运行了，主要就是要了解Pointcut、Advice 和 Aspect这几个概念，配置方式不是重点。接下来就要进入到源码分析了。

---

## 二：spring aop 源码分析

直接基于上面的接口配置代码进行分析。代码参见`spring-aop-lecture`.

首先我们需要再次从源码角度了解下aop相关术语。

### 1.aop术语以及实现

#### 连接点-joinpoint

连接点是指程序执行过程中的一些点，比如方法调用，异常处理等。在 Spring AOP 中，仅支持方法级别的连接点。我们来看下接口定义：

```java
package org.aopalliance.intercept;
public interface Joinpoint {
    /** 用于执行拦截器链中的下一个拦截器逻辑 */
    Object proceed() throws Throwable;
    Object getThis();
    AccessibleObject getStaticPart();

}
```

这个 Joinpoint 接口中，proceed 方法是核心，该方法用于执行拦截器逻辑。以`前置通知拦截器`为例。在执行目标方法前，该拦截器首先会执行前置通知逻辑，如果拦截器链中还有其他的拦截器，则继续调用下一个拦截器逻辑。直到拦截器链中没有其他的拦截器后，再去调用目标方法.其继承结构图如下：

![image-20190328164000967](https://ws1.sinaimg.cn/large/006tKfTcly1g1ill7rzmaj30eq0qsmyg.jpg)

-----

#### 切点-pointcut

起到拦截的作用，要对哪些连接点进行拦截的定义。

```java
public interface Pointcut {
    Pointcut TRUE = TruePointcut.INSTANCE;
		// 返回一个类型过滤器
    ClassFilter getClassFilter();
		// 返回一个方法匹配器 
    MethodMatcher getMethodMatcher();
}
```

Pointcut 接口中定义了两个接口，分别用于返回类型过滤器和方法匹配器。下面我们再来看一下类型过滤器和方法匹配器接口的定义：

```java
public interface ClassFilter {
    boolean matches(Class<?> clazz);
    ClassFilter TRUE = TrueClassFilter.INSTANCE;

}

public interface MethodMatcher {
    boolean matches(Method method, Class<?> targetClass);
    boolean matches(Method method, Class<?> targetClass, Object... args);
    boolean isRuntime();
    MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;
}
```

上面的两个接口均定义了 matches 方法，用户只要实现了 matches 方法，即可对连接点进行选择。在日常使用中，大家通常是用 AspectJ 表达式对连接点进行选择。Spring 中提供了一个 AspectJ 表达式切点类 - AspectJExpressionPointcut，下面我们来看一下这个类的继承体系图：

![image-20190328152711819](https://ws3.sinaimg.cn/large/006tKfTcly1g1ijhgwk57j311q0iqabk.jpg)



如上所示，这个类最终实现了 Pointcut、ClassFilter 和 MethodMatcher 接口，因此该类具备了通过 AspectJ 表达式对连接点进行选择的能力。

#### 通知-advice

通知 Advice 即我们定义的横切逻辑，如果说切点解决了通知在哪里调用的问题，那么现在还需要考虑了一个问题，即通知在何时被调用？Spring 中定义了以下几种通知类型：

- 前置通知（Before advice）- 在目标方便调用前执行通知
- 后置通知（After advice）- 在目标方法完成后执行通知
- 返回通知（After returning advice）- 在目标方法执行成功后，调用通知
- 异常通知（After throwing advice）- 在目标方法抛出异常后，执行通知
- 环绕通知（Around advice）- 在目标方法调用前后均可执行自定义逻辑

我们看下通知的接口定义：

```java
public interface Advice {
}
// 前置通知
public interface BeforeAdvice extends Advice {
}
// 前置通知
public interface MethodBeforeAdvice extends BeforeAdvice {
	void before(Method method, Object[] args, @Nullable Object target) throws Throwable;
}
```

可以看出通知的接口并没有什么，我们来看下它的类图：

![image-20190328164329729](https://ws3.sinaimg.cn/large/006tKfTcly1g1ilowaay4j30mq0g63zj.jpg)

-----

接下来就是切面（Aspect）了，通过切面我们可以整合Point cut和Advice。

#### 切面-Aspect

切面 Aspect 整合了切点和通知两个模块，切点解决了 where 问题，通知解决了 when 和 how 问题。切面把两者整合起来，就可以解决 对什么方法（where）在何时（when - 前置还是后置，或者环绕）执行什么样的横切逻辑（how）的三连发问题。在 AOP 中，切面只是一个概念，并没有一个具体的接口或类与此对应。不过 Spring 中倒是有一个接口的用途和切面很像，我们不妨了解一下，这个接口就是切点通知器PointcutAdvisor。我们先来看看这个接口的定义，如下：

```java
public interface Advisor {
    Advice getAdvice();
    boolean isPerInstance();
}
public interface PointcutAdvisor extends Advisor {
    Pointcut getPointcut();
}
```

那么Advisor怎么用上面也进行了说明，这里就不在赘述了。

#### 织入-Weaving

织入就是在切点的引导下，将通知逻辑插入到方法调用上，使得我们的通知逻辑在方法调用时得以执行。说完织入的概念，现在来说说 Spring 是通过何种方式将通知织入到目标方法上的。先来说说以何种方式进行织入，这个方式就是通过实现后置处理器 BeanPostProcessor 接口。该接口是 Spring 提供的一个拓展接口，通过实现该接口，用户可在 bean 初始化前后做一些自定义操作。那 Spring 是在何时进行织入操作的呢？答案是在 bean 初始化完成后，即 bean 执行完初始化方法（init-method）。Spring通过切点对 bean 类中的方法进行匹配。若匹配成功，则会为该 bean 生成代理对象，并将代理对象返回给容器。容器向后置处理器输入 bean 对象，得到 bean 对象的代理，这样就完成了织入过程。

### 2. aop入口

既然我们知道了aop的入口是BeanPostProcessor，我们就来看看相关的源码吧：

```java
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
        implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    
    @Override
    /** bean 初始化后置处理方法 */
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean != null) {
            Object cacheKey = getCacheKey(bean.getClass(), beanName);
            if (!this.earlyProxyReferences.contains(cacheKey)) {
                // 如果需要，为 bean 生成代理对象
                return wrapIfNecessary(bean, beanName, cacheKey);
            }
        }
        return bean;
    }
    
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        if (beanName != null && this.targetSourcedBeans.contains(beanName)) {
            return bean;
        }
        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;
        }

        /*
         * 如果是基础设施类（Pointcut、Advice、Advisor 等接口的实现类），或是应该跳过的类，
         * 则不应该生成代理，此时直接返回 bean
         */ 
        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
            // 将 <cacheKey, FALSE> 键值对放入缓存中，供上面的 if 分支使用
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }

        // 为目标 bean 查找合适的通知器
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
        /*
         * 若 specificInterceptors != null，即 specificInterceptors != DO_NOT_PROXY，
         * 则为 bean 生成代理对象，否则直接返回 bean
         */ 
        if (specificInterceptors != DO_NOT_PROXY) {
            this.advisedBeans.put(cacheKey, Boolean.TRUE);
            // 创建代理
            Object proxy = createProxy(
                    bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
            this.proxyTypes.put(cacheKey, proxy.getClass());
            /*
             * 返回代理对象，此时 IOC 容器输入 bean，得到 proxy。此时，
             * beanName 对应的 bean 是代理对象，而非原始的 bean
             */ 
            return proxy;
        }

        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        // specificInterceptors = null，直接返回 bean
        return bean;
    }
}
```

大致的过程：

1. 若 bean 是 AOP 基础设施类型，则直接返回
2. 为 bean 查找合适的通知器
3. 如果通知器数组不为空，则为 bean 生成代理对象，并返回该对象
4. 若数组为空，则返回原始 bean

### 3.筛选合适的通知器

在向目标 bean 中织入通知之前，我们先要为 bean 筛选出合适的通知器（通知器持有通知）。如何筛选呢？方式由很多，比如我们可以通过正则表达式匹配方法名，当然更多的时候用的是 AspectJ 表达式进行匹配。那下面我们就来看一下使用 AspectJ 表达式筛选通知器的过程，如下：

```java
protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource targetSource) {
    // 查找合适的通知器
    List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
    if (advisors.isEmpty()) {
        return DO_NOT_PROXY;
    }
    return advisors.toArray();
}

protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
    // 查找所有的通知器
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    /*
     * 筛选可应用在 beanClass 上的 Advisor，通过 ClassFilter 和 MethodMatcher
     * 对目标类和方法进行匹配
     */
    List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
    // 拓展操作
    extendAdvisors(eligibleAdvisors);
    if (!eligibleAdvisors.isEmpty()) {
        eligibleAdvisors = sortAdvisors(eligibleAdvisors);
    }
    return eligibleAdvisors;
}
```

如上，Spring 先查询出所有的通知器，然后再调用 findAdvisorsThatCanApply 对通知器进行筛选。在下面几节中，我将分别对 findCandidateAdvisors 和 findAdvisorsThatCanApply 两个方法进行分析.

#### findCandidateAdvisors

先来看一下 AbstractAdvisorAutoProxyCreator 中 findCandidateAdvisors 方法的定义，如下：

```java
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

    private BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;
    
    //...

    protected List<Advisor> findCandidateAdvisors() {
        return this.advisorRetrievalHelper.findAdvisorBeans();
    }

    //...
}
```

从上面的源码中可以看出，AbstractAdvisorAutoProxyCreator 中的 findCandidateAdvisors 是个空壳方法，所有逻辑封装在了一个 BeanFactoryAdvisorRetrievalHelper 的 findAdvisorBeans 方法中。这里大家可以仔细看一下类名 BeanFactoryAdvisorRetrievalHelper 和方法 findAdvisorBeans，两个名字其实已经描述出他们的职责了。BeanFactoryAdvisorRetrievalHelper 可以理解为`从 bean 容器中获取 Advisor 的帮助类`，findAdvisorBeans 则可理解为`查找 Advisor 类型的 bean`。所以即使不看 findAdvisorBeans 方法的源码，我们也可从方法名上推断出它要做什么，即从 bean 容器中将 Advisor 类型的 bean 查找出来。下面我来分析一下这个方法的源码，如下：

```java
public List<Advisor> findAdvisorBeans() {
    String[] advisorNames = null;
    synchronized (this) {
        // cachedAdvisorBeanNames 是 advisor 名称的缓存
        advisorNames = this.cachedAdvisorBeanNames;
        /*
         * 如果 cachedAdvisorBeanNames 为空，这里到容器中查找，
         * 并设置缓存，后续直接使用缓存即可
         */ 
        if (advisorNames == null) {
            // 从容器中查找 Advisor 类型 bean 的名称
            advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                    this.beanFactory, Advisor.class, true, false);
            // 设置缓存
            this.cachedAdvisorBeanNames = advisorNames;
        }
    }
    if (advisorNames.length == 0) {
        return new LinkedList<Advisor>();
    }

    List<Advisor> advisors = new LinkedList<Advisor>();
    // 遍历 advisorNames
    for (String name : advisorNames) {
        if (isEligibleBean(name)) {
            // 忽略正在创建中的 advisor bean
            if (this.beanFactory.isCurrentlyInCreation(name)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping currently created advisor '" + name + "'");
                }
            }
            else {
                try {
                    /*
                     * 调用 getBean 方法从容器中获取名称为 name 的 bean，
                     * 并将 bean 添加到 advisors 中
                     */ 
                    advisors.add(this.beanFactory.getBean(name, Advisor.class));
                }
                catch (BeanCreationException ex) {
                    Throwable rootCause = ex.getMostSpecificCause();
                    if (rootCause instanceof BeanCurrentlyInCreationException) {
                        BeanCreationException bce = (BeanCreationException) rootCause;
                        if (this.beanFactory.isCurrentlyInCreation(bce.getBeanName())) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Skipping advisor '" + name +
                                        "' with dependency on currently created bean: " + ex.getMessage());
                            }
                            continue;
                        }
                    }
                    throw ex;
                }
            }
        }
    }

    return advisors;
}
```

以上就是从容器中查找 Advisor 类型的 bean 所有的逻辑，代码虽然有点长，但并不复杂。主要做了两件事情：

1. 从容器中查找所有类型为 Advisor 的 bean 对应的名称
2. 遍历 advisorNames，并从容器中获取对应的 bean

看完上面的分析，我们继续来分析一下 @Aspect 注解的解析过程。

#### buildAspectJAdvisors 方法分析

与上一节的内容相比，解析 @Aspect 注解的过程还是比较复杂的，需要一些耐心去看。下面我们开始分析 buildAspectJAdvisors 方法的源码，如下：

```java
public List<Advisor> buildAspectJAdvisors() {
    List<String> aspectNames = this.aspectBeanNames;

    if (aspectNames == null) {
        synchronized (this) {
            aspectNames = this.aspectBeanNames;
            if (aspectNames == null) {
                List<Advisor> advisors = new LinkedList<Advisor>();
                aspectNames = new LinkedList<String>();
                // 从容器中获取所有 bean 的名称
                String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                        this.beanFactory, Object.class, true, false);
                // 遍历 beanNames
                for (String beanName : beanNames) {
                    if (!isEligibleBean(beanName)) {
                        continue;
                    }
                    
                    // 根据 beanName 获取 bean 的类型
                    Class<?> beanType = this.beanFactory.getType(beanName);
                    if (beanType == null) {
                        continue;
                    }

                    // 检测 beanType 是否包含 Aspect 注解
                    if (this.advisorFactory.isAspect(beanType)) {
                        aspectNames.add(beanName);
                        AspectMetadata amd = new AspectMetadata(beanType, beanName);
                        if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                            MetadataAwareAspectInstanceFactory factory =
                                    new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);

                            // 获取通知器
                            List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
                            if (this.beanFactory.isSingleton(beanName)) {
                                this.advisorsCache.put(beanName, classAdvisors);
                            }
                            else {
                                this.aspectFactoryCache.put(beanName, factory);
                            }
                            advisors.addAll(classAdvisors);
                        }
                        else {
                            if (this.beanFactory.isSingleton(beanName)) {
                                throw new IllegalArgumentException("Bean with name '" + beanName +
                                        "' is a singleton, but aspect instantiation model is not singleton");
                            }
                            MetadataAwareAspectInstanceFactory factory =
                                    new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
                            this.aspectFactoryCache.put(beanName, factory);
                            advisors.addAll(this.advisorFactory.getAdvisors(factory));
                        }
                    }
                }
                this.aspectBeanNames = aspectNames;
                return advisors;
            }
        }
    }

    if (aspectNames.isEmpty()) {
        return Collections.emptyList();
    }
    List<Advisor> advisors = new LinkedList<Advisor>();
    for (String aspectName : aspectNames) {
        List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
        if (cachedAdvisors != null) {
            advisors.addAll(cachedAdvisors);
        }
        else {
            MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
            advisors.addAll(this.advisorFactory.getAdvisors(factory));
        }
    }
    return advisors;
}
```

上面就是 buildAspectJAdvisors 的代码，看起来比较长。代码比较多，我们关注重点的方法调用即可。在进行后续的分析前，这里先对 buildAspectJAdvisors 方法的执行流程做个总结。如下：

1. 获取容器中所有 bean 的名称（beanName）
2. 遍历上一步获取到的 bean 名称数组，并获取当前 beanName 对应的 bean 类型（beanType）
3. 根据 beanType 判断当前 bean 是否是一个的 Aspect 注解类，若不是则不做任何处理
4. 调用 advisorFactory.getAdvisors 获取通知器

下面我们来重点分析`advisorFactory.getAdvisors(factory)`这个调用，如下：

```java
public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
    // 获取 aspectClass 和 aspectName
    Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
    String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
    validate(aspectClass);

    MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
            new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);

    List<Advisor> advisors = new LinkedList<Advisor>();

    // getAdvisorMethods 用于返回不包含 @Pointcut 注解的方法
    for (Method method : getAdvisorMethods(aspectClass)) {
        // 为每个方法分别调用 getAdvisor 方法
        Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
        if (advisor != null) {
            advisors.add(advisor);
        }
    }

    // If it's a per target aspect, emit the dummy instantiating aspect.
    if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
        Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
        advisors.add(0, instantiationAdvisor);
    }

    // Find introduction fields.
    for (Field field : aspectClass.getDeclaredFields()) {
        Advisor advisor = getDeclareParentsAdvisor(field);
        if (advisor != null) {
            advisors.add(advisor);
        }
    }

    return advisors;
}

public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
        int declarationOrderInAspect, String aspectName) {

    validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

    // 获取切点实现类
    AspectJExpressionPointcut expressionPointcut = getPointcut(
            candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
    if (expressionPointcut == null) {
        return null;
    }

    // 创建 Advisor 实现类
    return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
            this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
}
```

如上，getAdvisor 方法包含两个主要步骤，一个是获取 AspectJ 表达式切点，另一个是创建 Advisor 实现类。在第二个步骤中，包含一个隐藏步骤 – 创建 Advice。下面我将按顺序依次分析这两个步骤，先看获取 AspectJ 表达式切点的过程，如下：

```java
@Aspect
public class AnnotationAopCode {

    @Pointcut("execution(* xyz.coolblog.aop.*.world*(..))")
    public void pointcut() {}

    @Before("pointcut()")
    public void before() {
        System.out.println("AnnotationAopCode`s before");
    }
}
```

@Before 注解中的表达式是`pointcut()`，也就是说 ajexp 设置的表达式只是一个中间值，不是最终值，即`execution(* xyz.coolblog.aop.*.world*(..))`。所以后续还需要将 ajexp 中的表达式进行转换，关于这个转换的过程，我就不说了。有点复杂，我暂时没怎么看懂。

说完切点的获取过程，下面再来看看 Advisor 实现类的创建过程。如下：

```java
public InstantiationModelAwarePointcutAdvisorImpl(AspectJExpressionPointcut declaredPointcut,
        Method aspectJAdviceMethod, AspectJAdvisorFactory aspectJAdvisorFactory,
        MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {

    this.declaredPointcut = declaredPointcut;
    this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
    this.methodName = aspectJAdviceMethod.getName();
    this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
    this.aspectJAdviceMethod = aspectJAdviceMethod;
    this.aspectJAdvisorFactory = aspectJAdvisorFactory;
    this.aspectInstanceFactory = aspectInstanceFactory;
    this.declarationOrder = declarationOrder;
    this.aspectName = aspectName;

    if (aspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
        Pointcut preInstantiationPointcut = Pointcuts.union(
                aspectInstanceFactory.getAspectMetadata().getPerClausePointcut(), this.declaredPointcut);

        this.pointcut = new PerTargetInstantiationModelPointcut(
                this.declaredPointcut, preInstantiationPointcut, aspectInstanceFactory);
        this.lazy = true;
    }
    else {
        this.pointcut = this.declaredPointcut;
        this.lazy = false;

        // 按照注解解析 Advice
        this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
    }
}

```

上面是 InstantiationModelAwarePointcutAdvisorImpl 的构造方法，不过我们无需太关心这个方法中的一些初始化逻辑。我们把目光移到构造方法的最后一行代码中，即 instantiateAdvice(this.declaredPointcut)，这个方法用于创建通知 Advice。在上一篇文章中我已经说过，通知器 Advisor 是通知 Advice 的持有者，所以在 Advisor 实现类的构造方法中创建通知也是合适的。那下面我们就来看看构建通知的过程是怎样的，如下：

```java
private Advice instantiateAdvice(AspectJExpressionPointcut pcut) {
    return this.aspectJAdvisorFactory.getAdvice(this.aspectJAdviceMethod, pcut,
            this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
}

public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
        MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {

    Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
    validate(candidateAspectClass);

    // 获取 Advice 注解
    AspectJAnnotation<?> aspectJAnnotation =
            AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
    if (aspectJAnnotation == null) {
        return null;
    }

    if (!isAspect(candidateAspectClass)) {
        throw new AopConfigException("Advice must be declared inside an aspect type: " +
                "Offending method '" + candidateAdviceMethod + "' in class [" +
                candidateAspectClass.getName() + "]");
    }

    if (logger.isDebugEnabled()) {
        logger.debug("Found AspectJ method: " + candidateAdviceMethod);
    }

    AbstractAspectJAdvice springAdvice;

    // 按照注解类型生成相应的 Advice 实现类
    switch (aspectJAnnotation.getAnnotationType()) {
        case AtBefore:    // @Before -> AspectJMethodBeforeAdvice
            springAdvice = new AspectJMethodBeforeAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            break;

        case AtAfter:    // @After -> AspectJAfterAdvice
            springAdvice = new AspectJAfterAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            break;

        case AtAfterReturning:    // @AfterReturning -> AspectJAfterAdvice
            springAdvice = new AspectJAfterReturningAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
            if (StringUtils.hasText(afterReturningAnnotation.returning())) {
                springAdvice.setReturningName(afterReturningAnnotation.returning());
            }
            break;

        case AtAfterThrowing:    // @AfterThrowing -> AspectJAfterThrowingAdvice
            springAdvice = new AspectJAfterThrowingAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
            if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
                springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
            }
            break;

        case AtAround:    // @Around -> AspectJAroundAdvice
            springAdvice = new AspectJAroundAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            break;

        /*
         * 什么都不做，直接返回 null。从整个方法的调用栈来看，
         * 并不会出现注解类型为 AtPointcut 的情况
         */ 
        case AtPointcut:    
            if (logger.isDebugEnabled()) {
                logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
            }
            return null;
            
        default:
            throw new UnsupportedOperationException(
                    "Unsupported advice type on method: " + candidateAdviceMethod);
    }

    springAdvice.setAspectName(aspectName);
    springAdvice.setDeclarationOrder(declarationOrder);
    /*
     * 获取方法的参数列表名称，比如方法 int sum(int numX, int numY), 
     * getParameterNames(sum) 得到 argNames = [numX, numY]
     */
    String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
    if (argNames != null) {
        // 设置参数名
        springAdvice.setArgumentNamesFromStringArray(argNames);
    }
    springAdvice.calculateArgumentBindings();
    return springAdvice;
}
```

上面的代码逻辑不是很复杂，主要的逻辑就是根据注解类型生成与之对应的通知对象。下面来总结一下获取通知器（getAdvisors）整个过程的逻辑，如下：

1. 从目标 bean 中获取不包含 Pointcut 注解的方法列表
2. 遍历上一步获取的方法列表，并调用 getAdvisor 获取当前方法对应的 Advisor
3. 创建 AspectJExpressionPointcut 对象，并从方法中的注解中获取表达式，最后设置到切点对象中
4. 创建 Advisor 实现类对象 InstantiationModelAwarePointcutAdvisorImpl
5. 调用 instantiateAdvice 方法构建通知
6. 调用 getAdvice 方法，并根据注解类型创建相应的通知

如上所示，上面的步骤做了一定的简化。总的来说，获取通知器的过程还是比较复杂的，并不是很容易看懂。大家在阅读的过程中，还要写一些测试代码进行调试才行。调试的过程中，一些不关心的调用就别跟进去了，不然会陷入很深的调用栈中，影响对源码主流程的理解。

现在，大家知道了通知是怎么创建的。那我们难道不要去看看这些通知的实现源码吗？显然，我们应该看一下。那接下里，我们一起来分析一下 AspectJMethodBeforeAdvice，也就是 @Before 注解对应的通知实现类。看看它的逻辑是什么样的。

#### AspectJMethodBeforeAdvice 分析

```java
public class AspectJMethodBeforeAdvice extends AbstractAspectJAdvice implements MethodBeforeAdvice {

    public AspectJMethodBeforeAdvice(
            Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

        super(aspectJBeforeAdviceMethod, pointcut, aif);
    }


    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        // 调用通知方法
        invokeAdviceMethod(getJoinPointMatch(), null, null);
    }

    @Override
    public boolean isBeforeAdvice() {
        return true;
    }

    @Override
    public boolean isAfterAdvice() {
        return false;
    }

}

protected Object invokeAdviceMethod(JoinPointMatch jpMatch, Object returnValue, Throwable ex) throws Throwable {
    // 调用通知方法，并向其传递参数
    return invokeAdviceMethodWithGivenArgs(argBinding(getJoinPoint(), jpMatch, returnValue, ex));
}

protected Object invokeAdviceMethodWithGivenArgs(Object[] args) throws Throwable {
    Object[] actualArgs = args;
    if (this.aspectJAdviceMethod.getParameterTypes().length == 0) {
        actualArgs = null;
    }
    try {
        ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
        // 通过反射调用通知方法
        return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance(), actualArgs);
    }
    catch (IllegalArgumentException ex) {
        throw new AopInvocationException("Mismatch on arguments to advice method [" +
                this.aspectJAdviceMethod + "]; pointcut expression [" +
                this.pointcut.getPointcutExpression() + "]", ex);
    }
    catch (InvocationTargetException ex) {
        throw ex.getTargetException();
    }
}
```

如上，AspectJMethodBeforeAdvice 的源码比较简单，这里我们仅关注 before 方法。这个方法调用了父类中的 invokeAdviceMethod，然后 invokeAdviceMethod 在调用 invokeAdviceMethodWithGivenArgs，最后在 invokeAdviceMethodWithGivenArgs 通过反射执行通知方法。是不是很简单？

关于 AspectJMethodBeforeAdvice 就简单介绍到这里吧，至于剩下的几种实现，大家可以自己去看看。好了，关于 AspectJMethodBeforeAdvice 的源码分析，就分析到这里了。我们继续往下看吧。

----

#### 筛选合适的通知器

查找出所有的通知器，整个流程还没算完，接下来我们还要对这些通知器进行筛选。适合应用在当前 bean 上的通知器留下，不适合的就让它自生自灭吧。那下面我们来分析一下通知器筛选的过程，如下：

```java
protected List<Advisor> findAdvisorsThatCanApply(
        List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

    ProxyCreationContext.setCurrentProxiedBeanName(beanName);
    try {
        // 调用重载方法
        return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
    }
    finally {
        ProxyCreationContext.setCurrentProxiedBeanName(null);
    }
}

public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
    if (candidateAdvisors.isEmpty()) {
        return candidateAdvisors;
    }
    List<Advisor> eligibleAdvisors = new LinkedList<Advisor>();
    for (Advisor candidate : candidateAdvisors) {
        // 筛选 IntroductionAdvisor 类型的通知器
        if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
            eligibleAdvisors.add(candidate);
        }
    }
    boolean hasIntroductions = !eligibleAdvisors.isEmpty();
    for (Advisor candidate : candidateAdvisors) {
        if (candidate instanceof IntroductionAdvisor) {
            continue;
        }

        // 筛选普通类型的通知器
        if (canApply(candidate, clazz, hasIntroductions)) {
            eligibleAdvisors.add(candidate);
        }
    }
    return eligibleAdvisors;
}

public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
    if (advisor instanceof IntroductionAdvisor) {
        /*
         * 从通知器中获取类型过滤器 ClassFilter，并调用 matchers 方法进行匹配。
         * ClassFilter 接口的实现类 AspectJExpressionPointcut 为例，该类的
         * 匹配工作由 AspectJ 表达式解析器负责，具体匹配细节这个就没法分析了，我
         * AspectJ 表达式的工作流程不是很熟
         */
        return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
    }
    else if (advisor instanceof PointcutAdvisor) {
        PointcutAdvisor pca = (PointcutAdvisor) advisor;
        // 对于普通类型的通知器，这里继续调用重载方法进行筛选
        return canApply(pca.getPointcut(), targetClass, hasIntroductions);
    }
    else {
        return true;
    }
}

public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
    Assert.notNull(pc, "Pointcut must not be null");
    // 使用 ClassFilter 匹配 class
    if (!pc.getClassFilter().matches(targetClass)) {
        return false;
    }

    MethodMatcher methodMatcher = pc.getMethodMatcher();
    if (methodMatcher == MethodMatcher.TRUE) {
        return true;
    }

    IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
    if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
        introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
    }

    /*
     * 查找当前类及其父类（以及父类的父类等等）所实现的接口，由于接口中的方法是 public，
     * 所以当前类可以继承其父类，和父类的父类中所有的接口方法
     */ 
    Set<Class<?>> classes = new LinkedHashSet<Class<?>>(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
    classes.add(targetClass);
    for (Class<?> clazz : classes) {
        // 获取当前类的方法列表，包括从父类中继承的方法
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
        for (Method method : methods) {
            // 使用 methodMatcher 匹配方法，匹配成功即可立即返回
            if ((introductionAwareMethodMatcher != null &&
                    introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions)) ||
                    methodMatcher.matches(method, targetClass)) {
                return true;
            }
        }
    }

    return false;
}
```

以上是通知器筛选的过程，筛选的工作主要由 ClassFilter 和 MethodMatcher 完成。在 AOP 中，切点 Pointcut 是用来匹配连接点的，以 AspectJExpressionPointcut 类型的切点为例。该类型切点实现了ClassFilter 和 MethodMatcher 接口，匹配的工作则是由 AspectJ 表达式解析器复杂。除了使用 AspectJ 表达式进行匹配，Spring 还提供了基于正则表达式的切点类，以及更简单的根据方法名进行匹配的切点类。大家有兴趣的话，可以自己去了解一下，这里就不多说了。

在完成通知器的查找和筛选过程后，还需要进行最后一步处理 – 对通知器列表进行拓展。怎么拓展呢？我们一起到下一节中一探究竟吧。

#### 拓展筛选出通知器列表

拓展方法 extendAdvisors 做的事情并不多，逻辑也比较简单。我们一起来看一下，如下：

```java
protected void extendAdvisors(List<Advisor> candidateAdvisors) {
    AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
}

public static boolean makeAdvisorChainAspectJCapableIfNecessary(List<Advisor> advisors) {
    // 如果通知器列表是一个空列表，则啥都不做
    if (!advisors.isEmpty()) {
        boolean foundAspectJAdvice = false;
        /*
         * 下面的 for 循环用于检测 advisors 列表中是否存在 
         * AspectJ 类型的 Advisor 或 Advice
         */
        for (Advisor advisor : advisors) {
            if (isAspectJAdvice(advisor)) {
                foundAspectJAdvice = true;
            }
        }

        /*
         * 向 advisors 列表的首部添加 DefaultPointcutAdvisor，
         * 至于为什么这样做，我会在后续的文章中进行说明
         */
        if (foundAspectJAdvice && !advisors.contains(ExposeInvocationInterceptor.ADVISOR)) {
            advisors.add(0, ExposeInvocationInterceptor.ADVISOR);
            return true;
        }
    }
    return false;
}

private static boolean isAspectJAdvice(Advisor advisor) {
    return (advisor instanceof InstantiationModelAwarePointcutAdvisor ||
            advisor.getAdvice() instanceof AbstractAspectJAdvice ||
            (advisor instanceof PointcutAdvisor &&
                     ((PointcutAdvisor) advisor).getPointcut() instanceof AspectJExpressionPointcut));
}

```

如上，上面的代码比较少，也不复杂。由源码可以看出 extendAdvisors 是一个空壳方法，除了调用makeAdvisorChainAspectJCapableIfNecessary，该方法没有其他更多的逻辑了。至于 makeAdvisorChainAspectJCapableIfNecessary 这个方法，该方法主要的目的是向通知器列表首部添加 DefaultPointcutAdvisor 类型的通知器，也就是 ExposeInvocationInterceptor.ADVISOR。

----

—未完 待续