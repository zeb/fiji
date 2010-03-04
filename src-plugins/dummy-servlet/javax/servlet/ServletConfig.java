package javax.servlet;

import java.util.Enumeration;

public interface ServletConfig {
	Enumeration getInitParameterNames();
	String getInitParameter(String key);
}
