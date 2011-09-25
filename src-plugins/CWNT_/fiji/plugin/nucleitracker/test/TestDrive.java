package fiji.plugin.nucleitracker.test;	

import fiji.plugin.nucleitracker.CrownWearingSegmenter;
import fiji.plugin.nucleitracker.NucleiMasker;
import fiji.plugin.nucleitracker.splitting.NucleiSplitter;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.labeling.Labeling;
import mpicbg.imglib.type.numeric.RealType;

@SuppressWarnings("unused")
public class TestDrive {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) {
		
		File testImage = new File("E:/Users/JeanYves/Documents/Projects/BRajaseka/Data/Meta-nov7mdb18ssplus-embryo2-1.tif");
//		File testImage = new File("/Users/tinevez/Projects/BRajaseka/Data/Meta-nov7mdb18ssplus-embryo2-1.tif");
		
		
		
		 /* http://stackoverflow.com/questions/1548606/java-link-jslider-and-jtextfield-for-float-value */
		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(testImage.getAbsolutePath());
		imp.show();
		
		Image<? extends RealType> source = ImageJFunctions.wrap(imp);
		
		CrownWearingSegmenter algo = new CrownWearingSegmenter(source);
		algo.setNumThreads();
		System.out.println("Using "+algo.getNumThreads()+" threads:");
		Labeling results = null ;
		boolean check = algo.checkInput() && algo.process();
		if (check) {
			results = algo.getResult();
			ImagePlus impres = ImageJFunctions.copyToImagePlus(results);
			impres.getProcessor().resetMinAndMax();
			impres.updateAndDraw();
			impres.show();
		} else {
			System.err.println("Process failed: "+algo.getErrorMessage());
		}

		NucleiSplitter splitter = new NucleiSplitter(results);
		splitter.setNumThreads();
		Labeling results2;
		
		check = algo.checkInput() && algo.process();
		if (check) {
			results2 = algo.getResult();
			ImagePlus impres = ImageJFunctions.copyToImagePlus(results2);
			impres.getProcessor().resetMinAndMax();
			impres.updateAndDraw();
			impres.show();
		} else {
			System.err.println("Process failed: "+algo.getErrorMessage());
		}

		
		System.out.println("Total processing time: "+(algo.getProcessingTime()/1000)+" s");
		
		
	}
	
}
