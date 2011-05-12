package fiji.plugin.flowmate;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;


import fiji.plugin.flowmate.analysis.NormSquareSummer;
import fiji.plugin.flowmate.analysis.PeakDetector;
import fiji.plugin.flowmate.opticflow.LucasKanade;
import fiji.plugin.flowmate.util.OpticFlowUtils;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Plot;
import ij.plugin.PlugIn;

public class FlowMate_<T extends RealType<T>>   implements PlugIn {

	@Override
	public void run(String arg) {
		
		ImagePlus imp = WindowManager.getCurrentImage();

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
		opticFlowAlgo.checkInput();
		opticFlowAlgo.process();
		List<Image<FloatType>> opticFlow = opticFlowAlgo.getResults();
		IJ.log("Computing flow done in "+opticFlowAlgo.getProcessingTime()+" ms.");

		//		for (Image<FloatType> speedComponent : opticFlow) {
		//			ImageJFunctions.copyToImagePlus(speedComponent).show();
		//		}

		//		List<Image<FloatType>> eigenvalues= opticFlowAlgo.getEigenvalues();
		//		for (Image<FloatType> eigenvalue : eigenvalues) {
		//			ImageJFunctions.copyToImagePlus(eigenvalue).show();
		//		}

		
		
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
		
		IJ.log("Peak detection:");
		PeakDetector detector = new PeakDetector(meanSpeedSquare);
		int[] peakLocations = detector.process(5, 1);
		float[] peakTime = new float[peakLocations.length];
		float[] peakVal = new float[peakLocations.length];
		for (int i = 0; i < peakLocations.length; i++) {
			peakTime[i] = time[peakLocations[i]];
			peakVal[i] = meanSpeedSquare[peakLocations[i]];
			IJ.log("At t="+peakTime[i] +" - Val = "+peakVal[i]);
		}
		

		Plot plot = new Plot("Mean velocity squared", "Frame", "Velocity squared", time, meanSpeedSquare);
		plot.draw();
		plot.setColor(Color.red);
		plot.addPoints(peakTime, peakVal, 0);
		plot.show();
	}

}
