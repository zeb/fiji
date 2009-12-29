package fiji.plugin.constrainedshapes;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.plugin.PlugIn;

public class Snap_Circle implements PlugIn {

	/*
	 * RUN METHOD
	 */
	public void run(String arg) {
		
		ImagePlus imp = WindowManager.getCurrentImage();
		
		final int width  = imp.getWidth();
		final int height = imp.getHeight();
		final int r = Math.min(width, height) / 10;
		final int x = width/2 - r;
		final int y = height/2 - r;
		
		
		
		
	}

}
