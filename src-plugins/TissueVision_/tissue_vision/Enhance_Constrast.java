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
import ij.plugin.PlugIn;
import java.io.FilenameFilter;

import java.io.File;

import fiji.util.GenericDialogPlus;


public class Enhance_Constrast implements PlugIn
{ 
	/** source directory **/
	public static String sourceDirectory="";


	//-----------------------------------------------------------------------------------------
	/**
	 * Correct the illumination of a specific image by dividing it for another image.
	 */
	static private Callable<String> correctImage( 
			final String inputDirectory,  
			final String sName)
			{
		return new Callable<String>() {
			public String call() {
				try {
					ImagePlus imp1 = new ImagePlus(inputDirectory + sName);

					if(null != imp1)
					{
						IJ.run(imp1, "Enhance Contrast", "saturated=0.1");
						
						FileSaver fs = new FileSaver(imp1);
						fs.saveAsTiff(inputDirectory+sName);
						//close();
					}
					return inputDirectory+sName;
				} catch (Exception e) {
					e.printStackTrace();					
					return inputDirectory+sName;
				}
			}
		};
			} 



	public void run(String arg) 
	{
		GenericDialogPlus gd = new GenericDialogPlus("Enhance sequence contrast");

		gd.addDirectoryField("Source directory", sourceDirectory, 50);

		gd.showDialog();

		// Exit when canceled
		if (gd.wasCanceled()) 
			return;

		sourceDirectory = gd.getNextString();

		String source_dir = sourceDirectory;
		if (null == source_dir) 
			return;
		source_dir = source_dir.replace('\\', '/');
		if (!source_dir.endsWith("/")) source_dir += "/";

		exec(source_dir);

	}


	public static boolean exec(final String source_dir)
	{
		
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
			jobs[i] = exe.submit(correctImage(source_dir, src_names[i])); 	
		}

		// Join
		int n = 1;
		IJ.showProgress(0.0);
		for (final Future<String> job : jobs) {
			String filename = null;
			try {
				IJ.showStatus("Enhancing image " + n + "/" +src_names.length);
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

		IJ.showMessage("Constrast enhancement of " + src_names.length + " images is done!");
		IJ.showProgress(1.0);
		exe.shutdownNow();

		return true;
	}	

}


