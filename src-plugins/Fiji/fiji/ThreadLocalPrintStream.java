package fiji;

import java.io.OutputStream;
import java.io.PrintStream;

import java.lang.reflect.Method;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public class ThreadLocalPrintStream extends PrintStream {
	private static ThreadLocalPrintStream errInstance, outInstance;

	public static final void install() {
		if (errInstance == System.err)
			return;
		errInstance = new ThreadLocalPrintStream(System.err);
		System.setErr(errInstance);
		outInstance = new ThreadLocalPrintStream(System.out);
		System.setOut(outInstance);
	}

	public static void setErr(Thread thread, PrintStream printStream) {
		install();
		set(errInstance, thread, printStream);
	}

	public static void setOut(Thread thread, PrintStream printStream) {
		install();
		set(outInstance, thread, printStream);
	}

	public static void setErr(PrintStream printStream) {
		setErr(Thread.currentThread(), printStream);
	}

	public static void setOut(PrintStream printStream) {
		setOut(Thread.currentThread(), printStream);
	}

	public static void setErrAndOut(PrintStream err, PrintStream out) {
		Thread thread = Thread.currentThread();
		setErr(err);
		setOut(out);
	}

	public static void setErrAndOut(OutputStream out) {
		Thread thread = Thread.currentThread();
		PrintStream printStream = new PrintStream(out);
		setErr(printStream);
		setOut(printStream);
	}

	public static void set(ThreadLocalPrintStream instance, Thread thread, PrintStream printStream) {
		ThreadGroup group = thread.getThreadGroup();
		if (group == null)
			return;
		instance.map.put(thread, printStream);
		instance.groupMap.put(group, printStream);
	}

	public PrintStream get(Thread thread) {
		PrintStream printStream = map.get(thread);
		if (printStream == null) {
			printStream = get(thread.getThreadGroup());
			map.put(thread, printStream);
		}
		return printStream;
	}

	public PrintStream get(ThreadGroup group) {
		if (group == null)
			return fallBack;
		PrintStream printStream = groupMap.get(group);
		if (printStream != null)
			return printStream;
		printStream = get(group.getParent());
		groupMap.put(group, printStream);
		return printStream;
	}

	protected Map<Thread, PrintStream> map = new WeakHashMap<Thread, PrintStream>();
	protected Map<ThreadGroup, PrintStream> groupMap = new WeakHashMap<ThreadGroup, PrintStream>();
	private PrintStream fallBack;

	public ThreadLocalPrintStream(PrintStream fallBack) {
		super(fallBack);
		this.fallBack = fallBack;
	}

	public void flush() {
		get(Thread.currentThread()).flush();
	}

	public void close() {
		get(Thread.currentThread()).close();
	}

	public boolean checkError() {
		return get(Thread.currentThread()).checkError();
	}

	protected static Method setError;

	protected void setError() {
		try {
			if (setError == null) {
				setError = PrintStream.class.getMethod("setError", new Class[0]);
				setError.setAccessible(true);
			}
			setError.invoke(get(Thread.currentThread()), new Object[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void write(int b) {
		get(Thread.currentThread()).write(b);
	}

	public void write(byte[] buf, int off, int len) {
		get(Thread.currentThread()).write(buf, off, len);
	}

	public void print(boolean b) {
		get(Thread.currentThread()).print(b);
	}

	public void print(char c) {
		get(Thread.currentThread()).print(c);
	}

	public void print(int i) {
		get(Thread.currentThread()).print(i);
	}

	public void print(long l) {
		get(Thread.currentThread()).print(l);
	}

	public void print(float f) {
		get(Thread.currentThread()).print(f);
	}

	public void print(double d) {
		get(Thread.currentThread()).print(d);
	}

	public void print(char[] s) {
		get(Thread.currentThread()).print(s);
	}

	public void print(String s) {
		get(Thread.currentThread()).print(s);
	}

	public void print(Object obj) {
		get(Thread.currentThread()).print(obj);
	}

	public void println() {
		get(Thread.currentThread()).println();
	}

	public void println(boolean x) {
		get(Thread.currentThread()).println(x);
	}

	public void println(char x) {
		get(Thread.currentThread()).println(x);
	}

	public void println(int x) {
		get(Thread.currentThread()).println(x);
	}

	public void println(long x) {
		get(Thread.currentThread()).println(x);
	}

	public void println(float x) {
		get(Thread.currentThread()).println(x);
	}

	public void println(double x) {
		get(Thread.currentThread()).println(x);
	}

	public void println(char[] x) {
		get(Thread.currentThread()).println(x);
	}

	public void println(String x) {
		get(Thread.currentThread()).println(x);
	}

	public void println(Object x) {
		get(Thread.currentThread()).println(x);
	}

	public PrintStream printf(String format, Object[] args) {
		return get(Thread.currentThread()).printf(format, args);
	}

	public PrintStream printf(Locale locale, String format, Object[] args) {
		return get(Thread.currentThread()).printf(locale, format, args);
	}

	public PrintStream format(String format, Object[] args) {
		return get(Thread.currentThread()).format(format, args);
	}

	public PrintStream format(Locale locale, String format, Object[] args) {
		return get(Thread.currentThread()).format(locale, format, args);
	}

	public PrintStream append(CharSequence csq) {
		return get(Thread.currentThread()).append(csq);
	}
	
	public PrintStream append(CharSequence csq, int start, int end) {
		return get(Thread.currentThread()).append(csq, start, end);
	}

	public PrintStream append(char c) {
		return get(Thread.currentThread()).append(c);
	}
}