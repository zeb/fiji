package fiji.plugin.flowmate;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.text.TextPanel;
import ij.text.TextWindow;

import java.awt.Font;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

public class Batch_Flow_Mate implements PlugIn {

	@Override
	public void run(String arg) {

		// Instantiate and configure plugin
		FlowMate_ flowMate = new FlowMate_();
		if (!FlowMate_.showDialog())
			return;
		flowMate.assignParameters();
		flowMate.setVerbose(false);
		
		// Create Log window
		TextWindow logWindow = new TextWindow("Log for Batch FlowMate_", "", 600, 500);
		TextPanel log = logWindow.getTextPanel();
		log.setFont(new Font("Monospaced", Font.PLAIN, 11), true);
		
		// Get ref to ROI manager
		RoiManager roiManager = new RoiManager();
		ResultsTable mainTable = ResultsTable.getResultsTable();
		mainTable.show("Results");

		// Call for file folder
		String rootFolderName = IJ.getDirectory("Select raw files folder");
		File rootFolder = new File(rootFolderName);
		
		// Say Hi!
		log.append("____________________________________");
		log.append("Batch FlowMate - starting");
		log.append(new Date().toString());
		log.append("with: "+flowMate);
		log.append("__");
		log.append("Parsing folder "+rootFolder.getName());

		// Create save folder
		File saveFolder = new File(rootFolder, "Results");
		if (!saveFolder.exists() && !saveFolder.mkdir()) {
			log.append("!! Could not create the results folder in "+saveFolder);
			log.append("!! Aborting.");
			return;
		}
		log.append("Saving in "+saveFolder);			
		
		// Get files
		FilenameFilter lifFilesFilter = new FilenameFilter() {			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".lif");
			}
		};
		File[] files = rootFolder.listFiles(lifFilesFilter);
		log.append("Found "+files.length+" lif files");
		log.append("__");

		// Loop over files
		for(File file : files) {
			
			if (file.isDirectory())
				continue;
			log.append("|");
			log.append("+- Opening file "+file.getName());
			
			ImageReader reader = new ImageReader();
			try {
				reader.setId(file.getAbsolutePath());
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			int nseries = reader.getSeriesCount();
			log.append("|  Found "+nseries+" series in file");
			
			// Loop over series
			for (int i = 0; i < nseries; i++) {
				final int seriesIndex = i + 1;

				log.append("|  |");
				log.append("|  +- Reading series number "+seriesIndex+" of "+nseries);

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
					log.append("      Roi file not found for file "+file.getName()+" series "+seriesIndex+", skipping");
				} else {
										
					// Empty roi manager
					roiManager.getROIs().clear();
					roiManager.getList().removeAll();

					// Loading zip file
					log.append("|  |  Loading roi file "+roiFile.getName());
					roiManager.runCommand("Open", roiFile.getAbsolutePath());

					log.append("|  |  Found "+roiManager.getCount()+" ROIs in file");
					@SuppressWarnings("rawtypes")
					Hashtable rois = roiManager.getROIs();
					
					int nRois = rois.size();
					int roiIndex = 0;
					for (Object obj : rois.values()) {
						Roi roi = (Roi) obj;
						roiIndex++;
						
						log.append("|  |  |");
						log.append("|  |  +- Analyzing for roi "+roi.getName()+", "+roiIndex+" of "+nRois);
						imp.setRoi(roi);
						
						String rowName = imp.getShortTitle();
						rowName += "-Series_"+seriesIndex;
						if (null != roi) {
							if (nRois > 1) {
								String roiName = roi.getName() == null ? "-Roi" : roi.getName();
								rowName += "-"+roiName;
							} else {
								rowName += "-Roi";
							}
						}
						
						float npeaksPerFrame = flowMate.process(imp);

						// Save extra data
						
						if (flowMate.isComputeColorFlowImage()) {
							ImagePlus colorFlowImp = flowMate.getColorFlowImage();
							IJ.save(colorFlowImp, saveFolder+File.separator+rowName+"-Flow.tif");
							colorFlowImp.changes = false;
							colorFlowImp.close();
						}
						
						if (flowMate.isDisplayPeaks()) {
							ImagePlus plotImp = flowMate.getPeaks().getImagePlus();
							IJ.save(plotImp, saveFolder+File.separator+rowName+"-Peaks.png");
							plotImp.changes = false;
							plotImp.close();
						}
						
						if (flowMate.isComputeAccuracyImage()) {
							ImagePlus accImp = flowMate.getAccuracyImage();
							IJ.save(accImp, saveFolder+File.separator+rowName+"-Accuracy.tif");
							accImp.changes = false;
							accImp.close();
						}

						if (flowMate.isComputeFlowImages()) {
							List<ImagePlus> flowImps = flowMate.getFlowImage();
							for (int j = 0; j < flowImps.size(); j++) {
								ImagePlus flowImp = flowImps.get(j);
								String fileName = saveFolder+File.separator+rowName+"-Flow-"+(char)('X'+j)+".tif";
								IJ.save(flowImp, fileName);
								flowImp.changes = false;
								flowImp.close();
							}
						}
						
						mainTable.incrementCounter();
						mainTable.addLabel(rowName);
						mainTable.addValue(FlowMate_.PEAK_NUMBER_COLUMN_NAME, npeaksPerFrame);
						mainTable.show("Results");
						
						log.append("|  |  |  Done "+new Date().toString());

					} // Loop over Rois
					
				} // If we have Rois 
				
				for (ImagePlus tmp : imps) { 
					tmp.changes = false;
					tmp.close();
				}
				
			} // Loop over series
			
		} // Loop over files

		try {
			log.append("Saving results table in "+(saveFolder+File.separator+"Results.xls"));
			mainTable.saveAs(saveFolder+"Results.xls");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.append("Batch FlowMate - finishing");
		log.append(new Date().toString());
		log.append("____________________________________");

	}
	
	
	public static void main(String[] args) {
		ij.ImageJ.main(args);
		new Batch_Flow_Mate().run(null);
	}

}
