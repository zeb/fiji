package ij3d;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.SceneGraphObject;

public final class J3DUtils {

	private J3DUtils() {
		// prevent instantiation of utility class
	}

	private static final Map<SceneGraphObject, String> SGO_NAMES =
		new WeakHashMap<SceneGraphObject, String>();
	private static final Map<RenderingAttributes, Integer> RA_DTFS =
		new WeakHashMap<RenderingAttributes, Integer>();

	private static final Method SGO_GET_NAME;
	private static final Method SGO_SET_NAME;
	private static final Method RA_SET_DTF;

	static {
		Method sgoGetName = null;
		try {
			sgoGetName = SceneGraphObject.class.getDeclaredMethod("getName",
				new Class[0]);
		}
		catch (SecurityException e) {
		}
		catch (NoSuchMethodException e) {
		}
		SGO_GET_NAME = sgoGetName;

		Method sgoSetName = null;
		try {
			sgoSetName = SceneGraphObject.class.getDeclaredMethod("setName",
				new Class[] {String.class});
		}
		catch (SecurityException e) {
		}
		catch (NoSuchMethodException e) {
		}
		SGO_SET_NAME = sgoSetName;

		Method raSetDTF = null;
		try {
			raSetDTF =
				RenderingAttributes.class.getDeclaredMethod("setDepthTestFunction",
				new Class[] {int.class});
		}
		catch (SecurityException e) {
		}
		catch (NoSuchMethodException e) {
		}
		RA_SET_DTF = raSetDTF;
	}

	/**
	 * Gets the name of the given {@link SceneGraphObject}.
	 *
	 * This method exists to avoid a compile-time
	 * dependency on Java3D 1.4+.
	 */
	public static String getName(SceneGraphObject obj) {
		if (SGO_GET_NAME != null) {
			try {
				return (String) SGO_GET_NAME.invoke(new Object[] {obj});
			}
			catch (IllegalAccessException exc) {
			}
			catch (InvocationTargetException exc) {
			}
		}
		else {
			// no SceneGraphObject.getName method; retrieve name from Map instead
			return (String) SGO_NAMES.get(obj);
		}
		return null;
	}

	/**
	 * Sets the name of the given {@link SceneGraphObject}.
	 *
	 * This method exists to avoid a compile-time
	 * dependency on Java3D 1.4+.
	 */
	public static void setName(SceneGraphObject obj, String name) {
		if (SGO_SET_NAME != null) {
			try {
				SGO_SET_NAME.invoke(new Object[] {obj, name});
			}
			catch (IllegalAccessException exc) {
			}
			catch (InvocationTargetException exc) {
			}
		}
		else {
			// no SceneGraphObject.setName method; save name to Map instead
			SGO_NAMES.put(obj, name);
		}
	}

	/**
	 * Sets the depth test function of the given {@link RenderinAttributes}.
	 *
	 * This method exists to avoid a compile-time
	 * dependency on Java3D 1.4+.
	 */
	public static void setDepthTestFunction(RenderingAttributes rendAttr,
		int function)
	{
		if (RA_SET_DTF != null) {
			try {
				RA_SET_DTF.invoke(new Object[] {rendAttr, function});
			}
			catch (IllegalAccessException exc) {
			}
			catch (InvocationTargetException exc) {
			}
		}
		else {
			// no SceneGraphObject.setName method; save name to Map instead
			RA_DTFS.put(rendAttr, function);
		}
	}

}
