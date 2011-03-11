import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


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

public class Time_Noise_Reduce implements PlugInFilter {
   ImagePlus imp;

   public int setup(String arg, ImagePlus imp) {
       this.imp = imp;
       return DOES_ALL;
   }

   public void run(ImageProcessor ip) {
       int WIDTH = 1;
       int ORDER = 0;
       int DERIVATIVE=0;
       GenericDialog gd = new GenericDialog("Constants");

       gd.addNumericField("Half Width of Filter(px): ", WIDTH, 0);
       gd.addNumericField("Order of Filter(even prefered): ", ORDER, 0);
       gd.addNumericField("Derivative Order(Zero for none):",DERIVATIVE ,0);
       gd.showDialog();

       if (gd.wasCanceled())
           return;
       WIDTH = (int)gd.getNextNumber();
       ORDER = (int)gd.getNextNumber();
       DERIVATIVE = (int)gd.getNextNumber();
       

       SavitzkyGolayFilter sgfilter = new SavitzkyGolayFilter(WIDTH, WIDTH, ORDER, DERIVATIVE);




       ImageStack original = imp.getStack();
       ImageStack out = new ImageStack(imp.getWidth(), imp.getHeight());
       float[][] output = new float[imp.getNSlices()][imp.getWidth()*imp.getHeight()];
       float[] originals = new float[imp.getNSlices()];
       float[] px;

       //performs a 'z convolution' with px as the kernel
       for(int j = 0; j<imp.getHeight(); j++){
           for(int k = 0; k<imp.getWidth(); k++){

               for(int i = 1; i<=imp.getNSlices(); i++){
                   originals[i-1] = original.getProcessor(i).getf(k,j);
               }

               px = sgfilter.filterData(originals);


               for(int i = 0; i<imp.getNSlices(); i++){

                   output[i][j*imp.getWidth() + k] = px[i];

               }

           }
       }

       ImageProcessor improc;
       String[] labels = original.getSliceLabels();
       for(int i = 0; i<imp.getNSlices(); i++){
           improc = new FloatProcessor(imp.getWidth(), imp.getHeight());
           improc.setPixels(output[i]);
           out.addSlice(labels[i], improc);


       }

       ImagePlus y = new ImagePlus("SGtime_o" + ORDER + "_w" + WIDTH + "_d" + DERIVATIVE, out);
       y.show();

   }




}