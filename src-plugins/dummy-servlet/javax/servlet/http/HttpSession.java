package javax.servlet.http;

public interface HttpSession {
	Object getAttribute(String key);
	void removeAttribute(String key);
	void setAttribute(String key, Object value);
}
