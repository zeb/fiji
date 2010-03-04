package javax.servlet;

import java.util.Enumeration;

public interface FilterConfig {
	Enumeration getInitParameterNames();
	String getInitParameter(String key);
}
