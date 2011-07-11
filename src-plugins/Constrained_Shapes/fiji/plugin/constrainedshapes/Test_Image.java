package fiji.plugin.constrainedshapes;

import ij.IJ;

import ij.plugin.PlugIn;

public class Test_Image implements PlugIn {
	public void run(String arg) {
		IJ.run("Blobs (25K)");
		IJ.run("Find Edges");
		IJ.run("Gaussian Blur...", "radius=2");
		IJ.run("Grays");
		IJ.run("Invert");
	}
}