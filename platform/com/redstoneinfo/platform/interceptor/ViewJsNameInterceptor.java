package com.redstoneinfo.platform.interceptor;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;

public class ViewJsNameInterceptor extends MethodFilterInterceptor {

	private static final long serialVersionUID = -1536834978609237399L;

	@Override
	protected String doIntercept(ActionInvocation invocation) throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		String servletPath = request.getServletPath();
		String jsPath = servletPath.substring(0, servletPath.indexOf(".")) + ".js";
		request.setAttribute("jsPath", jsPath);
		String result = invocation.invoke();
		return result;
	}

}
