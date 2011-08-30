package mpicbg.imglib.algorithm.pde.test;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;

import mpicbg.imglib.algorithm.pde.AnisotropicDiffusion;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

public class AnistotropicDiffusion_TestDrive {

//	private static final File file = new File("/Users/tinevez/Desktop/Data/blobs.tif");
	private static final File file = new File("/Users/tinevez/Desktop/Data/boats.tif");
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) {
	
		ij.ImageJ.main(args);
		
		ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		imp.show();
		
		Image<? extends RealType> source = ImageJFunctions.wrap(imp);
		
		AnisotropicDiffusion<?> algo = new AnisotropicDiffusion(source, 1, 15, 2, 10);
		algo.setNumThreads(1);
		
		if (!(algo.checkInput() && algo.process())) {
			System.out.println("Failed! With: "+algo.getErrorMessage());
			return;
		}
		
		System.out.println("Done in "+algo.getProcessingTime()+"ms.");// DEBUG
		
		ImagePlus result = ImageJFunctions.copyToImagePlus(algo.getResult());
		result.show();
		result.getProcessor().resetMinAndMax();
		result.updateAndDraw();

	}
}
