# Springmvc源码分析

## 一：spring mvc请求处理流程

首先分享一张Spring in action 的流程图：

![image-20190329140443795](https://ws2.sinaimg.cn/large/006tKfTcly1g1jmpy9xfaj30wi0u0wy6.jpg)

那么接下来我们根据这张图理解下：

1. 



## 二：Spring mvc 工作的机制

在容器初始化时会建立所有 url 和 Controller 的对应关系,保存到 Map<url,Controller> 中.Tomcat 启动时会通知 Spring 初始化容器(加载 Bean 的定义信息和初始化所有单例 Bean),然后 SpringMVC 会遍历容器中的 Bean,获取每一个 Controller 中的所有方法访问的 url,然后将 url 和 Controller 保存到一个 Map 中; 

这样就可以根据 Request 快速定位到 Controller,因为最终处理 Request 的是 Controller 中的 方法,Map 中只保留了 **url 和 Controller 中的对应关系**,所以要根据 Request 的 url 进一步确认 Controller 中的 Method,这一步工作的原理就是**拼接 Controller 的 url(Controller 上 @RequestMapping 的值)和方法的 url(Method 上@RequestMapping 的值),与 request 的 url 进行匹 配,找到匹配的那个方法;** 

确定处理请求的 Method 后,接下来的任务就是参数绑定,把 Request 中参数绑定到方法的形式参数 上,这一步是整个请求处理过程中最复杂的一个步骤。SpringMVC提供了两种Request参数与方法形参 的绑定方法:

1. 通过注解进行绑定,@RequestParam 
2.  通过参数名称进行绑定. 

使用注解进行绑定,我们只要在方法参数前面声明@RequestParam("a"),就可以将 Request 中参 数 a 的值绑定到方法的该参数上.使用参数名称进行绑定的前提是必须要获取方法中参数的名称,Java 反射只提供了获取方法的参数的类型,并没有提供获取参数名称的方法.SpringMVC 解决这个问题的方 法是用 asm 框架读取字节码文件,来获取方法的参数名称

## 三：spring mvc 源码分析

我们直接跟做Springmvc的工作机制来分析源码：

1. IOC初始化事建立所有URL和Controller类的对应关系
2. 根据请求的URL找到对应的Controller，并从Controller中找到处理请求的方法
3. request 参数绑定到方法的形参,执行方法处理请求,并返回结果视图.

### 1.建立URL和Controller的对应关系

首先我们找到入口类方法：ApplicationObjectSupport.setApplicationContext(@Nullable ApplicationContext context)，setApplicationContext 方法中核心 部 分 就 是 初 始 化 容 器 initApplicationContext(context), 子 类AbstractDetectingUrlHandlerMapping 实现了该方法,所以我们直接看子类中的初始化容器方法。

```java
public void initApplicationContext() throws ApplicationContextException {
  super.initApplicationContext();
  detectHandlers();
}

/**
	 * 建立当前 ApplicationContext 中的所有 Controller 和 url 的对应关系
	 */
protected void detectHandlers() throws BeansException {
  ApplicationContext applicationContext = obtainApplicationContext();
  // 获取 ApplicationContext 容器中所有 bean 的 Name
  String[] beanNames = (this.detectHandlersInAncestorContexts ?
                        BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, Object.class) :
                        applicationContext.getBeanNamesForType(Object.class));

  // 遍历 beanNames,并找到这些 bean 对应的 url
  for (String beanName : beanNames) {
    // 找 bean 上的所有 url(Controller 上的 url+方法上的 url),该方法由对应的子类实现
    String[] urls = determineUrlsForHandler(beanName);
    if (!ObjectUtils.isEmpty(urls)) {
      // // 保存 urls 和 beanName 的对应关系,put it to Map<urls,beanName>
      // 该方法在父类 AbstractUrlHandlerMapping中实现
      registerHandler(urls, beanName);
    }
  }

  if ((logger.isDebugEnabled() && !getHandlerMap().isEmpty()) || logger.isTraceEnabled()) {
    logger.debug("Detected " + getHandlerMap().size() + " mappings in " + formatMappingName());
  }
}


/**
	 *获取 Controller 中所有方法的 url,由子类实现,典型的模板模式
	 */
protected abstract String[] determineUrlsForHandler(String beanName);

}
```

determineUrlsForHandler(String beanName)方法的作用是获取每个 Controller 中的 url,不同
的子类有不同的实现,这是一个典型的模板设计模式.因为开发中我们用的最多的就是用注解来配置
Controller 中 的 url,BeanNameUrlHandlerMapping 是 AbstractDetectingUrlHandlerMapping
的子类,处理注解形式的 url 映射.所以我们这里以 BeanNameUrlHandlerMapping 来进行分析.我们看
BeanNameUrlHandlerMapping 是如何查 beanName 上所有映射的 url.继续跟入到源码：

```java
/**
	 * 检查URL的给定bean的名称和别名，以“/”开头。
	 */
	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		List<String> urls = new ArrayList<>();
		if (beanName.startsWith("/")) 
      // 如果bean的名称以/开头就添加
			urls.add(beanName);
		}
		String[] aliases = obtainApplicationContext().getAliases(beanName);
		for (String alias : aliases) {
			if (alias.startsWith("/")) {
        // 如果别名的名称以/开头就添加
				urls.add(alias);
			}
		}
		return StringUtils.toStringArray(urls);
	}
```

到这里为止，HandlerMapping 组件就已经建立所有 url 和 Controller 的对应关系。

接下来就要根据访问 url 找到对应的 Controller 中处理请求的方法。

###2.根据请求URL查找对应Controller中的方法

第二个步骤是由请求触发的,所以入口为 DispatcherServlet 的核心方
法为 doService(),doService()中的核心逻辑由 doDispatch()实现,我们查看 doDispatch()的源代
码.

```java
/**
	 * 处理实际调度到处理程序。
	 * <p>将通过按顺序应用servlet的HandlerMappings来获取处理程序。
	 * *将通过查询servlet安装的HandlerAdapter获取HandlerAdapter
	 * *找到第一个支持处理程序类。
	 * <p>此方法处理所有HTTP方法。 这取决于HandlerAdapters或处理程序
	 * *他们自己决定哪些方法是可以接受的。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception in case of any kind of processing failure
	 */
	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				// 1. 检查是否是文件上传的请求
				processedRequest = checkMultipart(request);
				multipartRequestParsed = (processedRequest != request);

				// 2.取得处理当前请求的 Controller,这里也称为 hanlder,处理器,
				// 第一个步骤的意义就在这里体现了.这里并不是直接返回 Controller,
				// 而是返回的 HandlerExecutionChain 请求处理器链对象,
				// 该对象封装了 handler 和 interceptors.
				mappedHandler = getHandler(processedRequest);
				// 如果 handler 为空,则返回 404
				if (mappedHandler == null) {
					noHandlerFound(processedRequest, response);
					return;
				}

				//3. 获取处理 request 的处理器适配器 handler adapter
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// 处理 last-modified 请求头
				String method = request.getMethod();
				boolean isGet = "GET".equals(method);
				if (isGet || "HEAD".equals(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
				}

				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					return;
				}

				// 4.实际的处理器处理请求,返回结果视图对象
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

				if (asyncManager.isConcurrentHandlingStarted()) {
					return;
				}
				// 结果视图对象的处理
				applyDefaultViewName(processedRequest, mv);
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			}
			catch (Exception ex) {
				dispatchException = ex;
			}
			catch (Throwable err) {
				// As of 4.3, we're processing Errors thrown from handler methods as well,
				// making them available for @ExceptionHandler methods and other scenarios.
				dispatchException = new NestedServletException("Handler dispatch failed", err);
			}
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		}
		catch (Exception ex) {
			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		}
		catch (Throwable err) {
			triggerAfterCompletion(processedRequest, response, mappedHandler,
					new NestedServletException("Handler processing failed", err));
		}
		finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// Instead of postHandle and afterCompletion
				if (mappedHandler != null) {
					// 请求成功响应之后的方法
					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				}
			}
			else {
				// Clean up any resources used by a multipart request.
				if (multipartRequestParsed) {
					cleanupMultipart(processedRequest);
				}
			}
		}
	}
```

getHandler(processedRequest) 方 法 实 际 上 就 是 从 HandlerMapping 中 找 到 url 和
Controller 的对应关系.这也就是第一个步骤:建立 Map<url,Controller>的意义.我们知道,最终处
理 Request 的是 Controller 中的方法,我们现在只是知道了 Controller,还要进一步确认
Controller 中处理 Request 的方法.由于下面的步骤和第三个步骤关系更加紧密,直接转到第三个步
骤.

### 3.反射调用处理请求的方法，返回结果视图

上面的方法中,第 2 步其实就是从第一个步骤中的 Map<urls,beanName>中取得 Controller,然后经过
拦截器的预处理方法,到最核心的部分--第 5 步调用 Controller 的方法处理请求.在第 2 步中我们可以
知道处理 Request 的 Controller,第 5 步就是要根据 url 确定 Controller 中处理请求的方法,然后
通过反射获取该方法上的注解和参数,解析方法和参数上的注解,最后反射调用方法获取 ModelAndView
结果视图。因为上面采用注解 url 形式说明的.第 5 步调用的就是 RequestMappingHandlerAdapter类
的handleInternal(HttpServletRequest request,HttpServletResponse response, HandlerMethod handlerMethod)实现。

```java
protected ModelAndView handleInternal(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

		ModelAndView mav;
		checkRequest(request);

		// Execute invokeHandlerMethod in synchronized block if required.
		if (this.synchronizeOnSession) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					mav = invokeHandlerMethod(request, response, handlerMethod);
				}
			}
			else {
				// No HttpSession available -> no mutex necessary
				mav = invokeHandlerMethod(request, response, handlerMethod);
			}
		}
		else {
			// No synchronization on session demanded at all...
			mav = invokeHandlerMethod(request, response, handlerMethod);
		}

		if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
			if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
				applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
			}
			else {
				prepareResponse(response);
			}
		}

		return mav;
	}
```

上面这段代码属于第5步，但是整个过程最重要的就是第二步和第4步，我们再次回到第2步的gethandler()方法中,这步通过 Request 找 Controller 的处理方法.实际上就是拼接Controller 的 url 和方法的 url,与 Request 的 url 进行匹配,找到匹配的方法.我们跟着不停的跟踪源码，进入到AbstractHandlerMapping的getHandlerInternal方法，然后接着跟踪到了AbstractHandlerMethodMapping的getHandlerInternal中：

```java
/**
	 * 查找给定请求的处理程序方法。
	 */
	@Override
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		// 如果请求 url 为,http://localhost:8080/web/hello.json, 则 lookupPath=web/hello.json
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		this.mappingRegistry.acquireReadLock();
		try {
			// 遍历 Controller 上的所有方法,获取 url 匹配的方法
			HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
			return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
		}
		finally {
			this.mappingRegistry.releaseReadLock();
		}
	}
```

通过上面的代码,已经可以找到处理 Request 的 Controller 中的请求方法了,现在看如何解析该方法
上的参数,并调用该方法。也就是执行方法这一步。执行方法这一步最重要的就是获取方法的参数,然后
我们就可以反射调用方法了。回到上面标记的第4步：`mv = ha.handle(processedRequest, response, mappedHandler.getHandler());`跟入进去：AbstractHandlerMethodAdapter的handleInternal，再继续跟入到RequestMappingHandlerAdapter的invokeHandlerMethod(request, response, handlerMethod)，我们来看下这个里面的源码：

```java
/**
	 *获取处理请求的方法,执行并返回结果视图
	 * @since 4.2
	 * @see #createInvocableHandlerMethod(HandlerMethod)
	 */
	@Nullable
	protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		try {
			WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
			ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);

			ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
			if (this.argumentResolvers != null) {
				invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
			}
			if (this.returnValueHandlers != null) {
				invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
			}
			invocableMethod.setDataBinderFactory(binderFactory);
			invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);

			ModelAndViewContainer mavContainer = new ModelAndViewContainer();
			mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
			modelFactory.initModel(webRequest, mavContainer, invocableMethod);
			mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);

			AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
			asyncWebRequest.setTimeout(this.asyncRequestTimeout);

			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
			asyncManager.setTaskExecutor(this.taskExecutor);
			asyncManager.setAsyncWebRequest(asyncWebRequest);
			asyncManager.registerCallableInterceptors(this.callableInterceptors);
			asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);

			if (asyncManager.hasConcurrentResult()) {
				Object result = asyncManager.getConcurrentResult();
				mavContainer = (ModelAndViewContainer) asyncManager.getConcurrentResultContext()[0];
				asyncManager.clearConcurrentResult();
				LogFormatUtils.traceDebug(logger, traceOn -> {
					String formatted = LogFormatUtils.formatValue(result, !traceOn);
					return "Resume with async result [" + formatted + "]";
				});
				invocableMethod = invocableMethod.wrapConcurrentResult(result);
			}

			invocableMethod.invokeAndHandle(webRequest, mavContainer);
			if (asyncManager.isConcurrentHandlingStarted()) {
				return null;
			}

			return getModelAndView(mavContainer, modelFactory, webRequest);
		}
		finally {
			webRequest.requestCompleted();
		}
	}
```

invocableMethod.invokeAndHandle 最终要实现的目的就是:完成 Request 中的参数和方法参数上数
据的绑定。
SpringMVC 中提供两种 Request 参数到方法中参数的绑定方式:

1. 通过注解进行绑定,@RequestParam
2. 通过参数名称进行绑定.

使用注解进行绑定,我们只要在方法参数前面声明@RequestParam("a"),就可以将 request 中参数 a 的值绑定到方法的该参数上.使用参数名称进行绑定的前提是必须要获取方法中参数的名称,Java 反射只提供了获取方法的参数的类型,并没有提供获取参数名称的方法.SpringMVC 解决这个问题的方 法是用 asm 框架读取字节码文件,来获取方法的参数名称.asm 框架是一个字节码操作框架,关于 asm 更 多介绍可以参考它的官网.个人建议,使用注解来完成参数绑定,这样就可以省去 asm 框架的读取字节码 的操作. 

这是invokeAndHandle的源码：

```java
public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
		setResponseStatus(webRequest);

		if (returnValue == null) {
			if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
				mavContainer.setRequestHandled(true);
				return;
			}
		}
		else if (StringUtils.hasText(getResponseStatusReason())) {
			mavContainer.setRequestHandled(true);
			return;
		}

		mavContainer.setRequestHandled(false);
		Assert.state(this.returnValueHandlers != null, "No return value handlers");
		try {
			this.returnValueHandlers.handleReturnValue(
					returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
		}
		catch (Exception ex) {
			if (logger.isTraceEnabled()) {
				logger.trace(formatErrorForReturnValue(returnValue), ex);
			}
			throw ex;
		}
	}



public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
		if (logger.isTraceEnabled()) {
			logger.trace("Arguments: " + Arrays.toString(args));
		}
		return doInvoke(args);
	}

protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		if (ObjectUtils.isEmpty(getMethodParameters())) {
			return EMPTY_ARGS;
		}
		MethodParameter[] parameters = getMethodParameters();
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			MethodParameter parameter = parameters[i];
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
			args[i] = findProvidedArgument(parameter, providedArgs);
			if (args[i] != null) {
				continue;
			}
			if (!this.resolvers.supportsParameter(parameter)) {
				throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
			}
			try {
				args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
			}
			catch (Exception ex) {
				// Leave stack trace for later, exception may actually be resolved and handled..
				if (logger.isDebugEnabled()) {
					String error = ex.getMessage();
					if (error != null && !error.contains(parameter.getExecutable().toGenericString())) {
						logger.debug(formatArgumentError(parameter, error));
					}
				}
				throw ex;
			}
		}
		return args;
	}

```

到这里,方法的参数值列表也获取到了,就可以直接进行方法的调用了.整个请求过程中最复杂的一
步就是在这里了.ok,到这里整个请求处理过程的关键步骤都分析完了.理解了 SpringMVC 中的请求处
理流程,整个代码还是比较清晰的.

---

## 四：Spring mvc 优化

根据对Springmvc的工作原理，可以有以下几个优化点：

1. Controller 如果能保持单例,尽量使用单例,这样可以减少创建对象和回收对象的开销.也就是 说,如果 Controller 的类变量和实例变量可以以方法形参声明的尽量以方法的形参声明,不要以类变量 和实例变量声明,这样可以避免线程安全问题.
2. 处理 Request 的方法中的形参务必加上@RequestParam 注解,这样可以避免 SpringMVC 使用 asm 框架读取 class 文件获取方法参数名的过程.即便 SpringMVC 对读取出的方法参数名进行了缓存, 如果不要读取class 文件当然是更加好.  
3. 阅读源码的过程中,发现 SpringMVC 并没有对处理 url 的方法进行缓存,也就是说每次都要根据请求 url 去匹配 Controller 中的方法 url,如果把 url 和 Method 的关系缓存起来,会不会带来性能上的提升呢?有点恶心的是,负责解析 url 和 Method 对应关系的 ServletHandlerMethodResolver 是一个 private 的内部类,不能直接继承该类增强代码,必须要该代码后重新编译.当然,如果缓存起来,必须要考虑缓存的线程安全问题。

---





