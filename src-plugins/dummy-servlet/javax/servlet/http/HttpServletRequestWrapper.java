package javax.servlet.http;

public class HttpServletRequestWrapper implements HttpServletRequest {
	public HttpServletRequestWrapper(HttpServletRequest request) {}
	public String getParameter(String key) { return null; }
	public HttpSession getSession() { return null; }
	public HttpSession getSession(boolean flag) { return null; }
	public String getPathInfo() { return null; }
	public String getHeader(String key) { return null; }
	public boolean isSecure() { return false; }
	public StringBuffer getRequestURL() { return null; }
	public String getQueryString() { return null; }
}
