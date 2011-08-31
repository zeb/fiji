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
		
		Image<? extends RealType> source = ImageJFunctions.wrap(imp);
		
		AnisotropicDiffusion<?> algo = new AnisotropicDiffusion(source, 1, 20);
//		AnisotropicDiffusion<?> algo = new AnisotropicDiffusion(source, 1, new AnisotropicDiffusion.WideRegionEnhancer(20));
		algo.setNumThreads();
		
		if (!algo.checkInput()) {
			System.out.println("Check input failed! With: "+algo.getErrorMessage());
			return;
		}
		
		imp.show();

		int niter = 10;
		for (int i = 0; i < niter; i++) {
			System.out.println("Iteration "+(i+1)+" of "+niter+".");
			algo.process();
			imp.updateAndDraw();
		}
		
		System.out.println("Done in "+algo.getProcessingTime()+" ms.");

		

	}
}
