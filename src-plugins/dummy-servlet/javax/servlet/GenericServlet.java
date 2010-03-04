package javax.servlet;

public class GenericServlet implements Servlet {
	public void destroy() {}
	public ServletConfig getServletConfig() { return null; }
	public String getServletInfo() { return null; }
	public void init(ServletConfig config) throws ServletException {}
	public void service(ServletRequest req, ServletResponse res) {}
}
