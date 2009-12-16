package fiji.plugin.constrainedshapes;

import java.awt.Color;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Two_Circle_Fitter implements PlugIn {

	public void run(String arg) {
		final ImagePlus imp = WindowManager.getCurrentImage();
		displayICWindow(imp);
		Sampling2DShape tcs = ( (TwoCircleRoi)imp.getRoi() ).getSampling2DShape();
		ImageProcessor ip = imp.getProcessor();
		Sampling2DShapeFitter fitter = new Sampling2DShapeFitter(tcs, ip);
		fitter.setMethod(Sampling2DShapeFitter.Method.ORTHOGONAL_SEARCH);
		fitter.setFunction(Sampling2DShape.EvalFunction.MEAN);
		TwoCircleShape result = (TwoCircleShape) fitter.optimize();
		TwoCircleRoi roi = new TwoCircleRoi(result);
		Roi.setColor(Color.BLUE);
		imp.setRoi(roi);
	}
	
	
	private void displayICWindow(final ImagePlus imp) {
		final int width = imp.getWidth();
		final int height = imp.getHeight();
		TwoCircleShape tcs = new TwoCircleShape(3/8*width, height/2, width/4, 5/8*width, height/2, width/4);
		TwoCircleRoi roi = new TwoCircleRoi(tcs);
		imp.setRoi(roi);
		
		GenericDialog gd = new GenericDialog("Starting point");
		gd.addTextAreas(
				"Select the first slice, and draw the two circle shape on it." 
				+ "This will be used as starting point for the fit." , null	, 1, 1);
		gd.showDialog();
		if (gd.wasCanceled()) { }
	}

}
