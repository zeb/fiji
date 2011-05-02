package fiji.plugin.flowmate;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

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
	private static final File TEST_FILE = new File("/Users/tinevez/Desktop/Data/Stack.tif");

	public static <T extends RealType<T>> void  main(String[] args) {
		
		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(TEST_FILE.getAbsolutePath());
		imp.show();
		
		Image<T> img = ImageJFunctions.wrap(imp);
		
		System.out.println("Number of dimension: "+img.getNumDimensions());
		for (int i = 0; i < img.getNumDimensions(); i++) 
			System.out.println(" - for dim "+i+", size is "+img.getDimension(i));

		SimoncelliDerivation<T> filter = new SimoncelliDerivation<T>(img);
		
		ArrayList<Image<FloatType>> derivatives = new  ArrayList<Image<FloatType>>(img.getNumDimensions()); 
		for (int i = 0; i < img.getNumDimensions(); i++) {
			filter.setDerivationDimension(i);
			filter.checkInput();
			filter.process();
			Image<FloatType> output = filter.getResult();
			derivatives.add(output);
		}
		System.out.println("Filtering done in "+filter.getProcessingTime()+" ms.");

//		for (Image<FloatType> derivative : derivatives) {
//			ImageJFunctions.copyToImagePlus(derivative).show();
//		}
				
		// Optic flow
		LucasKanade opticFlowAlgo = new LucasKanade(derivatives);
		opticFlowAlgo.checkInput();
		opticFlowAlgo.process();
		List<Image<FloatType>> opticFlow = opticFlowAlgo.getResults();
		System.out.println("Computing flow done in "+opticFlowAlgo.getProcessingTime()+" ms.");
		
//		for (Image<FloatType> speedComponent : opticFlow) {
//			ImageJFunctions.copyToImagePlus(speedComponent).show();
//		}
		
//		List<Image<FloatType>> eigenvalues= opticFlowAlgo.getEigenvalues();
//		for (Image<FloatType> eigenvalue : eigenvalues) {
//			ImageJFunctions.copyToImagePlus(eigenvalue).show();
//		}
		
		Image<RGBALegacyType> flow = LucasKanade.convertToFlowImage(opticFlow.get(0), opticFlow.get(1));
		ImageJFunctions.copyToImagePlus(flow).show();
		
		Image<RGBALegacyType> indicator = LucasKanade.createIndicatorImage(256);
		ImageJFunctions.copyToImagePlus(indicator).show();
		
	}
	
	
	
}
