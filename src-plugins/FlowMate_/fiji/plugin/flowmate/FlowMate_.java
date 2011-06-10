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

	private static final String VERSION_STRING = "alpha-10/6/11";
	static final String PEAK_NUMBER_COLUMN_NAME = "Peaks per frame";
	
	private static final double  DEFAULT_THRESHOLD = 1;
	private static final boolean DEFAULT_COMPUTE_ACCURACY_IMAGE = false;
	private static final boolean DEFAULT_COMPUTE_CENTRAL_FRAME_ONLY = false;
	private static final boolean DEFAULT_DISPLAY_COLOR_WHEEL = false;
	private static final boolean DEFAULT_DISPLAY_COLOR_FLOW = true;
	private static final boolean DEFAULT_DISPLAY_FLOW_IMAGES = false;
	private static final boolean DEFAULT_DISPLAY_PEAKS = true;

	private double  threshold;
	private boolean computeAccuracyImage;
	private boolean computeOnlyCentralFrame;
	private boolean displayColorWheel;
	private boolean computeColorFlowImage;
	private boolean computeFlowImages;
	private boolean displayPeaks;
	private boolean verbose = true;
	private ArrayList<Image<FloatType>> derivatives;
	private ImagePlus colorFlow;
	private ImagePlus accuracyImage;
	private ArrayList<ImagePlus> velocityField;
	private Plot plot;
	private ArrayList<float[]> meanSquareFlow;
	private ArrayList<float[]> peakLocation;

	
	/*
	 * STATIC FIELDS (to remember between plugin launches). 
	 */
	
	private static double  thresholdSettings = DEFAULT_THRESHOLD;
	private static boolean computeAccuracyImageSettings = DEFAULT_COMPUTE_ACCURACY_IMAGE;
	private static boolean computeOnlyCentralFrameSettings = DEFAULT_COMPUTE_CENTRAL_FRAME_ONLY;
	private static boolean displayColorWheelSettings = DEFAULT_DISPLAY_COLOR_WHEEL;
	private static boolean computeColorFlowImageSettings = DEFAULT_DISPLAY_COLOR_FLOW;
	private static boolean computeFlowImagesSettings = DEFAULT_DISPLAY_FLOW_IMAGES;
	private static boolean displayPeaksSettings = DEFAULT_DISPLAY_PEAKS;
	

	/**
	 *  Copy static configured fields to instance fields
	 */
	public void assignParameters() {
		threshold = thresholdSettings;
		computeAccuracyImage = computeAccuracyImageSettings;
		computeColorFlowImage = computeColorFlowImageSettings;
		displayColorWheel = displayColorWheelSettings;
		computeFlowImages = computeFlowImagesSettings;
		displayPeaks = displayPeaksSettings;
		computeOnlyCentralFrame = computeOnlyCentralFrameSettings;
	}
	
	@Override
	public void run(String arg) {
		
		if (!showDialog())
			return;
		
		assignParameters();
	
		ImagePlus imp = WindowManager.getCurrentImage();
		float npeaksPerFrame = process(imp);
		
		String rowName = imp.getShortTitle();
		Roi roi = imp.getRoi();
		if (null != roi) {
			String roiName = roi.getName() == null ? "Roi" : roi.getName();
			rowName += "-"+roiName;
		}
		ResultsTable mainTable = ResultsTable.getResultsTable();
		mainTable.incrementCounter();
		mainTable.addLabel(rowName);
		mainTable.addValue(PEAK_NUMBER_COLUMN_NAME, npeaksPerFrame);
		mainTable.show("Results");
		
		IJ.log(String.format("Found a peak frequency of %.2e peaks/frame", npeaksPerFrame));

		if (computeColorFlowImage)
			colorFlow.show();
		
		if (computeFlowImages) {
			for (ImagePlus tmp : velocityField)
				tmp.show();
		}
		
		if (displayPeaks) 
			plot.show();

		if (computeAccuracyImage)
			accuracyImage.show();
		
		if (displayColorWheel) {
			Image<RGBALegacyType> indicator = OpticFlowUtils.createIndicatorImage(64);
			ImagePlus colorWheel = ImageJFunctions.copyToImagePlus(indicator);
			colorWheel.show();
		}

	}
		
		
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public float process(final ImagePlus source) {
		
		ImagePlus imp = source;
		if (computeOnlyCentralFrame)
			imp = OpticFlowUtils.getCentralSlices(imp, 5);
		
		ImagePlus originalImp = imp;
		Roi roi = imp.getRoi();
		Roi largeRoi = null;
		if (null != roi) {
			imp = new Duplicator().run(imp);
			roi.setLocation(0, 0); // Move it to the top left corner, in the new image.
		}
		
		String rowName = originalImp.getShortTitle();
		if (null != roi) {
			String roiName = roi.getName() == null ? "roi" : roi.getName();
			rowName += "-"+roiName;
		}
		
		if (!computeOnlyCentralFrame && verbose) {
			IJ.log("__");
			IJ.log("FlowMate for "+rowName);
		}
		
		Image<? extends RealType> img = ImageJFunctions.wrap(imp);

		// To compute correct optic flow, we need to enlarge the area in which we compute the derivatives.
		if (roi != null) 
			largeRoi = OpticFlowUtils.enlargeRoi(roi, 5);
		SimoncelliDerivation<? extends RealType> filter = new SimoncelliDerivation(img, 5, largeRoi);

		derivatives = new  ArrayList<Image<FloatType>>(img.getNumDimensions()); 
		for (int i = 0; i < img.getNumDimensions(); i++) {
			filter.setDerivationDimension(i);
			filter.checkInput();
			filter.process();
			Image<FloatType> output = filter.getResult();
			derivatives.add(output);
		}
		if (!computeOnlyCentralFrame && verbose)
			IJ.log("Computation of derivatives done in "+filter.getProcessingTime()/1000+" s.");

		// Optic flow
		LucasKanade opticFlowAlgo = new LucasKanade(derivatives, roi);
		opticFlowAlgo.setThreshold(threshold);
		opticFlowAlgo.setComputeAccuracy(computeAccuracyImage);
		opticFlowAlgo.setWindow(Windows.getFlat5x5Window(), new int[] {5, 5, 1});
		opticFlowAlgo.checkInput();
		opticFlowAlgo.process();
		List<Image<FloatType>> opticFlow = opticFlowAlgo.getResults();
		if (!computeOnlyCentralFrame && verbose)
			IJ.log("Computation of optic flow done in "+opticFlowAlgo.getProcessingTime()/1000+" s.");

		// Display outputs

		if (computeColorFlowImage) {
			Image<RGBALegacyType> flow = OpticFlowUtils.createColorFlowImage(opticFlow.get(0), opticFlow.get(1));
			colorFlow = ImageJFunctions.copyToImagePlus(flow);
			if (computeOnlyCentralFrame)
				OpticFlowUtils.cropToCentralSlice(colorFlow);
			colorFlow.setTitle(colorFlow.getTitle()+" - "+rowName);
		}

		
		if (computeAccuracyImage) {
			Image<FloatType> accuracy = opticFlowAlgo.getAccuracyImage();
			accuracyImage = ImageJFunctions.copyToImagePlus(accuracy);
			if (computeOnlyCentralFrame)
				OpticFlowUtils.cropToCentralSlice(accuracyImage);
			accuracyImage.getProcessor().resetMinAndMax();
			accuracyImage.setTitle(accuracyImage.getTitle()+" - "+rowName);
		}

		if (computeFlowImages) {
			velocityField = new ArrayList<ImagePlus>(opticFlow.size());
			for (Image<FloatType> speedComponent : opticFlow) {
				ImagePlus speedComp = ImageJFunctions.copyToImagePlus(speedComponent);
				if (computeOnlyCentralFrame)
					OpticFlowUtils.cropToCentralSlice(speedComp);
				speedComp.setSlice(speedComp.getStackSize()/2);
				speedComp.getProcessor().resetMinAndMax();
				speedComp.setTitle(speedComp.getTitle()+" - "+rowName);
				velocityField.add(speedComp);
			}
		}

		// Analysis
		NormSquareSummer summer = new NormSquareSummer(opticFlow.get(0), opticFlow.get(1));
		summer.checkInput();
		summer.process();
		if (verbose)
			IJ.log("Summation of velocity norm done in "+summer.getProcessingTime()/1000+" s.");
		
		float[] normSquare = summer.getSquareNorm();
		int[] count = summer.getCount();
		float[] meanSpeedSquare = new float[normSquare.length];
		float[] time = new float[normSquare.length];
		for (int i = 0; i < meanSpeedSquare.length; i++) { 
			meanSpeedSquare[i] = normSquare[i] / count[i];
			time[i] = i;
		}
		meanSquareFlow = new ArrayList<float[]>(2);
		meanSquareFlow.add(time);
		meanSquareFlow.add(meanSpeedSquare);
		
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
		peakLocation = new ArrayList<float[]>(2);
		peakLocation.add(peakTime);
		peakLocation.add(peakVal);
		
		if (verbose)
			IJ.log("Peak detection done in "+detector.getProcessingTime()/1000+" s.");

		if (displayPeaks) {
			plot = new Plot("Mean velocity squared for "+rowName, "Frame", "Velocity squared", time, meanSpeedSquare);
			plot.draw();
			plot.setColor(Color.red);
			plot.addPoints(peakTime, peakVal, 0);
		}
		
		return (float) npeaks / time.length; // DIRTY
		
	}

	public static boolean showDialog() {
		
		GenericDialog dialog = new GenericDialog("FlowMate v. "+VERSION_STRING);
		dialog.addMessage("Set threshold in flow accuracy");
		dialog.addNumericField("Threshold", thresholdSettings, 5);
		dialog.addCheckbox("Display flow color image", computeColorFlowImageSettings);
		dialog.addCheckbox("Display peaks analysis", displayPeaksSettings);
		dialog.addCheckbox("Display color wheel", displayColorWheelSettings);
		dialog.addMessage("Display the Vx and Vy flow images");
		dialog.addCheckbox("Display flow images", computeFlowImagesSettings);
		dialog.addMessage("Compute and compute accuracy image\n(on which threshold will be applied)");
		dialog.addCheckbox("Compute accuracy image", computeAccuracyImageSettings);
		dialog.addMessage("Do calculation only for the central frame\n(useful to quickly determine threshold)");
		dialog.addCheckbox("Compute only central frame", computeOnlyCentralFrameSettings);
		
		dialog.showDialog();
		
		if (dialog.wasCanceled())
			return false;
		
		thresholdSettings = dialog.getNextNumber();
		computeColorFlowImageSettings = dialog.getNextBoolean();
		displayPeaksSettings = dialog.getNextBoolean();
		displayColorWheelSettings = dialog.getNextBoolean();		
		computeFlowImagesSettings = dialog.getNextBoolean();
		computeAccuracyImageSettings = dialog.getNextBoolean();
		computeOnlyCentralFrameSettings = dialog.getNextBoolean();
		return true;
	}

	@Override
	public String toString() {
		String str = super.toString();
		str +="\nSettings:";
		str +="\n  Treshold:                   "+threshold;
		str +="\n  Compute color flow image:   "+computeColorFlowImage;
		str +="\n  Compute flow images:        "+computeFlowImages;
		str +="\n  Compute accuracy image:     "+computeAccuracyImage;
		str +="\n  Compute only central frame: "+computeOnlyCentralFrame;
		str +="\n  Display peaks amalysis:     "+displayPeaks;
		str +="\n  Display color wheel:        "+displayColorWheel;
		return str;
	}
	
	/*
	 * GETTER / SETTERS
	 */
	
	public void setThreshold(double threshold) { this.threshold = threshold; }
	public double getThreshold() {return this.threshold; }

	public void setComputeAccuracyImage(boolean computeAccuracyImage) { this.computeAccuracyImage = computeAccuracyImage; }
	public boolean isComputeAccuracyImage() { return this.computeAccuracyImage; }
	
	public ImagePlus getAccuracyImage() { return this.accuracyImage; }

	public void setComputeOnlyCentralFrame(boolean computeOnlyCentralFrame) { this.computeOnlyCentralFrame = computeOnlyCentralFrame; }
	public boolean isComputeOnlyCentralFrame() { return this.computeOnlyCentralFrame; }

	public void setDisplayColorWheel(boolean displayColorWheel) { this.displayColorWheel = displayColorWheel; }

	public void setComputeColorFlowImage(boolean computeColorFlowImage) { this.computeColorFlowImage = computeColorFlowImage; }
	public boolean isComputeColorFlowImage() { return this.computeColorFlowImage; }
	
	public ImagePlus getColorFlowImage() { return this.colorFlow; }

	public void setComputeFlowImages(boolean computeFlowImages) { this.computeFlowImages = computeFlowImages; }
	public boolean isComputeFlowImages() { return this.computeFlowImages; }

	public List<ImagePlus> getFlowImage() { return velocityField; }

	public void setDisplayPeaks(boolean displayPeaks) { this.displayPeaks = displayPeaks; }
	public boolean isDisplayPeaks() { return this.displayPeaks; }
	
	
	public Plot getPeaks() { return this.plot; }

	public void setVerbose(boolean verbose) { this.verbose = verbose; }
	public boolean isVerbose() { return this.verbose; }

	public List<float[]> getFlowMeanSquare() { return this.meanSquareFlow; }
	
	public List<float[]> getPeakLocation() { return this.peakLocation; }

}
