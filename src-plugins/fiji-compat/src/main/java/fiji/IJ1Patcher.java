package fiji;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import imagej.legacy.DefaultLegacyService;

/**
 * Patch ij.jar using Javassist, handle headless mode, too.
 *
 * @author Johannes Schindelin
 */

public class IJ1Patcher implements Runnable {
	@Override
	public void run() {
		try {
			String headless = System.getProperty("java.awt.headless");
			if ("true".equalsIgnoreCase(headless))
				new Headless().run();
			new IJHacker().run();
			try {
				System.err.println("Loaded " + DefaultLegacyService.class);
			} catch (final NoClassDefFoundError e) {
				// ImageJ2 not installed
for (Throwable t = e; t != null; t = t.getCause()) t.printStackTrace();
				System.err.println("Did not find DefaultLegacyService class: " + e);
			}
			try {
				JavassistHelper.defineClasses();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (NoClassDefFoundError e) {
			// Deliberately ignored - in some cases
			// javassist can not be found, and we should
			// continue anyway.
		}
	}
}