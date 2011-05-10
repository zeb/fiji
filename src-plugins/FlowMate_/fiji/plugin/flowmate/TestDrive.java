package fiji.plugin.flowmate;

import fiji.plugin.flowmate.analysis.NormSquareSummer;
import fiji.plugin.flowmate.analysis.PeakDetector;
import fiji.plugin.flowmate.opticflow.LucasKanade;
import fiji.plugin.flowmate.util.OpticFlowUtils;
import fiji.plugin.flowmate.util.Windows;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Plot;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;


public class TestDrive {
	
	
//	private static final File TEST_FILE = new File(TestDrive.class.getResource("flow.tif").getFile());
	private static final File TEST_FILE = new File("/Users/tinevez/Desktop/Amibes/IL-8 3 nM uni-1_01.tif");

	public static <T extends RealType<T>> void  main(String[] args) {
		
		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(TEST_FILE.getAbsolutePath());
		imp.show();
		
		Image<T> img = ImageJFunctions.wrap(imp);
		
		System.out.println("Number of dimension: "+img.getNumDimensions());
		for (int i = 0; i < img.getNumDimensions(); i++) 
			System.out.println(" - for dim "+i+", size is "+img.getDimension(i));

		SimoncelliDerivation<T> filter = new SimoncelliDerivation<T>(img, 5);
		
		ArrayList<Image<FloatType>> derivatives = new  ArrayList<Image<FloatType>>(img.getNumDimensions()); 
		for (int i = 0; i < img.getNumDimensions(); i++) {
			filter.setDerivationDimension(i);
			filter.checkInput();
			filter.process();
			Image<FloatType> output = filter.getResult();
			derivatives.add(output);
		}
		System.out.println("Computing derivatives done in "+filter.getProcessingTime()+" ms.");

//		for (Image<FloatType> derivative : derivatives) {
//			ImageJFunctions.copyToImagePlus(derivative).show();
//		}
				
		// Optic flow
		float[] gaussWindow = Windows.getGaussianWindow();
		int[] windowSize = {7, 7, 1};
		
		LucasKanade opticFlowAlgo = new LucasKanade(derivatives);
//		opticFlowAlgo.setNumThreads(1);
		opticFlowAlgo.setWindow(gaussWindow, windowSize);
		opticFlowAlgo.checkInput();
		opticFlowAlgo.process();
		List<Image<FloatType>> opticFlow = opticFlowAlgo.getResults();
		System.out.println("Computing flow done in "+opticFlowAlgo.getProcessingTime()+" ms.");
		
//		for (Image<FloatType> speedComponent : opticFlow) {
//			ImageJFunctions.copyToImagePlus(speedComponent).show();
//		}
//		
//		List<Image<FloatType>> eigenvalues= opticFlowAlgo.getEigenvalues();
//		for (Image<FloatType> eigenvalue : eigenvalues) {
//			ImageJFunctions.copyToImagePlus(eigenvalue).show();
//		}
		
//		List<Image<FloatType>> polar = OpticFlowUtils.convertToPolar(opticFlow.get(0), opticFlow.get(1));
//		for (Image<FloatType> polCoord : polar) 
//			ImageJFunctions.copyToImagePlus(polCoord).show();
		
		Image<RGBALegacyType> flow = OpticFlowUtils.createColorFlowImage(opticFlow.get(0), opticFlow.get(1));
		ImageJFunctions.copyToImagePlus(flow).show();
		
		Image<RGBALegacyType> indicator = OpticFlowUtils.createIndicatorImage(64);
		ImageJFunctions.copyToImagePlus(indicator).show();
		
		NormSquareSummer summer = new NormSquareSummer(opticFlow.get(0), opticFlow.get(1));
		summer.checkInput();
		summer.process();
		System.out.println("Summing norm done in "+summer.getProcessingTime()+" ms.");
		
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
		float[] peakTime = new float[peakLocations.length];
		float[] peakVal = new float[peakLocations.length];
		for (int i = 0; i < peakLocations.length; i++) {
			peakTime[i] = time[peakLocations[i]];
			peakVal[i] = meanSpeedSquare[peakLocations[i]];
			System.out.println("At t="+peakTime[i] +" - Val = "+peakVal[i]);
		}
		
		Plot plot = new Plot("Mean velocity squared", "Frame", "Velocity squared", time, meanSpeedSquare);
		plot.draw();
		plot.setColor(Color.red);
		plot.addPoints(peakTime, peakVal, 0);
		plot.show();
		
	}
	
	
	
}
