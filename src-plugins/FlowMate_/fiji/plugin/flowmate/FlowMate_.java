package fiji.plugin.flowmate;

import java.util.ArrayList;
import java.util.List;


import fiji.plugin.flowmate.opticflow.LucasKanade;
import fiji.plugin.flowmate.util.OpticFlowUtils;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class FlowMate_<T extends RealType<T>>   implements PlugIn {

	@Override
	public void run(String arg) {
		
		ImagePlus imp = WindowManager.getCurrentImage();

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

		
		
		Image<RGBALegacyType> flow = OpticFlowUtils.createColorFlowImage(opticFlow.get(0), opticFlow.get(1));
		ImageJFunctions.copyToImagePlus(flow).show();

		Image<RGBALegacyType> indicator = OpticFlowUtils.createIndicatorImage(64);
		ImageJFunctions.copyToImagePlus(indicator).show();

	}

}
