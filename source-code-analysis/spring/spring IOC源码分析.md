# Spring IOC 源码分析

## 一：IOC/DI





### 1:IOC、DI的概念

#### 1.1 IOC是什么

**Ioc—Inversion of Control，即“控制反转”，不是什么技术，而是一种设计思想。**在Java开发中，**Ioc意味着将你设计好的对象交给容器控制，而不是传统的在你的对象内部直接控制。**如何理解好Ioc呢？理解好Ioc的关键是要明确“谁控制谁，控制什么，为何是反转（有反转就应该有正转了），哪些方面反转了”，那我们来深入分析一下：

　　●**谁控制谁，控制什么：**传统Java SE程序设计，我们直接在对象内部通过new进行创建对象，是程序主动去创建依赖对象；而IoC是有专门一个容器来创建这些对象，即由Ioc容器来控制对 象的创建；**谁控制谁？当然是IoC 容器控制了对象；控制什么？那就是主要控制了外部资源获取（不只是对象包括比如文件等）。**

　　●**为何是反转，哪些方面反转了：**有反转就有正转，传统应用程序是由我们自己在对象中主动控制去直接获取依赖对象，也就是正转；而反转则是由容器来帮忙创建及注入依赖对象；为何是反转？**因为由容器帮我们查找及注入依赖对象，对象只是被动的接受依赖对象，所以是反转；哪些方面反转了？依赖对象的获取被反转了。**

#### 1.2 IOC能做什么

　IoC 不是一种技术，只是一种思想，一个重要的面向对象编程的法则，它能指导我们如何设计出松耦合、更优良的程序。传统应用程序都是由我们在类内部主动创建依赖对象，从而导致类与类之间高耦合，难于测试；有了IoC容器后，把创建和查找依赖对象的控制权交给了容器，由容器进行注入组合对象，所以对象与对象之间是 松散耦合，这样也方便测试，利于功能复用，更重要的是使得程序的整个体系结构变得非常灵活。

　　其实**IoC对编程带来的最大改变不是从代码上，而是从思想上，发生了“主从换位”的变化。应用程序原本是老大，要获取什么资源都是主动出击，但是在IoC/DI思想中，应用程序就变成被动的了，被动的等待IoC容器来创建并注入它所需要的资源了。**

　　**IoC很好的体现了面向对象设计法则之一—— 好莱坞法则：“别找我们，我们找你”；即由IoC容器帮对象找相应的依赖对象并注入，而不是由对象主动去找。**

#### 1.3IOC和DI

**DI—Dependency Injection，即“依赖注入”**：**组件之间依赖关系**由容器在运行期决定，形象的说，即**由容器动态的将某个依赖关系注入到组件之中**。**依赖注入的目的并非为软件系统带来更多功能，而是为了提升组件重用的频率，并为系统搭建一个灵活、可扩展的平台。**通过依赖注入机制，我们只需要通过简单的配置，而无需任何代码就可指定目标需要的资源，完成自身的业务逻辑，而不需要关心具体的资源来自何处，由谁实现。

　　理解DI的关键是：“谁依赖谁，为什么需要依赖，谁注入谁，注入了什么”，那我们来深入分析一下：

　　●**谁依赖于谁：**当然是**应用程序依赖于IoC容器**；

　　●**为什么需要依赖：****应用程序需要IoC容器来提供对象需要的外部资源**；

　　●**谁注入谁：**很明显是**IoC容器注入应用程序某个对象，应用程序依赖的对象**；

　　**●注入了什么：**就是**注入某个对象所需要的外部资源（包括对象、资源、常量数据）**。

　　**IoC和DI**由什么**关系**呢？其实它们**是同一个概念的不同角度描述**，由于控制反转概念比较含糊（可能只是理解为容器控制对象这一个层面，很难让人想到谁来维护对象关系），所以2004年大师级人物Martin Fowler又给出了一个新的名字：“依赖注入”，相对IoC 而言，**“****依赖注入”****明确描述了“被注入对象依赖IoC****容器配置依赖对象”。**

## 二：Spring IOC的体系结构

![image-20190326084043350](https://ws4.sinaimg.cn/large/006tKfTcgy1g1fwi03tabj31sc0pwq6z.jpg)

### 1：BeanFactory

BeanFactory，以Factory结尾，表示它是一个工厂类(接口)， **它负责生产和管理bean的一个工厂**。在Spring中，**BeanFactory是IOC容器的核心接口，它的职责包括：实例化、定位、配置应用程序中的对象及建立这些对象间的依赖。BeanFactory只是个接口，并不是IOC容器的具体实现，但是Spring容器给出了很多种实现，如 DefaultListableBeanFactory、XmlBeanFactory、ApplicationContext等，其中****XmlBeanFactory就是常用的一个，该实现将以XML方式描述组成应用的对象及对象间的依赖关系**。XmlBeanFactory类将持有此XML配置元数据，并用它来构建一个完全可配置的系统或应用。   

  都是附加了某种功能的实现。 它为其他具体的IOC容器提供了最基本的规范，例如DefaultListableBeanFactory,XmlBeanFactory,ApplicationContext 等具体的容器都是实现了BeanFactory，再在其基础之上附加了其他的功能。  

BeanFactory和ApplicationContext就是spring框架的两个IOC容器，现在一般使用ApplicationnContext，其不但包含了BeanFactory的作用，同时还进行更多的扩展。  

**BeanFacotry是spring中比较原始的Factory。如XMLBeanFactory就是一种典型的BeanFactory。**
原始的BeanFactory无法支持spring的许多插件，**如AOP功能、Web应用等**。ApplicationContext接口,它由BeanFactory接口派生而来.

### 2：FactoryBean

**一般情况下，Spring通过反射机制利用<bean>的class属性指定实现类实例化Bean，在某些情况下，实例化Bean过程比较复杂，如果按照传统的方式，则需要在<bean>中提供大量的配置信息。配置方式的灵活性是受限的，这时采用编码的方式可能会得到一个简单的方案。Spring为此提供了一个org.springframework.bean.factory.FactoryBean的工厂类接口，用户可以通过实现该接口定制实例化Bean的逻辑。FactoryBean接口对于Spring框架来说占用重要的地位，Spring自身就提供了70多个FactoryBean的实现**。它们隐藏了实例化一些复杂Bean的细节，给上层应用带来了便利。从Spring3.0开始，FactoryBean开始支持泛型，即接口声明改为FactoryBean<T>的形式

以Bean结尾，表示它是一个Bean，不同于普通Bean的是：它是实现了FactoryBean<T>接口的Bean，根据该Bean的ID从BeanFactory中获取的实际上是FactoryBean的getObject()返回的对象，而不是FactoryBean本身，如果要获取FactoryBean对象，请在id前面加一个&符号来获取。

## 三：IOC容器的初始化

###1.IOC容器初始化过程

IOC容器的初始化主要包含三个过程：Beandefinition的Resource定位、载入和注册。

#### beandefinition的载入

我们先来看一段最简化的启动IOC容器的代码：

```java
public class App {
  public static void main(String[] args) {
    // 用我们的配置文件来启动一个 ApplicationContext
    ApplicationContext context = new ClassPathXmlApplicationContext("classpath:application.xml");
    .......
      .......
  }
```

首先从构造函数入手：跟进去查看

```java
//从给定的xml文件中家在定义用来创建ClassPathXmlApplicationContext , 并且自动刷新上下文
public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
  this(new String[] {configLocation}, true, null);
}

/* 
请注意这个构造函数，它的意思是可以传多个配置文件地址
*/
public ClassPathXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

```

我们继续跟进到`this`;

```java
public ClassPathXmlApplicationContext(
  String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
  throws BeansException {

  super(parent);
  setConfigLocations(configLocations);
  if (refresh) {
    refresh();
  }
}
```

可以看到这个比较核心的方法就是`refresh`,我们跟进去看看：

```java
public void refresh() throws BeansException, IllegalStateException {
  // 注意这边的锁，防止其他销毁或者启动的动作
  synchronized (this.startupShutdownMonitor) {
    // 准备上下文
    prepareRefresh();
    // 这是在子类中去启动refreshBeanFactory()的地方
    ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
    // 准备需要在此上下文中使用的bean工厂
    prepareBeanFactory(beanFactory);
    try {
      // 设置beanFactory的后置处理
      postProcessBeanFactory(beanFactory);
      // 调用BeanFactory的后置处理器，这些后处理器是在bean定义中向容器注册的
      invokeBeanFactoryPostProcessors(beanFactory);
      // 注册bean的后处理器，在bean的创建过程中调用
      registerBeanPostProcessors(beanFactory);
      // 初始化次上下文的消息源.
      initMessageSource();
      // 初始化上下文中的事件机制
      initApplicationEventMulticaster();
      // 在特定的上下文子类中初始化其他特殊bean
      onRefresh();
      // 检查监听bean并将这些bean向容器注册
      registerListeners();
      // 实例化所有剩余（非延迟初始化）单例
      finishBeanFactoryInitialization(beanFactory);
      // 最后一步：发布相应的事件。结束Refresh过程
      finishRefresh();
    }
    catch (BeansException ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Exception encountered during context initialization - " +
                    "cancelling refresh attempt: " + ex);
      }
      // 为防止bean资源占用，在异常处理中，销毁已经在前面生成的单例bean
      destroyBeans();
      // 取消刷新上下文，重置active标志
      cancelRefresh(ex);
      throw ex;
    }
    finally {
      resetCommonCaches();
    }
  }
}
```

从上面的方法中大概了解了流程，那么接下来就要详细去介绍了：

**prepareRefresh(准备上下文)：** 

为刷新上下文做了准备工作，这边不是重点：

```java
protected void prepareRefresh() {
		// 记录启动时间、设置活动标志
		this.startupDate = System.currentTimeMillis();
		this.closed.set(false);
		this.active.set(true);
		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			}
			else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		// 在上下文环境中初始化任何占位符属性源
		initPropertySources();
		// 验证标记为必需的所有属性是否可解析
		getEnvironment().validateRequiredProperties();
		// 存储预刷新ApplicationListeners
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		}
		else {
			// 将本地应用程序侦听器重置为预刷新状态
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// 允许收集早期的ApplicationEvents，
		// 一旦多播器可用就会发布
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}
```

**obtainFreshBeanFactory（）重要**

我们先跟着调用进入到核心方法：

```java
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		refreshBeanFactory();
		return getBeanFactory();
	}

// 继续进入到refreshBeanFactory();
protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;


//进入到AbstractRefreshableApplicationContext的refreshBeanFactory
/**
	 * 实际刷新BeanFactory的操作
	 * @throws BeansException
	 */
	@Override
	protected final void refreshBeanFactory() throws BeansException {
		// 如果以前有存在的BeanFactory的话，直接销毁bean，并关闭beanfactory
		if (hasBeanFactory()) {
			destroyBeans();
			closeBeanFactory();
		}
		try {
			// 创建IOC容器，这里使用的是DefaultListableBeanFactory
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			// 主要用于beanfactory的序列化
			beanFactory.setSerializationId(getId());
			// 设置beanfactory的属性：是否允许bean覆盖以及是否允许循环引用
			customizeBeanFactory(beanFactory);
			// 启动对beandefinition的载入（很核心的方法，下面会讲）
			loadBeanDefinitions(beanFactory);
			synchronized (this.beanFactoryMonitor) {
				this.beanFactory = beanFactory;
			}
		}
		catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}
```

> 我们看到上面创建beanFactory的接收类是DefaultListableBeanFactory，为什么呢，大家可以翻到最上面的IOC的体系结构图去看，可以发现最底层的实现类就是这个类，它可以说集众家之所长，可以说是功能最完善的beanfactory了。

接下来分析的是`customizeBeanFactory`,主要就做了两件事：是否允许bean definition覆盖，以及是否允许bean间的循环依赖，下面会有例子解释

```java
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		if (this.allowBeanDefinitionOverriding != null) {
			// 是否允许beandefinition覆盖
			beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
		if (this.allowCircularReferences != null) {
			// 是否允许bean间的循环依赖
			beanFactory.setAllowCircularReferences(this.allowCircularReferences);
		}
	}
```

- beandefinition的覆盖

  简单的说就是如果allowBeanDefinitionOverriding为null（默认就是为null），你的bean定义的时候使用了相同的id或者name,在同一个配置文件中，就会直接抛出异常，如果在不同的配置文件中，那么就会发生覆盖现象。

- 循环引用

  简单说就是A依赖B，B依赖A，在默认情况下，spring是允许循环依赖的。

接下来就是重中之重：**加载bean定义，loadBeanDefinitions**,跟踪源码不难看出，loadBeanDefinitions是一个抽象方法，那么我们就来看看`AbstractXmlApplicationContext`类中的实现：

```java
	/**
	 * 通过XmlBeanDefinitionReader加载bean定义。
	 * @param beanFactory the bean factory to load bean definitions into
	 * @throws BeansException
	 * @throws IOException
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// 为给定的BeanFactory创建一个新的XmlBeanDefinitionReader
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// 使用此上下文配置bean定义读取器
		// 设置资源加载环境。
		beanDefinitionReader.setEnvironment(this.getEnvironment());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		//允许子类提供读者的自定义初始化，然后继续实际加载bean定义
		initBeanDefinitionReader(beanDefinitionReader);
    // 这部分就是核心重点
		loadBeanDefinitions(beanDefinitionReader);
	}
```

那么先大致的介绍下`loadbeandefinition`的过程：先初始化了读取器XmlBeanDefinitionReader，然后把读取器在IOC容器中设置好，最后启动读取器来完成BeanDefinition在IOC容器中的载入。那么重点就是`loadBeanDefinitions(beanDefinitionReader);`,我们来看下实现：

```java
/**
	 *使用给定的XmlBeanDefinitionReader加载bean定义
	 * @param reader
	 * @throws BeansException
	 * @throws IOException
	 */
protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
  // 以Resoure的方式获得配置文件的资源位置 1
  Resource[] configResources = getConfigResources();
  if (configResources != null) {
    reader.loadBeanDefinitions(configResources);
  }
  // 以String的形式获得配置文件的位置 2
  String[] configLocations = getConfigLocations();
  if (configLocations != null) {
    reader.loadBeanDefinitions(configLocations);
  }
}
```

不管是string形式获得配置文件还是通过Resource形式获得配置文件，最终都是由`doLoadBeanDefinitions(inputSource, encodedResource.getResource());来完成对BeanDefinition的加载，String形式的最终还是会根据它传过去的configLocations去获取Resource，代码如下：

```java
@Override
public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
  Assert.notNull(locations, "Location array must not be null");
  int count = 0;
  for (String location : locations) {
    count += loadBeanDefinitions(location);
  }
  return count;
}

// AbstractBeanDefinitionReader 下面的类
public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
		// 获取资源加载器
		ResourceLoader resourceLoader = getResourceLoader();
		if (resourceLoader == null) {
			throw new BeanDefinitionStoreException(
					"Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
		}

		if (resourceLoader instanceof ResourcePatternResolver) {
			//资源模式匹配可用
			try {
				// 从传过来的string形式的地址中获取资源
				Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
				// 最终还是调的跟Resource方式一样的方法
				int count = loadBeanDefinitions(resources);
				if (actualResources != null) {
					Collections.addAll(actualResources, resources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
				}
				return count;
			}
			catch (IOException ex) {
				throw n
          ........
```

那么既然都是走的`loadBeanDefinitions(resources)`,就看这个方法做的是什么，根据调用，最终会跟到XmlbeanDefinitionReader里面的方法：

```java
/**
	 * 从指定的资源加载bean定义
	 * @param encodedResource 指定了编码的xml资源描述符
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		Assert.notNull(encodedResource, "EncodedResource must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("Loading XML bean definitions from " + encodedResource);
		}

		Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
		if (currentResources == null) {
			currentResources = new HashSet<>(4);
			this.resourcesCurrentlyBeingLoaded.set(currentResources);
		}
		if (!currentResources.add(encodedResource)) {
			throw new BeanDefinitionStoreException(
					"Detected cyclic loading of " + encodedResource + " - check your import definitions!");
		}
		try {
			InputStream inputStream = encodedResource.getResource().getInputStream();
			// 这里得到了经过编码的xml资源描述符文件，根据IO的inputstream进行读取
			try {
				InputSource inputSource = new InputSource(inputStream);
				if (encodedResource.getEncoding() != null) {
					inputSource.setEncoding(encodedResource.getEncoding());
				}
				// 这里就是具体的读取过程
				return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"IOException parsing XML document from " + encodedResource.getResource(), ex);
		}
		finally {
			currentResources.remove(encodedResource);
			if (currentResources.isEmpty()) {
				this.resourcesCurrentlyBeingLoaded.remove();
			}
		}
	}

```

上面那么多一段，最核心的执行方法就是：`doLoadBeanDefinitions`,跟进去看下：

```java
/**
	 * 实际从xml资源描述符加载beandefinition的方法
	 */
protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
  throws BeanDefinitionStoreException {

  try {
    // 这里是取得XML文件的Document对象，这个解析过程是由documentloader完成的
    Document doc = doLoadDocument(inputSource, resource);

    // 重点
    // 这里讲述的是对BeanDefinition解析的详细过程
    // 会使用到Spring的bean配置规则
    int count = registerBeanDefinitions(doc, resource);
    if (logger.isDebugEnabled()) {
      logger.debug("Loaded " + count + " bean definitions from " + resource);
    }
    return count;
  }
  catch (BeanDefinitionStoreException ex) {
    throw ex;
  }
  catch (SAXParseException ex) {
    throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                                              "Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
  }
  catch (SAXException ex) {
    throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                                              "XML document from " + resource + " is invalid", ex);
  }
  catch (ParserConfigurationException ex) {
    throw new BeanDefinitionStoreException(resource.getDescription(),
                                           "Parser configuration exception parsing XML from " + resource, ex);
  }
  catch (IOException ex) {
    throw new BeanDefinitionStoreException(resource.getDescription(),
                                           "IOException parsing XML document from " + resource, ex);
  }
  catch (Throwable ex) {
    throw new BeanDefinitionStoreException(resource.getDescription(),
                                           "Unexpected exception parsing XML document from " + resource, ex);
  }
}
```

到此为止一个配置文件被成功转成了DOM树。继续看上面提到的重点方法`registerBeanDefinitions`：

```java
public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
		// 创建beandefinitionDocumentReader来对doc文档中的bean定义进行解析
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		int countBefore = getRegistry().getBeanDefinitionCount();
		// 具体的解析过程就在registerBeanDefinitions中完成
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
		return getRegistry().getBeanDefinitionCount() - countBefore;
	}
```

可以看到这里又创建了beandefinitionDocumentReader来对doc文档中的bean定义进行解析，具体过程就在documentReader.registerBeanDefinitions，继续跟入：

```java
/**
	 * 在给定的根元素（beans）中注册每个bean定义
	 */
	@SuppressWarnings("deprecation")  // for Environment.acceptsProfiles(String...)
	protected void doRegisterBeanDefinitions(Element root) {
		// 委托模式  由BeanDefinitionParserDelegate负责解析bean定义
		BeanDefinitionParserDelegate parent = this.delegate;
		this.delegate = createDelegate(getReaderContext(), root, parent);

		if (this.delegate.isDefaultNamespace(root)) {
			// 判断当前环境配置profile是否是需要的
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}

		// 首先处理任何自定义元素类型，允许XML可扩展 钩子方法
		preProcessXml(root);
		//解析文档中根级别的元素：* “import”，“别名”，“bean”。
		parseBeanDefinitions(root, this.delegate);

		// 通过最后处理任何自定义元素类型来允许XML可扩展，在我们完成处理bean定义之后 同样也是钩子方法
		postProcessXml(root);

		this.delegate = parent;
	}
```

如果有对profile不理解的可以看这篇文章<https://www.jianshu.com/p/948c303b2253>，这个方法通过委托模式将解析bean定义的工作交给了BeanDefinitionParserDelegate来完成，整个核心方法就是parseBeanDefinitions，继续跟入，接下来就是beandefinition的解析过程：

#### beandefinition的解析

```java
/**
	 * 解析文档中根级别的元素：
	 * “import”，“alias”，“bean”,"beans"。
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		if (delegate.isDefaultNamespace(root)) {
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;
					if (delegate.isDefaultNamespace(ele)) {
						// 解析“import”，“alias”，“bean”,"beans"
						parseDefaultElement(ele, delegate);
					}
					else {
						// 解析自定义元素
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		else {
			delegate.parseCustomElement(root);
		}
	}
```

可以看出，主要就进行了两个分支的操作，parseDefaultElement(ele, delegate) 和delegate.parseCustomElement(ele)。那么就挑我们常见的元素讲解：

```java
private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			// 处理import标签
			importBeanDefinitionResource(ele);
		}
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			// 处理 <alias /> 标签定义
			// <alias name="fromName" alias="toName"/>
			processAliasRegistration(ele);
		}
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			// 处理 <bean /> 标签定义
			processBeanDefinition(ele, delegate);
		}
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// 递归调用此方法，解析beans下的bean定义
			doRegisterBeanDefinitions(ele);
		}
	}
```

这部分大概的讲述了解析4种标签的方式，那么重点必然是我们用的最多的<bean>,读者有兴趣可自行研究，进入到processBeanDefinition方法中：

```java
/**
	 * 处理给定的bean元素，解析bean定义
	 * 并在注册表中注册
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		/*
		* BeanDefinitionHolder是BeanDefinition的封装类，封装了BeanDefinition，bean的名字以及别名。
		* 通过它来完成向IOC容器的注册。
		* */
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			// 这里涉及到了装饰者模式
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// 注册最终修饰的实例
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// 发送注册事件
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}
```

上面大概讲了一下处理给定bean定义的步骤，在详细讲解之前，我们必须了解下<bean>标签中主要可以定义哪些属性：

| Property                 | 解释                                                         |
| ------------------------ | ------------------------------------------------------------ |
| class                    | 类的全限定名                                                 |
| name                     | 可指定 id、name(用逗号、分号、空格分隔)                      |
| scope                    | 作用域                                                       |
| constructor arguments    | 指定构造参数                                                 |
| properties               | 设置属性的值                                                 |
| autowiring mode          | no(默认值)、byName、byType、 constructor                     |
| lazy-initialization mode | 是否懒加载(如果被非懒加载的bean依赖了那么其实也就不能懒加载了) |
| initialization method    | bean 属性设置完成后，会调用这个方法                          |
| destruction method       | bean 销毁后的回调方法                                        |

那么了解了大概的bean属性，再次回到`BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);`,跟入：

```java
public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
		// 这里取得在<bean>元素中定义的ID，name和aliase属性的值

		String id = ele.getAttribute(ID_ATTRIBUTE);
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		List<String> aliases = new ArrayList<>();

		// 建议大家了解一下id和name的配置
		if (StringUtils.hasLength(nameAttr)) {
			String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
			aliases.addAll(Arrays.asList(nameArr));
		}

		String beanName = id;
		// 如果没有指定id，那么就用别名列表中的 第一个 名字作为beanname
		if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
			beanName = aliases.remove(0);
			if (logger.isTraceEnabled()) {
				logger.trace("No XML 'id' specified - using '" + beanName +
						"' as bean name and " + aliases + " as aliases");
			}
		}

		if (containingBean == null) {
			checkNameUniqueness(beanName, aliases, ele);
		}

		// 此方法引发对bean元素的详细解析，并最终返回一个 beandefinition的实例
		AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);

		// 上面已经形成了一个beandefinition实例，bean标签解析完成了。
		if (beanDefinition != null) {
			if (!StringUtils.hasText(beanName)) {
				try {
					if (containingBean != null) {
						beanName = BeanDefinitionReaderUtils.generateBeanName(
								beanDefinition, this.readerContext.getRegistry(), true);
					}
					else {
						beanName = this.readerContext.generateBeanName(beanDefinition);
						String beanClassName = beanDefinition.getBeanClassName();
						if (beanClassName != null &&
								beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
								!this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
							aliases.add(beanClassName);
						}
					}
					if (logger.isTraceEnabled()) {
						logger.trace("Neither XML 'id' nor 'name' specified - " +
								"using generated bean name [" + beanName + "]");
					}
				}
				catch (Exception ex) {
					error(ex.getMessage(), ele);
					return null;
				}
			}
			// 别名数组
			String[] aliasesArray = StringUtils.toStringArray(aliases);
			// 返回beandefinitionholder
			return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
		}

		return null;
	}
```

上面方法的核心就是：AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);那么我跟入去看：

```java
/**
	 * 解析bean定义本省，不考虑名称或者别名。
	 */
	@Nullable
	public AbstractBeanDefinition parseBeanDefinitionElement(
			Element ele, String beanName, @Nullable BeanDefinition containingBean) {

		this.parseState.push(new BeanEntry(beanName));

		// 这里只是读取定义的<bean>中设置的class名字，然后将其载入到BeanDefinition中去。
		// 只是做个记录，并不涉及到对象的实例化过程，实例化是在依赖注入完成的
		String className = null;
		if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
			className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
		}
		String parent = null;
		if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
			parent = ele.getAttribute(PARENT_ATTRIBUTE);
		}

		try {
			// 生成需要的beandefinition对象，为bean定义信息的载入做准备
			AbstractBeanDefinition bd = createBeanDefinition(className, parent);

			// 对当前bean元素的属性进行解析，并设置description信息
			parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
			bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));

			// 接下来是对<bean>元素内部的各种子元素进行解析，然后再放入到bd中

			// 解析<meta>
			parseMetaElements(ele, bd);

			// 解析 <lookup-method />
			parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
			// 解析 <replaced-method />
			parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
			// 解析 <constructor-arg />
			parseConstructorArgElements(ele, bd);
			// 解析 <property />
			parsePropertyElements(ele, bd);
			// 解析 <qualifier />
			parseQualifierElements(ele, bd);

			bd.setResource(this.readerContext.getResource());
			bd.setSource(extractSource(ele));

			return bd;
		}

		// 这里就是我们常见的一些bean配置的异常，以后可以找到这个类做断点
		// BeanpDefinitionParserDelegate.parseBeanDefinitionElement
		catch (ClassNotFoundException ex) {
			error("Bean class [" + className + "] not found", ele, ex);
		}
		catch (NoClassDefFoundError err) {
			error("Class that bean class [" + className + "] depends on not found", ele, err);
		}
		catch (Throwable ex) {
			error("Unexpected failure during bean definition parsing", ele, ex);
		}
		finally {
			this.parseState.pop();
		}

		return null;
	}
```

上面对bean元素的各个子元素进行了解析，选一个我们经常用到的，也就是对property元素的解析：

```java 
/**
	 * 解析给定bean元素中的property元素
	 */
	public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
		NodeList nl = beanEle.getChildNodes();
		// 遍历所有bean元素下定义的property元素
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, PROPERTY_ELEMENT)) {
				// 判断是否是property元素，并对其解析
				parsePropertyElement((Element) node, bd);
			}
		}
	}
```

```java
/**
	 * 解析property元素
	 */
	public void parsePropertyElement(Element ele, BeanDefinition bd) {
		// 获取property名字
		String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
		if (!StringUtils.hasLength(propertyName)) {
			error("Tag 'property' must have a 'name' attribute", ele);
			return;
		}
		this.parseState.push(new PropertyEntry(propertyName));
		try {
			// 如果同一个bean中存在了同名的property，就不进行解析，直接返回。
			if (bd.getPropertyValues().contains(propertyName)) {
				error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
				return;
			}
			
			// 这里是解析property值的地方，返回结果会封装到propertyValue中去
			Object val = parsePropertyValue(ele, bd, propertyName);
			PropertyValue pv = new PropertyValue(propertyName, val);
			parseMetaElements(ele, pv);
			pv.setSource(extractSource(ele));
			bd.getPropertyValues().addPropertyValue(pv);
		}
		finally {
			this.parseState.pop();
		}
	}

```

上面主要获取property的名字，并解析property值，那么接下来我们进入到parsePropertyValue方法中：

```java
	/**
	 * 获取属性元素的值
	 */
	@Nullable
	public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
		String elementName = (propertyName != null ?
				"<property> element for property '" + propertyName + "'" :
				"<constructor-arg> element");

		// 应该只有一个子元素：ref，value，list等
		NodeList nl = ele.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
					!nodeNameEquals(node, META_ELEMENT)) {
				// Child element is what we're looking for.
				if (subElement != null) {
					error(elementName + " must not contain more than one sub-element", ele);
				}
				else {
					subElement = (Element) node;
				}
			}
		}

		// 判断property的属性是ref还是value，不允许同时是ref和value
		boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
		boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);


		if ((hasRefAttribute && hasValueAttribute) ||
				((hasRefAttribute || hasValueAttribute) && subElement != null)) {
			error(elementName +
					" is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
		}

		// 如果是ref,创建一个ref的数据对象RuntimeBeanReference，这个对象封装了ref信息
		if (hasRefAttribute) {
			String refName = ele.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(refName)) {
				error(elementName + " contains empty 'ref' attribute", ele);
			}
			RuntimeBeanReference ref = new RuntimeBeanReference(refName);
			ref.setSource(extractSource(ele));
			return ref;
		}
		// 如果是value，创建一个value的数据对象TypeedStringValue,这个对象封装了value的信息
		else if (hasValueAttribute) {
			TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
			valueHolder.setSource(extractSource(ele));
			return valueHolder;
		}
		// 如果还有子元素，则对子元素进行解析
		else if (subElement != null) {
			return parsePropertySubElement(subElement, bd);
		}
		else {
			// Neither child element nor "ref" or "value" attribute found.
			error(elementName + " must specify a ref or value", ele);
			return null;
		}
	}
```

parsePropertySubElement则是对property子元素的解析，比如Array、List、Set、Map等等元素都会被解析，生成对应的数据对象，具体的解析过程有parseArrayElement、parseMapElement等等。就这样经过层层解析，我们在xml文件中定义的BeanDefinition 就被载入到了IOC容器中，并有对应的数据映射。经过此载入过程，IOC容器大致完成了管理Bean对象的数据准备工作，但是在Bean Definition中存在的还只是一些静态配置信息，接下来就是要向IOC容器注册数据。

#### beandefinition注册到IOC中

我们在之前分析过了bean definition的载入和解析，那么接下来就要对这些Bean Definition进行注册以供IOC容器使用，在DefaultlistableBeanFactory中，是通过一个HashMap来持有Bean Definition的，我们直接跟入到DefaultlistableBeanFactory的`registerBeanDefinition`：

```java
//---------------------------------------------------------------------
	// BeanDefinitionRegistry 接口的实现
	//---------------------------------------------------------------------

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}
		// 检查是不是有相同名字的Bean definition 已经注册在iOC容器中
		// 如果有，又不允许覆盖，则会抛出异常
		BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
		if (existingDefinition != null) {
			if (!isAllowBeanDefinitionOverriding()) {
				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
			}
			else if (existingDefinition.getRole() < beanDefinition.getRole()) {
				// e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
				if (logger.isInfoEnabled()) {
					logger.info("Overriding user-defined bean definition for bean '" + beanName +
							"' with a framework-generated bean definition: replacing [" +
							existingDefinition + "] with [" + beanDefinition + "]");
				}
			}
			else if (!beanDefinition.equals(existingDefinition)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Overriding bean definition for bean '" + beanName +
							"' with a different definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Overriding bean definition for bean '" + beanName +
							"' with an equivalent definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			this.beanDefinitionMap.put(beanName, beanDefinition);
		}
		else {
			// 判断是否已经有其他的 Bean 开始初始化了.
			// 注意，"注册Bean" 这个动作结束，Bean 依然还没有初始
			// 在 Spring 容器启动的最后，会 预初始化 所有的 singleton beans
			if (hasBeanCreationStarted()) {
				// 注册的过程需要同步操作，保证数据一致性
				synchronized (this.beanDefinitionMap) {
					// 将beanname作为key,把 bean definition作为value存入到beandefinitionmap中
					this.beanDefinitionMap.put(beanName, beanDefinition);
					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
					updatedDefinitions.addAll(this.beanDefinitionNames);
					updatedDefinitions.add(beanName);
					this.beanDefinitionNames = updatedDefinitions;
					if (this.manualSingletonNames.contains(beanName)) {
						Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
						updatedSingletons.remove(beanName);
						this.manualSingletonNames = updatedSingletons;
					}
				}
			}
			else {
				// Still in startup registration phase
				this.beanDefinitionMap.put(beanName, beanDefinition);
				this.beanDefinitionNames.add(beanName);
				this.manualSingletonNames.remove(beanName);
			}
			this.frozenBeanDefinitionNames = null;
		}

		if (existingDefinition != null || containsSingleton(beanName)) {
			resetBeanDefinition(beanName);
		}
	}
```

到了这里也就完成了beandefinition的注册，再次回到refresh方法：

```java
@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// 准备上下文
			prepareRefresh();

			// 这是在子类中去启动refreshBeanFactory()的地方
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// 准备需要在此上下文中使用的bean工厂
			prepareBeanFactory(beanFactory);

			try {
				// 设置beanFactory的后置处理
				postProcessBeanFactory(beanFactory);

				// 调用BeanFactory的后置处理器，这些后处理器是在bean定义中向容器注册的
				invokeBeanFactoryPostProcessors(beanFactory);

				// 注册bean的后处理器，在bean的创建过程中调用
				registerBeanPostProcessors(beanFactory);

				// 初始化次上下文的消息源.
				initMessageSource();

				// 初始化上下文中的事件机制
				initApplicationEventMulticaster();

				// 在特定的上下文子类中初始化其他特殊bean
				onRefresh();

				// 检查监听bean并将这些bean向容器注册
				registerListeners();

				// 实例化所有剩余（非延迟初始化）单例
				finishBeanFactoryInitialization(beanFactory);

				// 最后一步：发布相应的事件。结束Refresh过程
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// 为防止bean资源占用，在异常处理中，销毁已经在前面生成的单例bean
				destroyBeans();

				// 取消刷新上下文，重置ACTIVE标志
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
			}
		}
```

关于beandefinition的载入、解析、注册都是在obtainFreshBeanFactory中完成的，那么接下来就要讲解`prepareBeanFactory`:

```java
/**
	 *配置工厂的标准上下文特征，
	 *例如上下文的ClassLoader和后处理器。
	 * @param beanFactory the BeanFactory to configure
	 */
protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
  // 告诉内部bean工厂使用上下文的类加载器等。
  beanFactory.setBeanClassLoader(getClassLoader());

  // 设置BeanExpressionResolver
  beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
  beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

  // 添加一个 BeanPostProcessor，这个 processor 比较简单：
  // 实现了 Aware 接口的 beans 在初始化的时候，这个 processor 负责回调，
  // 这个我们很常用，如我们会为了获取 ApplicationContext 而 implement ApplicationContextAware
  // 注意：它不仅仅回调 ApplicationContextAware，
  //   还会负责回调 EnvironmentAware、ResourceLoaderAware 等
  beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

  // 如果某个 bean 依赖于以下几个接口的实现类，在自动装配的时候忽略它们，
  // Spring 会通过其他方式来处理这些依赖。
  beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
  beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
  beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
  beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
  beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
  beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

  /**
		 * 下面几行就是为特殊的几个 bean 赋值，如果有 bean 依赖了以下几个，会注入这边相应的值，
		 * 之前我们说过，"当前 ApplicationContext 持有一个 BeanFactory"，这里解释了第一行
		 * ApplicationContext 还继承了 ResourceLoader、ApplicationEventPublisher、MessageSource
		 * 所以对于这几个依赖，可以赋值为 this，注意 this 是一个 ApplicationContext
		 * 那这里怎么没看到为 MessageSource 赋值呢？那是因为 MessageSource 被注册成为了一个普通的 bean
		 */
  beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
  beanFactory.registerResolvableDependency(ResourceLoader.class, this);
  beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
  beanFactory.registerResolvableDependency(ApplicationContext.class, this);

  // 这个 BeanPostProcessor 也很简单，在 bean 实例化后，如果是 ApplicationListener 的子类，
  // 那么将其添加到 listener 列表中，可以理解成：注册 事件监听器
  beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

  // Detect a LoadTimeWeaver and prepare for weaving, if found.
  if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
    beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
    // Set a temporary ClassLoader for type matching.
    beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
  }

  // Register default environment beans.
  if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
    beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
  }
  if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
    beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
  }
  if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
    beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
  }
}
```

在准备好需要的bean工厂之后，就是一系列的对bean工厂的设置，比如设置beanfactory的后置处理，初始化上下文消息源，初始化上下文事件，监听bean ，之后就来到了bean factory实例化的真正地方：

#### Beanfactory 预初始化

继续refresh方法的finishBeanFactoryInitialization方法，跟入：

```java
/**
	 * 完成此上下文的bean工厂的初始化，
	 * 初始化所有剩余的单例bean。
	 */
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// 初始化此上下文的转换服务。
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// 如果没有bean后置处理器，就注入默认的值解析器
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// 停止使用临时ClassLoader进行类型匹配。
		beanFactory.setTempClassLoader(null);

		// 允许缓存所有bean definition元数据，不希望进一步的更改。
		beanFactory.freezeConfiguration();

		// 实例化所有剩余（非延迟初始化）单例。
		beanFactory.preInstantiateSingletons();
	}
```

那么上文的重点就在于`preInstantiateSingletons`, 它完成了所有非延迟初始化的单例bean的实例化工作，跟入:

```java
/**
	 * 对配置了lazy-init属性的Bean进行预实例化处理
	 * @throws BeansException
	 */
	@Override
	public void preInstantiateSingletons() throws BeansException {
		if (logger.isTraceEnabled()) {
			logger.trace("Pre-instantiating singletons in " + this);
		}

		// 保存了beandefinition的名字
		List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

		// 初始化所有非延迟加载的单例bean
		for (String beanName : beanNames) {
			// 返回合并的RootBeanDefinition，遍历父bean定义
			// 如果指定的bean对应于子bean定义。
			// <bean id="" class="" parent="" />  ，可先行了解下bean的继承
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			// 非抽象、非懒加载的 singletons。如果配置了 'abstract = true'，那是不需要初始化的
			if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
				if (isFactoryBean(beanName)) {
					// 若是FactoryBean ，在 beanName 前面加上 ‘&’ 符号。
					Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
					if (bean instanceof FactoryBean) {
						final FactoryBean<?> factory = (FactoryBean<?>) bean;
						boolean isEagerInit;
						if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
							isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>)
											((SmartFactoryBean<?>) factory)::isEagerInit,
									getAccessControlContext());
						}
						else {
							isEagerInit = (factory instanceof SmartFactoryBean &&
									((SmartFactoryBean<?>) factory).isEagerInit());
						}
						if (isEagerInit) {
							// 调用getBean方法，触发容器对Bean实例化和依赖注入过程
							getBean(beanName);
						}
					}
				}
				else {
					// 触发依赖注入
					getBean(beanName);
				}
			}
		}

		// 到这里说明所有的非懒加载的 singleton beans 已经完成了初始化
		for (String beanName : beanNames) {
			Object singletonInstance = getSingleton(beanName);
			if (singletonInstance instanceof SmartInitializingSingleton) {
				final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
						smartSingleton.afterSingletonsInstantiated();
						return null;
					}, getAccessControlContext());
				}
				else {
					smartSingleton.afterSingletonsInstantiated();
				}
			}
		}
	}

```

当Bean定义资源的<Bean>元素中配置了lazy-init属性时，容器将会在初始化的时候对所配置的Bean进行预实例化，Bean的依赖注入在容器初始化的时候就已经完成。这样，当应用程序第一次向容器索取被管理的Bean时，就不用再初始化和对Bean进行依赖注入了，直接从容器中获取已经完成依赖注入的现成Bean，可以提高应用第一次向容器获取Bean的性能。而接下来的文章将重点讲述依赖注入过程。

## 四：IOC容器的依赖注入

上面讲到了ioc容器的预初始化，当调用getBean方法的时候将会触发IOC容器的依赖注入，跟入到getbean方法中：

```java
@Override
public Object getBean(String name) throws BeansException {
  return doGetBean(name, null, null, false);
}

/**
	 * 返回指定bean的实例，该实例可以是共享的或独立的。
	 * 实际取得bean的地方，也是依赖注入发生的地方
	 */
@SuppressWarnings("unchecked")
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
                          @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

  final String beanName = transformedBeanName(name);
  Object bean;

  // 先从缓存中取得bean，处理那些已经被创建过的单例bean，对这种bean的请求不需要重复的创建
  Object sharedInstance = getSingleton(beanName);
  if (sharedInstance != null && args == null) {
    if (logger.isTraceEnabled()) {
      if (isSingletonCurrentlyInCreation(beanName)) {
        logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
                     "' that is not fully initialized yet - a consequence of a circular reference");
      }
      else {
        logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
      }
    }
    /**
			 * 这里的getObjectForBeanInstance完成的是Factory Bean的相关处理，用来取得Factory Bean的生产结果
			 */
    bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
  }

  else {
    // 如果我们已经创建了这个bean实例，则会失败：
    if (isPrototypeCurrentlyInCreation(beanName)) {
      throw new BeanCurrentlyInCreationException(beanName);
    }

    // 检查bean definition是否存在于IOC容器中
    // 检查是否能在当前的bean工厂中取得bean，如果取不到，就顺着双亲Bean Factory链中去取
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
      // Not found -> check parent.
      String nameToLookup = originalBeanName(name);
      if (parentBeanFactory instanceof AbstractBeanFactory) {
        return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
          nameToLookup, requiredType, args, typeCheckOnly);
      }
      else if (args != null) {
        // Delegation to parent with explicit args.
        return (T) parentBeanFactory.getBean(nameToLookup, args);
      }
      else if (requiredType != null) {
        // No args -> delegate to standard getBean method.
        return parentBeanFactory.getBean(nameToLookup, requiredType);
      }
      else {
        return (T) parentBeanFactory.getBean(nameToLookup);
      }
    }

    if (!typeCheckOnly) {
      markBeanAsCreated(beanName);
    }

    /**
			 * 到这里的话，要准备创建 Bean 了，对于 singleton 的 Bean 来说，容器中还没创建过此 Bean；
			 * 对于 prototype 的 Bean 来说，本来就是要创建一个新的 Bean。
			 */
    try {
      // 根据bean的名字获取bean definition
      final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
      checkMergedBeanDefinition(mbd, beanName, args);

      // 获取当前bean的所有依赖bean，这样就会触发get Bean的递归调用，直到取到一个没有任何依赖的bean为止
      // 这里的依赖指的是 depends-on 中定义的依赖
      String[] dependsOn = mbd.getDependsOn();
      if (dependsOn != null) {
        for (String dep : dependsOn) {
          // 检查是不是有循环依赖
          if (isDependent(beanName, dep)) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                            "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
          }
          registerDependentBean(dep, beanName);
          try {
            getBean(dep);
          }
          catch (NoSuchBeanDefinitionException ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                            "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
          }
        }
      }

      /*
				* 这里通过调用createBean方法创建单例bean的实例
				* */
      if (mbd.isSingleton()) {
        sharedInstance = getSingleton(beanName, () -> {
          try {
            return createBean(beanName, mbd, args);
          }
          catch (BeansException ex) {
            // Explicitly remove instance from singleton cache: It might have been put there
            // eagerly by the creation process, to allow for circular reference resolution.
            // Also remove any beans that received a temporary reference to the bean.
            destroySingleton(beanName);
            throw ex;
          }
        });
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
      }
      // 创建prototype bean的地方
      else if (mbd.isPrototype()) {
        // It's a prototype -> create a new instance.
        Object prototypeInstance = null;
        try {
          beforePrototypeCreation(beanName);
          prototypeInstance = createBean(beanName, mbd, args);
        }
        finally {
          afterPrototypeCreation(beanName);
        }
        bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
      }

      else {
        String scopeName = mbd.getScope();
        final Scope scope = this.scopes.get(scopeName);
        if (scope == null) {
          throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
        }
        try {
          Object scopedInstance = scope.get(beanName, () -> {
            beforePrototypeCreation(beanName);
            try {
              return createBean(beanName, mbd, args);
            }
            finally {
              afterPrototypeCreation(beanName);
            }
          });
          bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
        }
        catch (IllegalStateException ex) {
          throw new BeanCreationException(beanName,
                                          "Scope '" + scopeName + "' is not active for the current thread; consider " +
                                          "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                                          ex);
        }
      }
    }
    catch (BeansException ex) {
      cleanupAfterBeanCreationFailure(beanName);
      throw ex;
    }
  }

  // 对创建的Bean进行类型检查，如果没有问题，就返回这个新建的Bean（包含了依赖关系）
  if (requiredType != null && !requiredType.isInstance(bean)) {
    try {
      T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
      if (convertedBean == null) {
        throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
      }
      return convertedBean;
    }
    catch (TypeMismatchException ex) {
      if (logger.isTraceEnabled()) {
        logger.trace("Failed to convert bean '" + name + "' to required type '" +
                     ClassUtils.getQualifiedName(requiredType) + "'", ex);
      }
      throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
    }
  }
  return (T) bean;
}
```

get Bean就是依赖注入的入口，bean definition就是数据，那些核心实现就是createBean，它不但生成了需要的bean，还对bean的初始化做了处理，比如实现了BeanDefinition中的init-method属性定义、bean后置处理器等，那么接下来就进入到createBean方法中：

```java
protected abstract Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
  throws BeanCreationException;


/**
	 * 这个类的核心方法：创建一个bean实例，
	 * 填充bean实例，应用后处理器等。
	 * @see #doCreateBean
	 */
	@Override
	protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		if (logger.isTraceEnabled()) {
			logger.trace("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		// 判断创建的bean是否可以实例化，是否可以通过类加载器载入
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}

		// Prepare method overrides.
		try {
			mbdToUse.prepareMethodOverrides();
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

		try {
			// 如果bean配置了PostProcessor,那么返回的就是一个proxy
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		try {
			// 这里是创建bean的调用（重点）
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isTraceEnabled()) {
				logger.trace("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		}
		catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
			// A previously detected exception with proper bean creation context already,
			//			// or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
		}
	}

```

create Bean方法中最核心的方法就是doCreateBean,跟入：

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

		// beanWrapper是用来持有创建出来的bean对象
		BeanWrapper instanceWrapper = null;
		// 如果是Singleton，则要先把缓存中的同名bean清除
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
			// 这里是创建bean的地方，由create BeanInstance来完成（重点）
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		final Object bean = instanceWrapper.getWrappedInstance();
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		// Allow post-processors to modify the merged bean definition.
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// 这里是对bean的初始化，依赖注入往往是在这里发生，这个exposedObject在初始化处理完以后
		// 会返回作为依赖注入完成后的Bean
		Object exposedObject = bean;
		try {
			// 负责属性装配，前面的实例只是实例化，在这里负责设置值（重点）
			populateBean(beanName, mbd, instanceWrapper);
			// 处理 bean 初始化完成后的各种回调
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			Object earlySingletonReference = getSingleton(beanName, false);
			if (earlySingletonReference != null) {
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		try {
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}
```

到这里我们大概的讲完了doCreateBean方法，其中两个关键的方法就是创建bean实例的`createBeanInstance` 和依赖注入的`populateBean`，接下来依次介绍：

**createBeanInstance**

```java
/**
	 * 使用适当的实例化策略为指定的bean创建新实例：
	 * 工厂方法，构造函数自动装配或简单实例化。
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @param args explicit arguments to use for constructor or factory method invocation
	 * @return a BeanWrapper for the new instance
	 * @see #obtainFromSupplier
	 * @see #instantiateUsingFactoryMethod
	 * @see #autowireConstructor
	 * @see #instantiateBean
	 */
	protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
		// 确认需要创建bean实例的类可以实例化
		Class<?> beanClass = resolveBeanClass(mbd, beanName);

		// 使用工厂方法对Bean进行实例化
		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
		}

		Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
		if (instanceSupplier != null) {
			return obtainFromSupplier(instanceSupplier, beanName);
		}

		if (mbd.getFactoryMethodName() != null) {
			return instantiateUsingFactoryMethod(beanName, mbd, args);
		}

		// 如果不是第一次创建，比如第二次创建 prototype bean。
		// 这种情况下，我们可以从第一次创建知道，采用无参构造函数，还是构造函数依赖注入 来完成实例化
		boolean resolved = false;
		boolean autowireNecessary = false;
		if (args == null) {
			synchronized (mbd.constructorArgumentLock) {
				if (mbd.resolvedConstructorOrFactoryMethod != null) {
					resolved = true;
					autowireNecessary = mbd.constructorArgumentsResolved;
				}
			}
		}
		if (resolved) {
			if (autowireNecessary) {
				// 构造函数依赖注入
				return autowireConstructor(beanName, mbd, null, null);
			}
			else {
				// 无参构造函数实例化
				return instantiateBean(beanName, mbd);
			}
		}

		// 使用构造函数对Bean进行实例化
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			return autowireConstructor(beanName, mbd, ctors, args);
		}

		// Preferred constructors for default construction?
		ctors = mbd.getPreferredConstructors();
		if (ctors != null) {
			return autowireConstructor(beanName, mbd, ctors, null);
		}

		// 使用默认的构造函数对Bean实例化，最常见的（重点）
		return instantiateBean(beanName, mbd);
	}
```

最常见的其实就是使用默认的构造函数对Bean进行实例化，接下就看下怎么实现的：`instantiateBean`

```java
/**
	 * 使用其默认构造函数实例化给定的bean。
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @return a BeanWrapper for the new instance
	 */
	protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
		/**
		 * 使用默认的实例化策略对bean进行实例化，默认的实例化策略是：
		 * CglibsubclassingInstantiationStrategy,也就是使用CGLIB来对bean进行实例化
		 */
		try {
			Object beanInstance;
			final BeanFactory parent = this;
			if (System.getSecurityManager() != null) {
				beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
						getInstantiationStrategy().instantiate(mbd, beanName, parent),
						getAccessControlContext());
			}
			else {
				// 核心方法就在这边，获取实例化策略进行实例化
				beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
			}
			BeanWrapper bw = new BeanWrapperImpl(beanInstance);
			initBeanWrapper(bw);
			return bw;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
		}
	}
```

上面有个很重要的方法就是etInstantiationStrategy().instantiate(mbd, beanName, parent)，通过选择实例化策略进行实例化，我们跟入进去查看,它会选择默认的实例化策略来去实例化（通过cglib对bean进行实例化）cglib是一个常用的字节码生成器的类库，它主要可以生成和转换Java字节码。回到instantiate中，

```java
@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		// 如果不存在方法覆写，那就使用 java 反射进行实例化，否则使用 CGLIB,
		if (!bd.hasMethodOverrides()) {
			Constructor<?> constructorToUse;
			synchronized (bd.constructorArgumentLock) {
				constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse == null) {
					final Class<?> clazz = bd.getBeanClass();
					if (clazz.isInterface()) {
						throw new BeanInstantiationException(clazz, "Specified class is an interface");
					}
					try {
						if (System.getSecurityManager() != null) {
							constructorToUse = AccessController.doPrivileged(
									(PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor);
						}
						else {
							constructorToUse = clazz.getDeclaredConstructor();
						}
						bd.resolvedConstructorOrFactoryMethod = constructorToUse;
					}
					catch (Throwable ex) {
						throw new BeanInstantiationException(clazz, "No default constructor found", ex);
					}
				}
			}
			// 利用构造方法进行实例化
			return BeanUtils.instantiateClass(constructorToUse);
		}
		else {
			// 存在方法覆写，利用 CGLIB 来完成实例化，需要依赖于 CGLIB 生成子类，这里就不展开了。
			// tips: 因为如果不使用 CGLIB 的话，存在 override 的情况 JDK 并没有提供相应的实例化支持
			return instantiateWithMethodInjection(bd, beanName, owner);
		}
	}
```

到此为止已经分析了实例化Bean对象的整个过程，那么接下来就是依赖注入的过程了，核心方法AbstractAutowireCapableBeanFactory.docreateBean中的populateBean方法。

**populateBean**

```java
/**
	 * 使用bean定义中的属性值填充给定BeanWrapper中的bean实例。
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @param bw the BeanWrapper with bean instance
	 */
@SuppressWarnings("deprecation")  // for postProcessPropertyValues
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
  if (bw == null) {
    // 获取beanDefinition中设置的property值
    if (mbd.hasPropertyValues()) {
      throw new BeanCreationException(
        mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
    }
    else {
      // 跳过null实例的属性填充阶段。
      return;
    }
  }
  boolean continueWithPropertyPopulation = true;

  if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
    for (BeanPostProcessor bp : getBeanPostProcessors()) {
      if (bp instanceof InstantiationAwareBeanPostProcessor) {
        InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
        if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
          continueWithPropertyPopulation = false;
          break;
        }
      }
    }
  }

  if (!continueWithPropertyPopulation) {
    return;
  }

  PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

  // 开始进行依赖注入过程 ，先处理autowire的注入
  if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME || mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
    MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
    // 根据bean的名字来完成bean的autowire
    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME) {
      autowireByName(beanName, mbd, bw, newPvs);
    }
    // 按bean类型添加基于autowire的属性值。
    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
      autowireByType(beanName, mbd, bw, newPvs);
    }
    pvs = newPvs;
  }

  boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
  boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

  PropertyDescriptor[] filteredPds = null;
  if (hasInstAwareBpps) {
    if (pvs == null) {
      pvs = mbd.getPropertyValues();
    }
    for (BeanPostProcessor bp : getBeanPostProcessors()) {
      if (bp instanceof InstantiationAwareBeanPostProcessor) {
        InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
        PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
        if (pvsToUse == null) {
          if (filteredPds == null) {
            filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
          }
          pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
          if (pvsToUse == null) {
            return;
          }
        }
        pvs = pvsToUse;
      }
    }
  }
  if (needsDepCheck) {
    if (filteredPds == null) {
      filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
    }
    checkDependencies(beanName, mbd, filteredPds, pvs);
  }

  if (pvs != null) {
    // 对属性进行注入
    applyPropertyValues(beanName, mbd, bw, pvs);
  }
}
```

通过上面的applyPropertyValues来了解下具体的对属性进行解析然后注入的过程，进入方法：

```java
protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
		if (pvs.isEmpty()) {
			return;
		}

		if (System.getSecurityManager() != null && bw instanceof BeanWrapperImpl) {
			((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
		}

		MutablePropertyValues mpvs = null;
		List<PropertyValue> original;

		if (pvs instanceof MutablePropertyValues) {
			mpvs = (MutablePropertyValues) pvs;
			if (mpvs.isConverted()) {
				// Shortcut: use the pre-converted values as-is.
				try {
					bw.setPropertyValues(mpvs);
					return;
				}
				catch (BeansException ex) {
					throw new BeanCreationException(
							mbd.getResourceDescription(), beanName, "Error setting property values", ex);
				}
			}
			original = mpvs.getPropertyValueList();
		}
		else {
			original = Arrays.asList(pvs.getPropertyValues());
		}

		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}
		// BeanDefinitionValueResolver对 beandefinition的解析是在valueResolver中完成的
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

		// 这里为解析值创建一个副本，副本的数据会被注入到bean中
		List<PropertyValue> deepCopy = new ArrayList<>(original.size());
		boolean resolveNecessary = false;
		for (PropertyValue pv : original) {
			if (pv.isConverted()) {
				deepCopy.add(pv);
			}
			else {
				String propertyName = pv.getName();
				Object originalValue = pv.getValue();
				Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
				Object convertedValue = resolvedValue;
				boolean convertible = bw.isWritableProperty(propertyName) &&
						!PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
				if (convertible) {
					convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
				}
				// Possibly store converted value in merged bean definition,
				// in order to avoid re-conversion for every created bean instance.
				if (resolvedValue == originalValue) {
					if (convertible) {
						pv.setConvertedValue(convertedValue);
					}
					deepCopy.add(pv);
				}
				else if (convertible && originalValue instanceof TypedStringValue &&
						!((TypedStringValue) originalValue).isDynamic() &&
						!(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
					pv.setConvertedValue(convertedValue);
					deepCopy.add(pv);
				}
				else {
					resolveNecessary = true;
					deepCopy.add(new PropertyValue(pv, convertedValue));
				}
			}
		}
		if (mpvs != null && !resolveNecessary) {
			mpvs.setConverted();
		}

		// Set our (possibly massaged) deep copy.
		try {
			// 这里是依赖注入的地方，会在beanwrapperimpl中完成（核心方法）
			bw.setPropertyValues(new MutablePropertyValues(deepCopy));
		}
		catch (BeansException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Error setting property values", ex);
		}
	}
```

最核心的方法就是bw.setPropertyValues(new MutablePropertyValues(deepCopy))，依赖注入的发生就是在这里实现的，进入到源码中：

```java
@Override
	public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
			throws BeansException {

		List<PropertyAccessException> propertyAccessExceptions = null;
		List<PropertyValue> propertyValues = (pvs instanceof MutablePropertyValues ?
				((MutablePropertyValues) pvs).getPropertyValueList() : Arrays.asList(pvs.getPropertyValues()));
		for (PropertyValue pv : propertyValues) {
			try {
				// 核心方法
				setPropertyValue(pv);
			}
			catch (NotWritablePropertyException ex) {
				if (!ignoreUnknown) {
					throw ex;
				}
				// Otherwise, just ignore it and continue...
			}
			catch (NullValueInNestedPathException ex) {
				if (!ignoreInvalid) {
					throw ex;
				}
				// Otherwise, just ignore it and continue...
			}
			catch (PropertyAccessException ex) {
				if (propertyAccessExceptions == null) {
					propertyAccessExceptions = new ArrayList<>();
				}
				propertyAccessExceptions.add(ex);
			}
		}

		// If we encountered individual exceptions, throw the composite exception.
		if (propertyAccessExceptions != null) {
			PropertyAccessException[] paeArray = propertyAccessExceptions.toArray(new PropertyAccessException[0]);
			throw new PropertyBatchUpdateException(paeArray);
		}
	}
```

上面核心方法就是setPropertyValue,跟入：

```java
@Override
public void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException {
  AbstractNestablePropertyAccessor nestedPa;
  try {
    nestedPa = getPropertyAccessorForPropertyPath(propertyName);
  }
  catch (NotReadablePropertyException ex) {
    throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
                                           "Nested property in path '" + propertyName + "' does not exist", ex);
  }
  // 设置token的keys和索引
  PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
  nestedPa.setPropertyValue(tokens, new PropertyValue(propertyName, value));
}
```

```java
protected void setPropertyValue(PropertyTokenHolder tokens, PropertyValue pv) throws BeansException {
  if (tokens.keys != null) {
    processKeyedProperty(tokens, pv);
  }
  else {
    processLocalProperty(tokens, pv);
  }
}
```

继续跟入到processkeyedproperty:

```java
private void processKeyedProperty(PropertyTokenHolder tokens, PropertyValue pv) {
		Object propValue = getPropertyHoldingValue(tokens);
		PropertyHandler ph = getLocalPropertyHandler(tokens.actualName);
		if (ph == null) {
			throw new InvalidPropertyException(
					getRootClass(), this.nestedPath + tokens.actualName, "No property handler found");
		}
		Assert.state(tokens.keys != null, "No token keys");
		String lastKey = tokens.keys[tokens.keys.length - 1];

		// 对Array进行注入
		if (propValue.getClass().isArray()) {
			Class<?> requiredType = propValue.getClass().getComponentType();
			int arrayIndex = Integer.parseInt(lastKey);
			Object oldValue = null;
			try {
				if (isExtractOldValueForEditor() && arrayIndex < Array.getLength(propValue)) {
					oldValue = Array.get(propValue, arrayIndex);
				}
				Object convertedValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
						requiredType, ph.nested(tokens.keys.length));
				int length = Array.getLength(propValue);
				if (arrayIndex >= length && arrayIndex < this.autoGrowCollectionLimit) {
					Class<?> componentType = propValue.getClass().getComponentType();
					Object newArray = Array.newInstance(componentType, arrayIndex + 1);
					System.arraycopy(propValue, 0, newArray, 0, length);
					setPropertyValue(tokens.actualName, newArray);
					propValue = getPropertyValue(tokens.actualName);
				}
				Array.set(propValue, arrayIndex, convertedValue);
			}
			catch (IndexOutOfBoundsException ex) {
				throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
						"Invalid array index in property path '" + tokens.canonicalName + "'", ex);
			}
		}

		// 对List进行注入
		else if (propValue instanceof List) {
			Class<?> requiredType = ph.getCollectionType(tokens.keys.length);
			List<Object> list = (List<Object>) propValue;
			int index = Integer.parseInt(lastKey);
			Object oldValue = null;
			if (isExtractOldValueForEditor() && index < list.size()) {
				oldValue = list.get(index);
			}
			Object convertedValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
					requiredType, ph.nested(tokens.keys.length));
			int size = list.size();
			if (index >= size && index < this.autoGrowCollectionLimit) {
				for (int i = size; i < index; i++) {
					try {
						list.add(null);
					}
					catch (NullPointerException ex) {
						throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
								"Cannot set element with index " + index + " in List of size " +
								size + ", accessed using property path '" + tokens.canonicalName +
								"': List does not support filling up gaps with null elements");
					}
				}
				list.add(convertedValue);
			}
			else {
				try {
					list.set(index, convertedValue);
				}
				catch (IndexOutOfBoundsException ex) {
					throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
							"Invalid list index in property path '" + tokens.canonicalName + "'", ex);
				}
			}
		}
		// 对map进行注入
		else if (propValue instanceof Map) {
			Class<?> mapKeyType = ph.getMapKeyType(tokens.keys.length);
			Class<?> mapValueType = ph.getMapValueType(tokens.keys.length);
			Map<Object, Object> map = (Map<Object, Object>) propValue;
			// IMPORTANT: Do not pass full property name in here - property editors
			// must not kick in for map keys but rather only for map values.
			TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(mapKeyType);
			Object convertedMapKey = convertIfNecessary(null, null, lastKey, mapKeyType, typeDescriptor);
			Object oldValue = null;
			if (isExtractOldValueForEditor()) {
				oldValue = map.get(convertedMapKey);
			}
			// Pass full property name and old value in here, since we want full
			// conversion ability for map values.
			Object convertedMapValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
					mapValueType, ph.nested(tokens.keys.length));
			map.put(convertedMapKey, convertedMapValue);
		}

		else {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Property referenced in indexed property path '" + tokens.canonicalName +
					"' is neither an array nor a List nor a Map; returned value was [" + propValue + "]");
		}
	}

```

以上分别对array、list、map进行了注入，到此为止依赖注入也就完成了。

****

## 五：IOC容器的高级特性

TODO 完善整篇文章的目录以及一些解释以及高级特性





