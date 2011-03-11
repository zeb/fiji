import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

import Jama.*;

  /**
   *
   *  Implements a Savitzky-Golay smooting filter over each image.
   *  Requires the Jama matrix library.
   * 
   *     @Author: Matthew B. Smith 
   *     @Url: http://orangepalantir.org
   *     @Version: 0.8
   *     @Date: 2/10/2010
  * */
  
public class Space_Noise_Reduce implements PlugInFilter {
	ImagePlus imp;
    
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
        int WIDTH = 6;
        int ORDER = 4;
        
        GenericDialog gd = new GenericDialog("Constants");

        gd.addNumericField("Half Width of Filter(px): ", WIDTH, 2);
        gd.addNumericField("Order of Filter(even prefered): ", ORDER, 2);
        
        gd.showDialog();
        
        if (gd.wasCanceled())
            return;
        WIDTH = (int)gd.getNextNumber();
        ORDER = (int)gd.getNextNumber();
        
        double[][] kernel = SavitzkyGolayFilter.getKernel(WIDTH,ORDER);
        float[] fkernel = new float[kernel.length*kernel[0].length];
        for(int i = 0; i<fkernel.length; i++){
            int xdex = i/kernel.length;
            int ydex = i%kernel.length;
            fkernel[i] = (float)kernel[ydex][xdex];

        }
        ImageStack original = imp.getStack();
        ImageStack out = new ImageStack(imp.getWidth(), imp.getHeight());


        String[] labels = original.getSliceLabels();
        for(int i = 1; i<=imp.getNSlices(); i++){
            ImageProcessor improc = original.getProcessor(i).duplicate();
            improc.convolve(fkernel, kernel.length, kernel[0].length);

            out.addSlice(labels[i-1], improc);
            
        }
        
        
        
        ImagePlus y = new ImagePlus("SG_Space_o" + ORDER + "_w" + WIDTH , out);
        y.show();
            
	}
    
    
    
}
