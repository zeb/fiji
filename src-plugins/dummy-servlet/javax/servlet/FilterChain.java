package javax.servlet;

public interface FilterChain {
	public void doFilter(ServletRequest request, ServletResponse response);
}
