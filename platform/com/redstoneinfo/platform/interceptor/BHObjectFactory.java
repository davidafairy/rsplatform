package com.redstoneinfo.platform.interceptor;

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.struts2.StrutsConstants;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.WebApplicationContext;

import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.spring.SpringObjectFactory;
import com.opensymphony.xwork2.util.reflection.ReflectionException;
import com.opensymphony.xwork2.util.reflection.ReflectionExceptionHandler;

@Controller
public class BHObjectFactory extends SpringObjectFactory {

	public BHObjectFactory() {

	}

	@Inject
	public BHObjectFactory(
			@Inject(value = StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_AUTOWIRE, required = false) String autoWire,
			@Inject(value = StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_AUTOWIRE_ALWAYS_RESPECT, required = false) String alwaysAutoWire,
			@Inject(value = StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_USE_CLASS_CACHE, required = false) String useClassCacheStr,
			@Inject ServletContext servletContext,
			@Inject(StrutsConstants.STRUTS_DEVMODE) String devMode,
			@Inject Container container) {

		super();
		boolean useClassCache = "true".equals(useClassCacheStr);

		Object rootWebApplicationContext = servletContext
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		if (rootWebApplicationContext instanceof RuntimeException) {
			RuntimeException runtimeException = (RuntimeException) rootWebApplicationContext;
			return;
		}

		ApplicationContext appContext = (ApplicationContext) rootWebApplicationContext;
		if (appContext == null) {
			// uh oh! looks like the lifecycle listener wasn't installed. Let's
			// inform the user
			String message = "********** FATAL ERROR STARTING UP STRUTS-SPRING INTEGRATION **********\n"
					+ "Looks like the Spring listener was not configured for your web app! \n"
					+ "Nothing will work until WebApplicationContextUtils returns a valid ApplicationContext.\n"
					+ "You might need to add the following to web.xml: \n"
					+ "    <listener>\n"
					+ "        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>\n"
					+ "    </listener>";
			return;
		}

		String watchList = container.getInstance(String.class,
				"struts.class.reloading.watchList");
		String acceptClasses = container.getInstance(String.class,
				"struts.class.reloading.acceptClasses");
		String reloadConfig = container.getInstance(String.class,
				"struts.class.reloading.reloadConfig");

		this.setApplicationContext(appContext);

		int type = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME; // default
		if ("name".equals(autoWire)) {
			type = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
		} else if ("type".equals(autoWire)) {
			type = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;
		} else if ("auto".equals(autoWire)) {
			type = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;
		} else if ("constructor".equals(autoWire)) {
			type = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;
		} else if ("no".equals(autoWire)) {
			type = AutowireCapableBeanFactory.AUTOWIRE_NO;
		}
		this.setAutowireStrategy(type);

		this.setUseClassCache(useClassCache);

		this.setAlwaysRespectAutowireStrategy("true"
				.equalsIgnoreCase(alwaysAutoWire));

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1210165505337912361L;

	@Override
	public Result buildResult(ResultConfig resultConfig,
			Map<String, Object> extraContext) throws Exception {
		String resultClassName = resultConfig.getClassName();
		Result result = null;

		if (resultClassName != null) {
			result = (Result) buildBean(resultClassName, extraContext);
			Map<String, String> params = resultConfig.getParams();
			if (params != null) {
				for (Map.Entry<String, String> paramEntry : params.entrySet()) {
					try {
						reflectionProvider.setProperty(paramEntry.getKey(),
								paramEntry.getValue(), result, extraContext,
								true);
					} catch (ReflectionException ex) {
						if (result instanceof ReflectionExceptionHandler) {
							((ReflectionExceptionHandler) result).handle(ex);
						}
					}
				}
			}
		}

		return result;
	}

	public void init() {

	}
}
