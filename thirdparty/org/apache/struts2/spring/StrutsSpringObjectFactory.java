package org.apache.struts2.spring;

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.lang.xwork.StringUtils;
import org.apache.struts2.StrutsConstants;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.spring.SpringObjectFactory;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;
import com.opensymphony.xwork2.util.reflection.ReflectionException;
import com.opensymphony.xwork2.util.reflection.ReflectionExceptionHandler;

public class StrutsSpringObjectFactory extends SpringObjectFactory {
	private static final Logger LOG = LoggerFactory
			.getLogger(StrutsSpringObjectFactory.class);

	// @Inject
	// public StrutsSpringObjectFactory(
	// @Inject(value=StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_AUTOWIRE,required=false)
	// String autoWire,
	// @Inject(value=StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_USE_CLASS_CACHE,required=false)
	// String useClassCacheStr,
	// @Inject ServletContext servletContext) {
	// this(autoWire, "false", useClassCacheStr, servletContext);
	// }

	/**
	 * Constructs the spring object factory
	 * 
	 * @param autoWire
	 *            The type of autowiring to use
	 * @param alwaysAutoWire
	 *            Whether to always respect the autowiring or not
	 * @param useClassCacheStr
	 *            Whether to use the class cache or not
	 * @param servletContext
	 *            The servlet context
	 * @since 2.1.3
	 */
	@Inject
	public StrutsSpringObjectFactory(
			@Inject(value = StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_AUTOWIRE, required = false) String autoWire,
			@Inject(value = StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_AUTOWIRE_ALWAYS_RESPECT, required = false) String alwaysAutoWire,
			@Inject(value = StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_USE_CLASS_CACHE, required = false) String useClassCacheStr,
			@Inject ServletContext servletContext,
			@Inject(StrutsConstants.STRUTS_DEVMODE) String devMode,
			@Inject Container container) {
		super();
		boolean useClassCache = "true".equals(useClassCacheStr);
		LOG.info("Initializing Struts-Spring integration...");
		Object rootWebApplicationContext = servletContext
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (rootWebApplicationContext instanceof RuntimeException) {
			RuntimeException runtimeException = (RuntimeException) rootWebApplicationContext;
			LOG.fatal(runtimeException.getMessage());
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
			LOG.fatal(message);
			return;
		}

		String watchList = container.getInstance(String.class,
				"struts.class.reloading.watchList");
		String acceptClasses = container.getInstance(String.class,
				"struts.class.reloading.acceptClasses");
		String reloadConfig = container.getInstance(String.class,
				"struts.class.reloading.reloadConfig");
		if ("true".equals(devMode) && StringUtils.isNotBlank(watchList)
				&& appContext instanceof ClassReloadingXMLWebApplicationContext) {
			// prevent class caching
			useClassCache = false;

			ClassReloadingXMLWebApplicationContext reloadingContext = (ClassReloadingXMLWebApplicationContext) appContext;
			reloadingContext.setupReloading(watchList.split(","),
					acceptClasses, servletContext, "true".equals(reloadConfig));
			LOG.info(
					"Class reloading is enabled. Make sure this is not used on a production environment!",
					watchList);

			setClassLoader(reloadingContext.getReloadingClassLoader());

			// we need to reload the context, so our isntance of the factory is
			// picked up
			reloadingContext.refresh();
		}

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

		LOG.info("... initialized Struts-Spring integration successfully");
	}

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
						String jspPath = paramEntry.getValue();
						if (resultConfig.getLocation() != null) {
							String desc = resultConfig.getLocation()
									.getDescription();
							jspPath = StringUtils.replace(jspPath, "${var}",
									desc);
						}

						reflectionProvider.setProperty(paramEntry.getKey(),
								jspPath, result, extraContext, true);
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
}
