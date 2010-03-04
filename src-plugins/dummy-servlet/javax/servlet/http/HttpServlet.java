package javax.servlet.http;

import java.util.Enumeration;

import javax.servlet.GenericServlet;

public class HttpServlet extends GenericServlet {
	public Enumeration getInitParameterNames() { return null; }
	public String getInitParameter(String name) { return null; }
}
