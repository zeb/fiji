package fiji.plugin.constrainedshapes;

import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class Two_Circle_Fitter implements PlugIn {

	/*
	 * INNER CLASSES
	 */



	public void run(String arg) {

		final ImagePlus imp = WindowManager.getCurrentImage();
		final int width = imp.getWidth();
		final int height = imp.getHeight();
		final double radius = Math.min(width, height)/4;
		TwoCircleShape tcs = new TwoCircleShape(width/2.0-0.8*radius, height/2.0, radius, width/2.0+0.8*radius, height/2.0, radius);
		TwoCircleRoi roi = new TwoCircleRoi(tcs);
		imp.setRoi(roi);

		displayICWindow(imp);



		//		ImageProcessor ip = imp.getProcessor();
		//		Sampling2DShapeFitter fitter = new Sampling2DShapeFitter(tcs, ip);
		//		fitter.setMethod(Sampling2DShapeFitter.Method.ORTHOGONAL_SEARCH);
		//		fitter.setFunction(Sampling2DShape.EvalFunction.MEAN);
		//		TwoCircleShape result = (TwoCircleShape) fitter.optimize();
		//		TwoCircleRoi roi = new TwoCircleRoi(result);
		//		Roi.setColor(Color.BLUE);
		//		imp.setRoi(roi);
	}


	private void displayICWindow(final ImagePlus imp) {
		new TCSDialog(null).setVisible(true);
	}
}
