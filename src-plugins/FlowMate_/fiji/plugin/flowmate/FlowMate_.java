package fiji.plugin.flowmate;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;


import fiji.plugin.flowmate.analysis.NormSquareSummer;
import fiji.plugin.flowmate.analysis.PeakDetector;
import fiji.plugin.flowmate.opticflow.LucasKanade;
import fiji.plugin.flowmate.util.OpticFlowUtils;
import fiji.plugin.flowmate.util.Windows;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;

public class FlowMate_<T extends RealType<T>>   implements PlugIn {

	private static final String VERSION_STRING = "alpha-6/6/11";
	private static final String PEAK_NUMBER_COLUMN_NAME = "Peaks per frame";
	private double threshold;

	@Override
	public void run(String arg) {
		
		if (!launchDialog())
			return;
				
		ImagePlus imp = WindowManager.getCurrentImage();
		Roi roi = imp.getRoi();
		if (null != roi)
			imp = new Duplicator().run(imp);
		
		Image<T> img = ImageJFunctions.wrap(imp);

		IJ.log("Number of dimension: "+img.getNumDimensions());
		for (int i = 0; i < img.getNumDimensions(); i++) 
			IJ.log(" - for dim "+i+", size is "+img.getDimension(i));

		SimoncelliDerivation<T> filter = new SimoncelliDerivation<T>(img, 5);

		ArrayList<Image<FloatType>> derivatives = new  ArrayList<Image<FloatType>>(img.getNumDimensions()); 
		for (int i = 0; i < img.getNumDimensions(); i++) {
			filter.setDerivationDimension(i);
			filter.checkInput();
			filter.process();
			Image<FloatType> output = filter.getResult();
			derivatives.add(output);
		}
		IJ.log("Computing derivatives done in "+filter.getProcessingTime()+" ms.");

		//		for (Image<FloatType> derivative : derivatives) {
		//			ImageJFunctions.copyToImagePlus(derivative).show();
		//		}

		// Optic flow
		LucasKanade opticFlowAlgo = new LucasKanade(derivatives);
		opticFlowAlgo.setThreshold(threshold);
		opticFlowAlgo.setWindow(Windows.getFlat5x5Window(), new int[] {5, 5, 1});
		opticFlowAlgo.checkInput();
		opticFlowAlgo.process();
		List<Image<FloatType>> opticFlow = opticFlowAlgo.getResults();
		IJ.log("Computing flow done in "+opticFlowAlgo.getProcessingTime()+" ms.");

		//		for (Image<FloatType> speedComponent : opticFlow) {
		//			ImageJFunctions.copyToImagePlus(speedComponent).show();
		//		}

		List<Image<FloatType>> eigenvalues= opticFlowAlgo.getEigenvalues();
		for (Image<FloatType> eigenvalue : eigenvalues) {
			ImageJFunctions.copyToImagePlus(eigenvalue).show();
		}

		Image<RGBALegacyType> flow = OpticFlowUtils.createColorFlowImage(opticFlow.get(0), opticFlow.get(1));
		ImageJFunctions.copyToImagePlus(flow).show();

		Image<RGBALegacyType> indicator = OpticFlowUtils.createIndicatorImage(64);
		ImageJFunctions.copyToImagePlus(indicator).show();

		// Analysis
		NormSquareSummer summer = new NormSquareSummer(opticFlow.get(0), opticFlow.get(1));
		summer.checkInput();
		summer.process();
		IJ.log("Summing norm done in "+summer.getProcessingTime()+" ms.");
		
		float[] normSquare = summer.getSquareNorm();
		int[] count = summer.getCount();
		float[] meanSpeedSquare = new float[normSquare.length];
		float[] time = new float[normSquare.length];
		for (int i = 0; i < meanSpeedSquare.length; i++) { 
			meanSpeedSquare[i] = normSquare[i] / count[i];
			time[i] = i;
		}
		
		PeakDetector detector = new PeakDetector(meanSpeedSquare);
		int[] peakLocations = detector.process(5, 1);
		int npeaks = peakLocations.length;
		float[] peakTime = new float[peakLocations.length];
		float[] peakVal = new float[peakLocations.length];
//		IJ.log("Peak detection:");
		for (int i = 0; i < npeaks; i++) {
			peakTime[i] = time[peakLocations[i]];
			peakVal[i] = meanSpeedSquare[peakLocations[i]];
//			IJ.log("At t="+peakTime[i] +" - Val = "+peakVal[i]);
		}

//		int ncol = mainTable.getColumnIndex(PEAK_NUMBER_COLUMN_NAME);
//		if (ncol == ResultsTable.COLUMN_NOT_FOUND) {
//?		}
		
		Plot plot = new Plot("Mean velocity squared", "Frame", "Velocity squared", time, meanSpeedSquare);
		plot.draw();
		plot.setColor(Color.red);
		plot.addPoints(peakTime, peakVal, 0);
		plot.show();
		
		ResultsTable mainTable = ResultsTable.getResultsTable();
		float npeaksPerFrame = (float) npeaks / img.getDimension(2); // DIRTY
		String rowName = imp.getShortTitle();
		if (null != roi)
			rowName += "-"+roi.getName();
		mainTable.incrementCounter();
		mainTable.addLabel(rowName);
		mainTable.addValue(PEAK_NUMBER_COLUMN_NAME, npeaksPerFrame);
		mainTable.show("Results");

	}

	private boolean launchDialog() {
		
		GenericDialog dialog = new GenericDialog("FlowMate v. "+VERSION_STRING);
		dialog.addNumericField("Threshold", 1, 5);
		dialog.showDialog();
		
		if (dialog.wasCanceled())
			return false;
		
		threshold = dialog.getNextNumber();
		return true;
		
		
	}
	
}
