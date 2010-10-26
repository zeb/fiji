
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

import ij.process.ImageProcessor;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;

import mpicbg.imglib.type.numeric.RealType;

public class MSER_<T extends RealType<T>> implements PlugIn {

	// the image to process
	private Image<T>  image;
	private Image<T>  regions;
	private ImagePlus imp;
	private ImagePlus reg;

	private int[] dimensions;

	private MSER<T>   mser;

	public void run(String args) {

		IJ.log("Starting plugin MSER");

		// read image
		imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Please open an image first.");
			return;
		}

		int delta = 10;
		int minArea = 10;
		int maxArea = 1000000;
		double maxVariation = 10.0;
		double minDiversity = 0.5;
		final boolean darkToBright;
		final boolean brightToDark;

		// ask for parameters
		GenericDialog gd = new GenericDialog("Settings");
		gd.addNumericField("delta:", delta, 0);
		gd.addNumericField("min area:", minArea, 0);
		gd.addNumericField("max area:", maxArea, 0);
		gd.addNumericField("max variation:", maxVariation, 2);
		gd.addNumericField("min diversity:", minDiversity, 2);
		gd.addCheckbox("dark to bright", true);
		gd.addCheckbox("bright to dark", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
	
		delta = (int)gd.getNextNumber();
		minArea = (int)gd.getNextNumber();
		maxArea = (int)gd.getNextNumber();
		maxVariation = gd.getNextNumber();
		minDiversity = gd.getNextNumber();
		darkToBright = gd.getNextBoolean();
		brightToDark = gd.getNextBoolean();

		image      = ImagePlusAdapter.wrap(imp);
		dimensions = image.getDimensions();
		int width  = dimensions[0];
		int height = dimensions[1];
		int slices = 1;
		if (dimensions.length > 2)
			slices = dimensions[2];
	
		// prepare segmentation image
		reg = imp.createImagePlus();
		ImageStack stack = new ImageStack(width, height);
		for (int s = 1; s <= slices; s++) {
			ImageProcessor duplProcessor = imp.getStack().getProcessor(s).duplicate();
			stack.addSlice("", duplProcessor);
		}
		reg.setStack(stack);
		reg.setDimensions(1, slices, 1);
		if (slices > 1)
			reg.setOpenAsHyperStack(true);
	
		reg.setTitle("msers of " + imp.getTitle());
	
		regions = ImagePlusAdapter.wrap(reg);
		LocalizableByDimCursor<T> cursor = regions.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getType().setReal(0.0);
		}
	
		// set up algorithm
		mser = new MSER<T>(image, delta, minArea, maxArea, maxVariation, minDiversity);
	
		Thread processThread = new Thread(new Runnable() {
			public void run() {
				mser.process(regions, darkToBright, brightToDark);
				// change the LUT for the segmentation image
				IJ.run(reg, "glasbey", "");
				reg.show();
				reg.updateAndDraw();
			}
		});
		processThread.start();

		// wait for the thread to finish
		try {
			processThread.join();
		} catch (InterruptedException e) {
			processThread.interrupt();
		}
	}
}
