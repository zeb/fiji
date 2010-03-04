package javax.servlet.http;

import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

public interface HttpServletResponse extends ServletResponse {
	final int SC_UNAUTHORIZED = 0;

	ServletOutputStream getOutputStream();
	void setContentType(String type);
	void setHeader(String key, String value);
	void addHeader(String key, String value);
	void setStatus(int value);
	PrintWriter getWriter();
	void flushBuffer();
	void sendRedirect(String to);
	void setContentLength(int length);
}
