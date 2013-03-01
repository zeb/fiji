package fiji.plugin.multiviewtracker.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utils {

	/**
	 * @return the project version string as stored in the maven pom.xml file. 
	 * Adapted from http://blog.nigelsim.org/2011/08/31/programmatically-getting-the-maven-version-of-your-project/
	 */
	public static final String getAPIVersion() {
		String path = "/version.prop";
		InputStream stream = Utils.class.getResourceAsStream(path);
		if (stream == null) return "UNKNOWN";
		Properties props = new Properties();
		try {
			props.load(stream);
			stream.close();
			return (String)props.get("version");
		} catch (IOException e) {
			return "UNKNOWN";
		}
	}
	
	
}
