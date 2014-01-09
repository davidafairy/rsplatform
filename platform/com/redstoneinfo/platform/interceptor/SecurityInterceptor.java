	package com.redstoneinfo.platform.interceptor;

import java.io.InputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;
import com.redstoneinfo.bh.entity.User;

public class SecurityInterceptor extends MethodFilterInterceptor {

	private static final long serialVersionUID = 5152787736479031150L;

	@Override
	protected String doIntercept(ActionInvocation invocation) throws Exception {
		Properties properties = new Properties();
		try {
			InputStream is = ServletActionContext.getServletContext()
					.getResourceAsStream("/WEB-INF/config/config.properties");
			properties.load(is);
		}catch (Exception e) {
			throw new RuntimeException("Failed to get properties!");
		}
		String showPageInfo=properties.getProperty("showPageInfo");
		/*如果是0  代表展现的是前台页面，这时跳出错误页面*/
		if("portal".equals(showPageInfo))
		{
			return "errorPage";
		}
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpSession session = request.getSession();
		String servletPath = request.getServletPath();
		if (servletPath.equals("/manager/index.action")
				|| servletPath.equals("/manager/login.action")) {
			String result = invocation.invoke();
			return result;
		} else {
			User currentUser = (User) session.getAttribute("currentUser");
			if (currentUser != null) {
				String result = invocation.invoke();
				return result;
			} else {
				return "relogin";
			}
		}
	}

}
