package com.redstoneinfo.bh.interceptor;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;
import com.redstoneinfo.bh.annotation.PortalSecurity;
import com.redstoneinfo.bh.entity.PortalUser;
import com.redstoneinfo.bh.manager.LogManager;
import com.redstoneinfo.bh.manager.PortalLoginManager;
import com.redstoneinfo.bh.manager.SubsManager;

public class PortalSecurityInterceptor extends MethodFilterInterceptor {

	private static final long serialVersionUID = -5631745269516181148L;
	private Log log = LogFactory.getLog(getClass());
	@Override
	protected String doIntercept(ActionInvocation invocation) throws Exception {
		/*判断本套系统展现什么页面*/
		Properties properties = new Properties();
		try {
			InputStream is = ServletActionContext.getServletContext()
					.getResourceAsStream("/WEB-INF/config/config.properties");
			properties.load(is);
		}catch (Exception e) {
			throw new RuntimeException("Failed to get properties!");
		}
		String showPageInfo=properties.getProperty("showPageInfo");
		/*如果是1  代表展现的是后台页面，这时跳出错误页面*/
		if("manager".equals(showPageInfo))
		{
			return "errorPage";
		}
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response=ServletActionContext.getResponse();
		HttpSession session = request.getSession();
		String servletPath = request.getServletPath();
		String actionName = servletPath.substring(
				servletPath.lastIndexOf("/") + 1, servletPath.indexOf("."));
		/*如果要退出  直接让退出  不做任何处理*/
		if("logout".equalsIgnoreCase(actionName))
		{
			/*清cookies*/
			resetCookies(request,response);
			return invocation.invoke();
		}
		Class actionClazz = invocation.getAction().getClass();
		Method actionMethod = actionClazz.getMethod(actionName);
		PortalSecurity portalSecurity = actionMethod
				.getAnnotation(PortalSecurity.class);
		String from = request.getParameter("from");
		if (null == from) {
			from = (String) session.getAttribute("from");
		}
		if (null != portalSecurity
				|| (null != from && servletPath.contains("index.action"))|| (null != from && servletPath.contains("wantBuyForHeavyIndustryRe.action"))) {

			if ("app".equalsIgnoreCase(from) || "web".equalsIgnoreCase(from)) {
				session.setAttribute("from", from);
				PortalUser portalUser = (PortalUser) session
						.getAttribute("portalUser");
				if (null == portalUser) {
					String loginName = request.getParameter("loginName");
					String password = request.getParameter("password");
					String platformId = request.getParameter("platform");
					doLoginForApp(loginName,password,platformId);
				}

			}

			PortalUser portalUser = (PortalUser) session
					.getAttribute("portalUser");
			if (null == portalUser) {
				return cookiesForLogin(request,response);
			}
		}else
		{
			 cookiesForLogin(request,response);
		}
		String result = invocation.invoke();
		return result;
	}
	public void resetCookies(HttpServletRequest request,HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		for(int i=0;i<cookies.length;i++)
		{
			/*首先得出所有的cookies  查询是否有username和platId和password*/
			if("username".equalsIgnoreCase(cookies[i].getName()))
			{
				cookies[i].setMaxAge(0);
				response.addCookie(cookies[i]);
			}
			if("pwd".equalsIgnoreCase(cookies[i].getName()))
			{
				cookies[i].setMaxAge(0);
				response.addCookie(cookies[i]);
			}
			if("platId".equalsIgnoreCase(cookies[i].getName()))
			{
				cookies[i].setMaxAge(0);
				response.addCookie(cookies[i]);
			}
		}
	}
	/*做记住密码使用*/
	public  String  cookiesForLogin(HttpServletRequest request,HttpServletResponse response) {
		String username="";
		String pwd="";
		String platId="";
		Cookie[] cookies = request.getCookies();
		if(null!=cookies&&cookies.length>0)
		{
			for(int i=0;i<cookies.length;i++)
			{
				/*首先得出所有的cookies  查询是否有username和platId和password*/
				if("username".equalsIgnoreCase(cookies[i].getName())&&cookies[i].getMaxAge()!=0)
				{
					username=cookies[i].getValue();
				}
				if("pwd".equalsIgnoreCase(cookies[i].getName())&&cookies[i].getMaxAge()!=0)
				{
					pwd=cookies[i].getValue();
				}
				if("platId".equalsIgnoreCase(cookies[i].getName())&&cookies[i].getMaxAge()!=0)
				{
					platId=cookies[i].getValue();
				}
			}
		}
		/*如果这里面任何一个没有，直接登录*/
		if((!StringUtils.isNotBlank(username))&&(!StringUtils.isNotBlank(pwd))&&(!(StringUtils.isNotBlank(platId))))
				{
					return "toLogin";
				}
		/*如果三个值都存在，拿值去登录*/
		else
		{
			try {
				doLoginForApp(username,pwd,platId);
			} catch (Exception e) {
				log.info("记住密码时出错！！");
				e.printStackTrace();
			}
		}
		return null;
	}

	public void doLoginForApp(String loginName,String password,String platformId) throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpSession session = request.getSession();
		ApplicationContext ac1 = WebApplicationContextUtils
				.getWebApplicationContext(request.getSession()
						.getServletContext());
		PortalLoginManager portalLoginManager = (PortalLoginManager) ac1
				.getBean("portalLoginManager");
		
		SubsManager subsManager = (SubsManager) ac1
		.getBean("subsManager");
		
		LogManager logManager = (LogManager) ac1.getBean("logManager");

		JSONObject json = portalLoginManager.getPlatform();
		
		
		JSONObject platJson = portalLoginManager.getPlatform();
		
		if (StringUtils.isNotBlank(loginName)
				&& StringUtils.isNotBlank(password)
				&& StringUtils.isNotBlank(platformId)) {
			JSONObject customer = portalLoginManager.getCustomer(loginName,
					password, platformId);
			String msgLogin = (String) customer.get("msg");
			if (msgLogin.equals("success")) {
				PortalUser portalUser = new PortalUser();
				portalUser.setUsername(loginName);
				portalUser.setPwd(password);
				portalUser.setPlatId(platformId);
				portalUser.setPlatform(platJson.getString(platformId));
				String isBusinessCustomer=(String)customer.getString("isBusinessCustomer");
				session.setAttribute("isBusinessCustomer", isBusinessCustomer);
				if("0".equals(isBusinessCustomer)){//是计费客户
					session.setAttribute("otherCusId", (Integer) customer
							.get("customerId"));
					/*portalUser.setCustomerId((Integer) customer
							.get("customerId"));*/
					portalUser.setAmount((String) customer.get("amount"));
				}
				/*业务平台有可能不传customerId*/
				if(null != customer.get("carIds"))//其他平台客户
				{
					portalUser.setCustomerId(0);
				}else //云通途，微重工或者其他平台计费客户
				{
					portalUser.setCustomerId((Integer) customer.get("customerId"));
				}
				/*业务平台有可能不传customerName*/
				/*if(null != customer.get("carIds")){ //其他平台
*/					String name=(String)customer.getString("name");
					if(StringUtils.isBlank(name)){
						portalUser.setCustomerName(loginName);
					}else{
						portalUser.setCustomerName(name);
					}
				/*}else{//云通途和微重工平台
					if(null==customer.get("customerName"))
					{
						portalUser.setCustomerName(customer.getString("userName"));
					}else
					{
						portalUser.setCustomerName(customer.getString("customerName"));
					}
				}*/
				
				if(null!=customer.get("amount"))
				{
					Object amountObj = customer.get("amount");
					String amountStr = "0";
					if (null != amountObj) {
						amountStr = amountObj.toString();
					}
					portalUser.setAmount(amountStr);
				} else {
					portalUser.setAmount("0");
				}
				if(null != customer.get("carIds")) {
					portalUser.setCarIds(customer.getString("carIds"));
					subsManager.getAllSubs(session.getId());
					/*插入车辆数据*/
				}
				session.setAttribute("portalUser", portalUser);
				/*logManager.insertLog(portalUser, "登录",1);*/
			} else {
				session.setAttribute("portalUser", null);
				session.setAttribute("from", null);
			}
		}

	}

}
