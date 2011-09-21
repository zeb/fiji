package fiji.plugin.nucleitracker.test;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.util.List;

import mpicbg.imglib.algorithm.GaussianGradient2D;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

public class GaussianGradient_TestDrive {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) {
		
		File testImage = new File("/Users/tinevez/Projects/BRajaseka/Data/point.tif");
		
		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(testImage.getAbsolutePath());
		imp.show();

		Image<? extends RealType> source = ImageJFunctions.wrap(imp);

		GaussianGradient2D grad = new GaussianGradient2D(source, 1);
		grad.checkInput();
		grad.process();
		
		Image norm = grad.getResult();
		List<Image> list = grad.getGradientComponents();
		
		ImageJFunctions.copyToImagePlus(norm).show();
		ImageJFunctions.copyToImagePlus(list.get(0)).show();
		ImageJFunctions.copyToImagePlus(list.get(1)).show();
		

		
	}
	
}
