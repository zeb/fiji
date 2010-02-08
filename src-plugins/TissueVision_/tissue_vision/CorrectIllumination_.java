package tissue_vision;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
import ij.process.Blitter;
import ij.plugin.PlugIn;
import java.io.FilenameFilter;

import java.io.File;

import fiji.util.GenericDialogPlus;


public class CorrectIllumination_ implements PlugIn
{ 
	/** source directory **/
	public static String sourceDirectory="";
	/** output directory **/
	public static String outputDirectory="";
	/** correction image (with path) */
	public static String correctionImagePath ="";
	/** gamma value */
	public static double gammaValue = 0.65;

	//-----------------------------------------------------------------------------------------
	/**
	 * Correct the illumination of a specific image by dividing it for another image.
	 */
	static private Callable<String> correctImage( 
			final String inputDirectory, 
			final String outputDirectory, 
			final ImageProcessor correctionImage, 
			final String sName,
			final double gammaValue,
			final double minValue,
			final double maxValue)
	{
		return new Callable<String>() {
			public String call() {
				try {
					ImagePlus imp1 = new ImagePlus(inputDirectory + sName);

					if(null != imp1)
					{
						ImageProcessor ip1 = imp1.getProcessor().convertToFloat();	
						//imageCalculator("Divide create 32-bit", sName,"illuminationCorrectionImage.tif");
						ip1.copyBits(correctionImage, 0, 0, Blitter.DIVIDE);
						ip1.setMinAndMax(minValue, maxValue);
						//ip1.resetMinAndMax();
						//selectWindow("Result of "+sName);
						//run("8-bit");
						//selectWindow(sName);
						//close();
						ip1 = ip1.convertToByte(true);
						ip1.gamma(gammaValue);
						final ImagePlus result = new ImagePlus("", ip1);

						//IJ.run(result,"8-bit","");
						//IJ.run(result, "Gamma...", "value=" + gammaValue);
						//selectWindow("Result of "+sName);
						//IJ.saveAs(result, "PNG", outputDirectory+sName);							
						FileSaver fs = new FileSaver(result);
						fs.saveAsPng(outputDirectory+sName);
						//close();
					}
					return outputDirectory+sName;
				} catch (Exception e) {
					e.printStackTrace();					
					return outputDirectory+sName;
				}
			}
		};
	} 



	public void run(String arg) 
	{
		GenericDialogPlus gd = new GenericDialogPlus("Correct Illumination");

		gd.addDirectoryField("Source directory", sourceDirectory, 50);
		gd.addDirectoryField("Output directory", outputDirectory, 50);
		gd.addFileField("Correction image", correctionImagePath, 50);
		gd.addNumericField("Gamma value", gammaValue, 2, 5, "");			

		gd.showDialog();

		// Exit when canceled
		if (gd.wasCanceled()) 
			return;

		sourceDirectory = gd.getNextString();
		outputDirectory = gd.getNextString();
		correctionImagePath = gd.getNextString();
		gammaValue = gd.getNextNumber();
		//minValue = gd.getNextNumber();
		//maxValue = gd.getNextNumber();


		String source_dir = sourceDirectory;
		if (null == source_dir) 
			return;
		source_dir = source_dir.replace('\\', '/');
		if (!source_dir.endsWith("/")) source_dir += "/";


		String target_dir = outputDirectory;
		if (null == target_dir) 
			return;
		target_dir = target_dir.replace('\\', '/');
		if (!target_dir.endsWith("/")) target_dir += "/";		

		String corr_image = correctionImagePath;
		if (null == corr_image) 
			return;
		corr_image = corr_image.replace('\\', '/');


		exec(source_dir, target_dir, corr_image, gammaValue);

	}

	
	public static boolean exec(
			final String source_dir, 
			final String target_dir, 
			final String corr_image, 
			final double gammaValue)
	{
		final ImageProcessor correctionImage = (new ImagePlus(corr_image)).getProcessor();

		final double minValue = 0.0;
		final double maxValue = 255.0 / correctionImage.getMin();

		// Get source file listing
		final String exts = ".tif.jpg.png.gif.tiff.jpeg.bmp.pgm";
		final String[] src_names = new File(source_dir).list(new FilenameFilter() 
		{
			public boolean accept(File dir, String name) 
			{
				int idot = name.lastIndexOf('.');
				if (-1 == idot) return false;
				return exts.contains(name.substring(idot).toLowerCase());
			}
		});
		Arrays.sort(src_names);



		ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final Future<String>[] jobs = new Future[src_names.length];


		for(int i=0; i < src_names.length ; i++)
		{			
			jobs[i] = exe.submit(correctImage(source_dir, target_dir, correctionImage, src_names[i], gammaValue, minValue, maxValue)); 	
		}

		// Join
		int n = 1;
		IJ.showProgress(0.0);
		for (final Future<String> job : jobs) {
			String filename = null;
			try {
				IJ.showStatus("Correcting image " + n + "/" +src_names.length);
				IJ.showProgress((double) n / src_names.length);
				filename = job.get();
				n++;
			} catch (InterruptedException e) {
				IJ.error("Interruption exception! (file " + filename + ")");
				e.printStackTrace();
				exe.shutdownNow();
				return false;
			} catch (ExecutionException e) {
				IJ.error("Execution exception! (file " + filename + ")");
				e.printStackTrace();
				exe.shutdownNow();
				return false;
			}

		}

		IJ.showMessage("Illumination correction of " + src_names.length + " images is done!");
		IJ.showProgress(1.0);
		exe.shutdownNow();

		return true;
	}	

}
