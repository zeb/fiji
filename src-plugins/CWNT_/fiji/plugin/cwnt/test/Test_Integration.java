package fiji.plugin.cwnt.test;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;

import fiji.plugin.cwnt.TrackMate_CWNT;

public class Test_Integration {
	
	public static void main(String[] args) {
		
		TrackMate_CWNT plugin = new TrackMate_CWNT();

		
//		File testImage = new File("/Users/tinevez/Projects/BRajasekaran/Data/Meta-nov7mdb18ssplus-embryo2-2.tif");
		File testImage = new File("E:/Users/JeanYves/Documents/Projects/BRajaseka/Data/Meta-nov7mdb18ssplus-embryo2-2.tif");
//		File testImage = new File("E:/Users/JeanYves/Documents/Projects/BRajaseka/Data/Meta-nov7mdb18ssplus-embryo2-1.tif");
//		File testImage = new File("/Users/tinevez/Desktop/Data/10-01-21-1hour-bis.tif");

		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(testImage.getAbsolutePath());
		imp.show();

		plugin.run("");
		
	}

}
