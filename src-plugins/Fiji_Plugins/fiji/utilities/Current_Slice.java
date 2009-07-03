package fiji.utilities;

import ij.IJ;
import ij.ImagePlus;

import ij.plugin.PlugIn;

public class Current_Slice extends CurrentSlice implements PlugIn {
	CurrentSlice current;

	public void run(String arg) { }

	public void obsolete() {
		IJ.write("obsolete: " + image + ", " + ip + "(" + slice + ")");
	}

	public void changed() {
		IJ.write("changed: " + image + ", " + ip + "(" + slice + ")");
	}
}
