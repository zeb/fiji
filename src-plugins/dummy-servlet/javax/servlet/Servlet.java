package javax.servlet;

public interface Servlet {
	void destroy();
	ServletConfig getServletConfig();
	String getServletInfo();
	void init(ServletConfig config) throws ServletException;
	void service(ServletRequest req, ServletResponse res);
}
