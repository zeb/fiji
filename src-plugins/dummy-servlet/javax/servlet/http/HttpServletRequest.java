package javax.servlet.http;

import javax.servlet.ServletRequest;

public interface HttpServletRequest extends ServletRequest {
	String getParameter(String key);
	HttpSession getSession();
	HttpSession getSession(boolean flag);
	String getPathInfo();
	String getHeader(String key);
	boolean isSecure();
	StringBuffer getRequestURL();
	String getQueryString();
}
