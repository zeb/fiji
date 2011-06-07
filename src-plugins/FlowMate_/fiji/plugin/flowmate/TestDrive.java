package fiji.plugin.flowmate;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;

import mpicbg.imglib.type.numeric.RealType;


public class TestDrive {
	
	
	private static final File TEST_FILE = new File(TestDrive.class.getResource("flow.tif").getFile());
//	private static final File TEST_FILE = new File("/Users/tinevez/Desktop/Amibes/IL-8 3 nM uni-1_01.tif");

	public static <T extends RealType<T>> void  main(String[] args) {
		
		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(TEST_FILE.getAbsolutePath());
		imp.show();
		
//		imp.setRoi(new PolygonRoi(new int[] {10, 20, 30, 30, 20, 10}, new int[] {10, 20, 10, 40, 30, 40}, 6, Roi.POLYGON));
		
		new FlowMate_().run(null);
		
	}
	
	
	
}
