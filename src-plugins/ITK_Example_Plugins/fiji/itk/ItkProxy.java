//
// ItkProxy.java
//

/*
Fiji Java reflection wrapper for ITK.
Copyright (c) 2010, UW-Madison LOCI.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the UW-Madison LOCI nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY UW-MADISON LOCI ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL UW-MADISON LOCI BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package fiji.itk;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import fiji.itk.util.ItkTools;

public abstract class ItkProxy {
	
	// -- Constants --
	
	public static final String TYPE_UINT8 = "UC";
	public static final String TYPE_UINT16 = "US";
	public static final String TYPE_UINT64 = "UL";
	public static final String TYPE_FLOAT = "F";
	public static final String TYPE_DOUBLE = "D";
	//public static final String TYPE_COMPLEX_FLOAT = "CF";

	static {
		try {
			ItkTools.initITK();
		}
		catch (ItkException e) { }
	}
	
	// -- Fields --
	
	protected Object delegate;
	protected String type, outType;
	protected int dim, outDim;
	
	// -- Constructors --
	
	protected ItkProxy(Object delegate, int dim) throws ItkException {
		this(delegate, null, dim);
	}
	
	protected ItkProxy(Object delegate, String type, int dim)
		throws ItkException
	{
		this(delegate, type, dim, null, 0);
	}
	
	protected ItkProxy(Object delegate, String type, int dim,
		String outType, int outDim) throws ItkException
	{
		this.delegate = delegate;
		this.type = type;
		this.dim = dim;
		this.outType = outType;
		this.outDim = outDim;
	}
	
	// -- ItkProxy methods --
	
	/** Gets the actual ITK delegate object. */
	public Object var() {
		return delegate;
	}
	
	/** Gets the pixel type of this ITK object. */
	public String getType() {
		return type;
	}
	
	/** Gets the dimension of this ITK object. */
	public int getDimension() {
		return dim;
	}
	
	// -- Internal API methods --
	
	/**
	 * Invokes the given method with the specified
	 * arguments on the delegate ITK object.
	 */
	protected Object invoke(String method, Object... args) throws ItkException {
		unwrapArgs(args);
		Method[] methods = delegate.getClass().getMethods();
		for (Method m : methods) {
			Class<?>[] c = m.getParameterTypes();
			if (m.getName().equals(method) && c.length == args.length) {
				// NB: Probably the method we are looking for. :-)
				try {
					return m.invoke(delegate, args);
				}
				catch (IllegalArgumentException e) {
					throw new ItkException(e);
				}
				catch (IllegalAccessException e) {
					throw new ItkException(e);
				}
				catch (InvocationTargetException e) {
					throw new ItkException(e);
				}
			}
		}
		throw new ItkException("No such method: " + method);
	}

	/** Wraps the given ITK object with the appropriate proxy. */
	@SuppressWarnings("unchecked")
	protected <T extends ItkProxy> T wrap(Object obj,
		Class<T> returnType, Object... args) throws ItkException
	{
		Constructor<T>[] con = returnType.getConstructors();
		for (Constructor<T> c : con) {
			Class<?>[] types = c.getParameterTypes();
			if (types.length == args.length + 1 && types[0] == Object.class) {
				// NB: Probably the constructor we are looking for. :-)
				try {
					Object[] args1 = new Object[args.length + 1];
					args1[0] = obj;
					System.arraycopy(args, 0, args1, 1, args.length);
					return c.newInstance(args1);
				}
				catch (IllegalArgumentException e) {
					throw new ItkException(e);
				}
				catch (InstantiationException e) {
					throw new ItkException(e);
				}
				catch (IllegalAccessException e) {
					throw new ItkException(e);
				}
				catch (InvocationTargetException e) {
					throw new ItkException(e);
				}
			}
		}
		throw new ItkException("No appropriate constructor for " +
			returnType.getName());
	}
	
	// -- Helper methods --
	
	protected static Object newDelegate(String classBase, int dim)
		throws ItkException
	{
		return newDelegate(classBase, null, dim);
	}
	
	protected static Object newDelegate(String classBase, String type, int dim)
		throws ItkException
	{
		return newDelegate(classBase, type, dim, null, 0, false);
	}
	
	protected static Object newDelegate(String classBase, String type, int dim,
		String outType, int outDim, boolean addI) throws ItkException
	{
		StringBuilder className = new StringBuilder();
		className.append(classBase);
		
		if (addI) className.append("I");
		if (type != null) className.append(type);
		if (dim != 0) className.append(dim);
		if (addI) className.append("I");
		if (outType != null) className.append(outType);
		if (outDim != 0) className.append(outDim);

		return create(className.toString());
	}
	
	protected static Number convertType(String type, float value)
		throws ItkException
	{
		if (type.equals(TYPE_UINT8)) {
			return (short) value;
		}
		else if (type.equals(TYPE_UINT16)) {
			return (int) value;
		}
		else if (type.equals(TYPE_UINT64)) {
			return (long) value;
		}
		else if (type.equals(TYPE_FLOAT)) {
			return value;
		}
		else if (type.equals(TYPE_DOUBLE)) {
			return (double) value;
		}
		else throw new ItkException("Unsupported type conversion: " + type);
	}
	
	private static Object create(String className) throws ItkException {
		try {
			Class<?> c = Class.forName(className);
			return c.newInstance();
		}
		catch (ClassNotFoundException e) {
			throw new ItkException(e);
		}
		catch (InstantiationException e) {
			throw new ItkException(e);
		}
		catch (IllegalAccessException e) {
			throw new ItkException(e);
		}
	}
	
	/** Unwraps ITK proxies to their delegate objects. */
	private static void unwrapArgs(Object... args) {
		for (int i=0; i<args.length; i++) {
			Object arg = args[i];
			if (arg instanceof ItkProxy) {
				args[i] = ((ItkProxy) arg).var();
			}
		}
	}
	
}
