package com.redstoneinfo.bh.interceptor;

import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.DefaultActionInvocation;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;
import com.opensymphony.xwork2.interceptor.PreResultListener;
import com.opensymphony.xwork2.util.location.LocationImpl;
import com.redstoneinfo.bh.utils.Config;
import com.redstoneinfo.bh.utils.HttpReqUtil;

public class PortalInterceptor extends MethodFilterInterceptor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8362191700005731349L;

	@Override
	protected String doIntercept(ActionInvocation invocation) throws Exception {

		invocation.addPreResultListener(new PreResultListener() {

			public void beforeResult(ActionInvocation invocation,
					String resultCode) {

				HttpServletRequest request = ServletActionContext.getRequest();

				Properties config = Config.getInstance().getProperties();
				String pcTemplate = config.getProperty("pc.template"); 
				String moblieTemplate = config.getProperty("mobile.template");
				
				String mrule = HttpReqUtil.getFromMobileRule(request);
				String ver = pcTemplate;
//				String from = request.getParameter("from");
//				if (null == from) {
//					from = (String) request.getSession().getAttribute("from");
//				}
//				if ("app".equalsIgnoreCase(from) || "web".equalsIgnoreCase(from)) {
//					ver = "mobile";
//				} else if (mrule != null && !mrule.equals("iPad")) {
//					// Android也都当作手机端，暂时不考虑pad
//					ver = moblieTemplate;
//				}
				
				if (mrule != null && !mrule.equals("iPad")) {
//					// Android也都当作手机端，暂时不考虑pad
					ver = moblieTemplate;
				}
				DefaultActionInvocation defaultActionInvocation = (DefaultActionInvocation) invocation;
				Map<String, ResultConfig> resultMap = defaultActionInvocation
						.getProxy().getConfig().getResults();

				for (Map.Entry<String, ResultConfig> resultentry : resultMap
						.entrySet()) {
					ResultConfig finalResultConfig = resultentry.getValue();
					finalResultConfig.setLocation(new LocationImpl(ver, ""));
				}

			}
		});
		String resultStr = invocation.invoke();

		return resultStr;
	}

}
