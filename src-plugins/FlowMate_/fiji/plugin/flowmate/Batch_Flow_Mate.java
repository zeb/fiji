package fiji.plugin.flowmate;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Hashtable;

import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

public class Batch_Flow_Mate implements PlugIn {

	@Override
	public void run(String arg) {

		// Instantiate plugin
		FlowMate_ flowMate = new FlowMate_();
		double threshold = 2000;
		flowMate.setThreshold(threshold);
		flowMate.setComputeColorFlowImage(true);
		flowMate.setDisplayPeaks(true);
		
		RoiManager roiManager = new RoiManager();
		ResultsTable mainTable = ResultsTable.getResultsTable();
		mainTable.show("Results");

		String rootFolderName = IJ.getDirectory("Select raw files folder");
		File rootFolder = new File(rootFolderName);
		
		IJ.log("Parsing folder "+rootFolder.getName());
		
		FilenameFilter lifFilesFilter = new FilenameFilter() {			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".lif");
			}
		};
		
		File[] files = rootFolder.listFiles(lifFilesFilter);
		
		IJ.log("Found "+files.length+" lif files");
		
		for(File file : files) {
			
			if (file.isDirectory())
				continue;
			
			IJ.log("Opening file "+file.getName());
			
			ImageReader reader = new ImageReader();
			try {
				reader.setId(file.getAbsolutePath());
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			int nseries = reader.getSeriesCount();
			IJ.log("Found "+nseries+" series in file");
			
			for (int i = 0; i < nseries; i++) {
				final int seriesIndex = i + 1;

				IJ.log("Reading series number "+seriesIndex);

				ImporterOptions options = null;
				try {
					options = new ImporterOptions();
					options.parseOptions("");
					options.setId(file.getAbsolutePath());
					for (int j = 0; j < nseries; j++)
						if (i == j )
							options.setSeriesOn(j, true);
						else
							options.setSeriesOn(j, false);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				ImportProcess process = new ImportProcess(options);
				try {
					process.execute();
				} catch (FormatException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				ImagePlusReader ipReader = new ImagePlusReader(process);
				ImagePlus[] imps = null;
				try {
					imps = ipReader.openImagePlus();
				} catch (FormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				ImagePlus imp = imps[0];
				
				// Now read the ROIS
				
				// Find adequate filename. We ask the ROI file to be named as follow:
				// Their name should start with the image file name, and it should finish 
				// with the number of the series (e.g. __XP2.zip or ___XP2.roi for the 2nd series)
				FilenameFilter zipFilesFilter = new FilenameFilter() {			
					@Override
					public boolean accept(File dir, String name) {
						return (name.endsWith(seriesIndex+".zip") ||  name.endsWith(seriesIndex+".roi"));
					}
				};
				
				File[] zipFiles = rootFolder.listFiles(zipFilesFilter);
				File roiFile = null;
				for(File zipFile : zipFiles) {
					if (zipFile.getName().startsWith(file.getName().substring(0, file.getName().lastIndexOf(".")))) {
						roiFile = zipFile;
						break;
					}
				}
				if (null == roiFile) {
					IJ.log("Roi file not found for file "+file.getName()+" series "+seriesIndex+", skipping");
				} else {
					
					
					// Empty roi manager
					IJ.log("Emptying ROI manager");
					roiManager.getROIs().clear();
					roiManager.getList().removeAll();

					// Loading zip file
					IJ.log("Loading roi file "+roiFile.getName());
					roiManager.runCommand("Open", roiFile.getAbsolutePath());
					
					IJ.log("Found "+roiManager.getCount()+" ROIs in file");
					@SuppressWarnings("rawtypes")
					Hashtable rois = roiManager.getROIs();
					
					for (Object obj : rois.values()) {
						Roi roi = (Roi) obj;
						
						IJ.log("Analyzing for roi "+roi.getName());
						imp.setRoi(roi);
						
						String rowName = imp.getShortTitle();
						if (null != roi) {
							String roiName = roi.getName() == null ? "roi" : roi.getName();
							rowName += "-"+roiName;
						}
						
						float npeaksPerFrame = flowMate.process(imp);

						ImagePlus colorFlowImp = flowMate.getColorFlowImage();
						IJ.save(colorFlowImp, rootFolderName+rowName+"-flow.tif");
						colorFlowImp.changes = false;
						colorFlowImp.close();
						
						ImagePlus plotImp = flowMate.getPeaks().getImagePlus();
						IJ.save(plotImp, rootFolderName+rowName+"-peaks.tif");
						plotImp.changes = false;
						plotImp.close();

						mainTable.incrementCounter();
						mainTable.addLabel(rowName);
						mainTable.addValue(FlowMate_.PEAK_NUMBER_COLUMN_NAME, npeaksPerFrame);
						mainTable.show("Results");
						
						IJ.log("______________________________");
						
					} // Loop over Rois
					
				} // If we have Rois 

				
			} // Loop over series
					
			
		}
		
		
		

	}
	
	
	
	
	
	public static void main(String[] args) {
		ij.ImageJ.main(args);
		new Batch_Flow_Mate().run(null);
	}

}
