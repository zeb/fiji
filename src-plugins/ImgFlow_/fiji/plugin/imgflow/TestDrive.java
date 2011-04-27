package fiji.plugin.imgflow;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;


public class TestDrive {
	
	
	private static final File TEST_FILE = new File(TestDrive.class.getResource("flow.tif").getFile()); 

	public static <T extends RealType<T>> void  main(String[] args) {
		
		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(TEST_FILE.getAbsolutePath());
		imp.show();
		
		Image<T> img = ImageJFunctions.wrap(imp);
		
		System.out.println("Number of dimension: "+img.getNumDimensions());
		for (int i = 0; i < img.getNumDimensions(); i++) 
			System.out.println(" - for dim "+i+", size is "+img.getDimension(i));

		SimoncelliDerivation<T> filter = new SimoncelliDerivation<T>(img);
		
		ArrayList<Image<FloatType>> outputs = new  ArrayList<Image<FloatType>>(img.getNumDimensions()); 
		for (int i = 0; i < img.getNumDimensions(); i++) {
			filter.setDerivationDimension(i);
			filter.checkInput();
			filter.process();
			Image<FloatType> output = filter.getResult();
			outputs.add(output);
			ImageJFunctions.copyToImagePlus(output).show();
		}
		System.out.println("Filtering done in "+filter.getProcessingTime()+" ms");
		
		
	}
	
	
	
}
