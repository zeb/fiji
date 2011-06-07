package fiji.plugin.flowmate;

import fiji.plugin.flowmate.analysis.NormSquareSummer;
import fiji.plugin.flowmate.analysis.PeakDetector;
import fiji.plugin.flowmate.opticflow.LucasKanade;
import fiji.plugin.flowmate.util.OpticFlowUtils;
import fiji.plugin.flowmate.util.Windows;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

public class FlowMate_  implements PlugIn {

	private static final String VERSION_STRING = "alpha-6/6/11";
	private static final String PEAK_NUMBER_COLUMN_NAME = "Peaks per frame";
	
	private static final double  DEFAULT_THRESHOLD = 1;
	private static final boolean DEFAULT_COMPUTE_ACCURACY_IMAGE = false;
	private static final boolean DEFAULT_COMPUTE_CENTRAL_FRAME_ONLY = false;
	private static final boolean DEFAULT_DISPLAY_COLOR_WHEEL = false;
	private static final boolean DEFAULT_DISPLAY_COLOR_FLOW = true;
	private static final boolean DEFAULT_DISPLAY_FLOW_IMAGES = false;
	private static final boolean  DEFAULT_DISPLAY_PEAKS = true;
	
	/*
	 * STATIC FIELDS (to remember between plugin launches). 
	 */
	
	private static double threshold = DEFAULT_THRESHOLD;
	private static boolean displayAccuracyImage = DEFAULT_COMPUTE_ACCURACY_IMAGE;
	private static boolean computeOnlyCentralFrame = DEFAULT_COMPUTE_CENTRAL_FRAME_ONLY;
	private static boolean displayColorWheel = DEFAULT_DISPLAY_COLOR_WHEEL;
	private static boolean displayColorFlowImage = DEFAULT_DISPLAY_COLOR_FLOW;
	private static boolean displayFlowImages = DEFAULT_DISPLAY_FLOW_IMAGES;
	private static boolean displayPeaks = DEFAULT_DISPLAY_PEAKS;
	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void run(String arg) {
		
		if (!launchDialog())
			return;
				
		ImagePlus imp = WindowManager.getCurrentImage();
		
		if (computeOnlyCentralFrame)
			imp = OpticFlowUtils.getCentralSlices(imp, 5);
		
		ImagePlus originalImp = imp;
		Roi roi = imp.getRoi();
		Roi largeRoi = null;
		if (null != roi) {
			imp = new Duplicator().run(imp);
			roi.setLocation(0, 0); // Move it to the top left corner, in the new image.
		}
		
		if (!computeOnlyCentralFrame) {
			IJ.log("__");
			IJ.log("FlowMate for "+imp.getShortTitle());
		}
		
		Image<? extends RealType> img = ImageJFunctions.wrap(imp);

		// To compute correct optic flow, we need to enlarge the area in which we compute the derivatives.
		if (roi != null) 
			largeRoi = OpticFlowUtils.enlargeRoi(roi, 5);
		SimoncelliDerivation<? extends RealType> filter = new SimoncelliDerivation(img, 5, largeRoi);

		ArrayList<Image<FloatType>> derivatives = new  ArrayList<Image<FloatType>>(img.getNumDimensions()); 
		for (int i = 0; i < img.getNumDimensions(); i++) {
			filter.setDerivationDimension(i);
			filter.checkInput();
			filter.process();
			Image<FloatType> output = filter.getResult();
			derivatives.add(output);
		}
		if (!computeOnlyCentralFrame)
			IJ.log("Computation of derivatives done in "+filter.getProcessingTime()/1000+" s.");

		// Optic flow
		LucasKanade opticFlowAlgo = new LucasKanade(derivatives, roi);
		opticFlowAlgo.setThreshold(threshold);
		opticFlowAlgo.setComputeAccuracy(displayAccuracyImage);
		opticFlowAlgo.setWindow(Windows.getFlat5x5Window(), new int[] {5, 5, 1});
		opticFlowAlgo.checkInput();
		opticFlowAlgo.process();
		List<Image<FloatType>> opticFlow = opticFlowAlgo.getResults();
		if (!computeOnlyCentralFrame)
			IJ.log("Computation of optic flow done in "+opticFlowAlgo.getProcessingTime()/1000+" s.");

		// Display outputs

		if (displayColorFlowImage) {
			Image<RGBALegacyType> flow = OpticFlowUtils.createColorFlowImage(opticFlow.get(0), opticFlow.get(1));
			ImagePlus colorFlow = ImageJFunctions.copyToImagePlus(flow);
			if (computeOnlyCentralFrame)
				OpticFlowUtils.cropToCentralSlice(colorFlow);
			colorFlow.show();
		}

		
		if (displayAccuracyImage) {
			Image<FloatType> accuracy = opticFlowAlgo.getAccuracyImage();
			ImagePlus accuracyImage = ImageJFunctions.copyToImagePlus(accuracy);
			if (computeOnlyCentralFrame)
				OpticFlowUtils.cropToCentralSlice(accuracyImage);
			accuracyImage.getProcessor().resetMinAndMax();
			accuracyImage.show();
		}

		if (displayFlowImages) {
			for (Image<FloatType> speedComponent : opticFlow) {
				ImagePlus speedComp = ImageJFunctions.copyToImagePlus(speedComponent);
				if (computeOnlyCentralFrame)
					OpticFlowUtils.cropToCentralSlice(speedComp);
				speedComp.setSlice(speedComp.getStackSize()/2);
				speedComp.getProcessor().resetMinAndMax();
				speedComp.show();
			}
		}

		if (displayColorWheel) {
			Image<RGBALegacyType> indicator = OpticFlowUtils.createIndicatorImage(64);
			ImageJFunctions.copyToImagePlus(indicator).show();
		}
		
		if (computeOnlyCentralFrame)
			return; // No need to go further then

		// Analysis
		NormSquareSummer summer = new NormSquareSummer(opticFlow.get(0), opticFlow.get(1));
		summer.checkInput();
		summer.process();
			IJ.log("Summation of velocity norm done in "+summer.getProcessingTime()/1000+" s.");
		
		float[] normSquare = summer.getSquareNorm();
		int[] count = summer.getCount();
		float[] meanSpeedSquare = new float[normSquare.length];
		float[] time = new float[normSquare.length];
		for (int i = 0; i < meanSpeedSquare.length; i++) { 
			meanSpeedSquare[i] = normSquare[i] / count[i];
			time[i] = i;
		}
		
		PeakDetector detector = new PeakDetector(meanSpeedSquare, 5, 1);
		detector.process();
		int[] peakLocations = detector.getResults();
		int npeaks = peakLocations.length;
		float[] peakTime = new float[peakLocations.length];
		float[] peakVal = new float[peakLocations.length];
		for (int i = 0; i < npeaks; i++) {
			peakTime[i] = time[peakLocations[i]];
			peakVal[i] = meanSpeedSquare[peakLocations[i]];
		}
		IJ.log("Peak detection done in "+detector.getProcessingTime()/1000+" s.");

		if (displayPeaks) {
			Plot plot = new Plot("Mean velocity squared", "Frame", "Velocity squared", time, meanSpeedSquare);
			plot.draw();
			plot.setColor(Color.red);
			plot.addPoints(peakTime, peakVal, 0);
			plot.show();
		}
		
		ResultsTable mainTable = ResultsTable.getResultsTable();
		float npeaksPerFrame = (float) npeaks / img.getDimension(2); // DIRTY
		String rowName = originalImp.getShortTitle();
		if (null != roi) {
			String roiName = roi.getName() == null ? "roi" : roi.getName();
			rowName += "-"+roiName;
		}
		mainTable.incrementCounter();
		mainTable.addLabel(rowName);
		mainTable.addValue(PEAK_NUMBER_COLUMN_NAME, npeaksPerFrame);
		mainTable.show("Results");
		
		IJ.log(String.format("Found a peak frequency of %.2e peaks/frame", npeaksPerFrame));

	}

	private boolean launchDialog() {
		
		GenericDialog dialog = new GenericDialog("FlowMate v. "+VERSION_STRING);
		dialog.addMessage("Set threshold in flow accuracy");
		dialog.addNumericField("Threshold", threshold, 5);
		dialog.addCheckbox("Display flow color image", displayColorFlowImage);
		dialog.addCheckbox("Display peaks analysis", displayPeaks);
		dialog.addCheckbox("Display color wheel", displayColorWheel);
		dialog.addMessage("Display the Vx and Vy flow images");
		dialog.addCheckbox("Display flow images", displayFlowImages);
		dialog.addMessage("Compute and display accuracy image\n(on which threshold will be applied)");
		dialog.addCheckbox("Compute accuracy image", displayAccuracyImage);
		dialog.addMessage("Do calculation only for the central frame\n(useful to quickly determine threshold)");
		dialog.addCheckbox("Compute only central frame", computeOnlyCentralFrame);
		
		dialog.showDialog();
		
		if (dialog.wasCanceled())
			return false;
		
		threshold = dialog.getNextNumber();
		displayColorFlowImage = dialog.getNextBoolean();
		displayPeaks = dialog.getNextBoolean();
		displayColorWheel = dialog.getNextBoolean();		
		displayFlowImages = dialog.getNextBoolean();
		displayAccuracyImage = dialog.getNextBoolean();
		computeOnlyCentralFrame = dialog.getNextBoolean();
		return true;
		
		
	}
	
}
