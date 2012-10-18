package fiji.plugin.multiviewtracker;

import fiji.plugin.multiviewtracker.util.TransformUtils;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import loci.formats.FilePattern;
import loci.formats.FormatException;
import loci.plugins.LociImporter;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.Importer;
import loci.plugins.in.ImporterOptions;

public class MVLauncher <T extends RealType<T> & NativeType<T>> implements PlugIn {

	private static final String ANGLE_STRING = "Angle";
	private static final String REGISTRATION_SUBFOLDER = "registration";
	private static final String REGISTRATION_FILE_SUFFIX = ".registration";


	public MVLauncher() {}

	@Override
	public void run(String arg) {
		
		File folder = null;
		if (null != arg ) {
			folder = new File(arg);
		}
		
		Logger logger = Logger.IJ_LOGGER;
		
		File file = askForFile(folder, null, logger);
		Map<ImagePlus, AffineTransform3D> impMap;

		try {
			
			// Load all the data
			impMap = openSPIM(file, true, Logger.IJ_LOGGER);
			
			// Instantiate model
			ImagePlus imp1 = impMap.keySet().iterator().next();
			Settings<T> settings = new Settings<T>(imp1);
			TrackMate_<T> tm = new TrackMate_<T>(settings);
			TrackMateModel<T> model = tm.getModel();

			// Initialize viewer
			logger.log("Instantiating viewer.\n");
			MultiViewDisplayer<T> viewer = new MultiViewDisplayer<T>(impMap.keySet(), impMap, model);
			logger.log("Rendering viewer.\n");
			viewer.render();
			logger.log("Done.\n");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}

	}

	
	
	/*
	 * STATIC METHODS
	 */
	
	private static Map<ImagePlus, AffineTransform3D> openSPIM(File path, boolean useVirtual, final Logger logger) throws IOException, FormatException {
		LociImporter plugin = new LociImporter();
		Importer importer = new Importer(plugin);
		
		
		ImporterOptions  options = importer.parseOptions("");
		
		options.setId(path.getAbsolutePath());
		options.setGroupFiles(true);
		options.setVirtual(useVirtual);
		options.setSplitChannels(true);
		options.setQuiet(true);
		
		ImportProcess process = new ImportProcess(options);
		ImagePlusReader reader = new ImagePlusReader(process);
		DisplayHandler displayHandler = new DisplayHandler(process);

		process.execute();
		
		// Get the ImagePluses - 1 per channel (or angle)
		ImagePlus[] imps = importer.readPixels(reader, options, displayHandler);
		int nImps = imps.length;
		
		// Get the file names used to build these imps
		String[] fileNames = process.getVirtualReader().getUsedFiles();
		int nFileNames = fileNames.length;
		
		logger.log("Found "+nFileNames+" files grouped in "+nImps+" views.\n");
		
		final FilePattern fp 		= process.getFileStitcher().getFilePattern();
		final String[][] elements 	= fp.getElements();
		final String[] blocks 		= fp.getBlocks();
		final String[] prefixes 	= fp.getPrefixes();
		final String suffix 		= fp.getSuffix();

		importer.finish(process);

		// Try to identify the angle block
		int angleBlockIndex = -1;
		for (int i = 0; i < blocks.length; i++) {
			if (prefixes[i].contains(ANGLE_STRING)) {
				angleBlockIndex = i;
				break;
			}
		}
		if (angleBlockIndex < 0) {
			logger.error("Cound not identify the angle string in filenames.\n");
			return null;
		}
		
		int nAngles = elements[angleBlockIndex].length;
		logger.log("Found " + nAngles + " views from the filenames.\n");
		if (nAngles == nImps) {
			logger.log("Have " + nImps + " matching ImagePlus, so everything is fine.\n");
		} else {
			logger.error("But have " + nImps + " ImagePlus. Something is wrong.\n");
			return null;
		}
		
		logger.log("Owing to the fact that the file pattern is " + fp.getPattern() + ", the views will be split according to:\n");
		for (int i = 0; i < imps.length; i++) {
			logger.log(" - view " + i + ": Angle "+elements[angleBlockIndex][i]+"\n");
		}

		/*
		 *  Retrieve registration file
		 */
		
		// Build the first file names for every view.
		logger.log("Identified the following files as first file for each view:\n");
		String[] viewFirstFiles = new String[nAngles];
		for (int i = 0; i < nAngles; i++) {
			StringBuilder str = new StringBuilder();
			for (int j = 0; j < prefixes.length; j++) {
				str.append(prefixes[j]);
				if (j == angleBlockIndex) {
					str.append(elements[angleBlockIndex][i]);
				} else {
					str.append(elements[j][0]);
				}
			}
			str.append(suffix);
			viewFirstFiles[i] = str.toString();
			logger.log(" - "+str.toString());
		}
		
		// Load transforms
		AffineTransform3D[] transforms = new AffineTransform3D[nAngles];
		logger.log("Locating registration files:\n");
		File registrationFolder = new File(path.getParent(), REGISTRATION_SUBFOLDER);
		int identityTransformIndex = - 1;
		for (int i = 0; i < viewFirstFiles.length; i++) {
			File registrationFile = new File(registrationFolder, viewFirstFiles[i] + REGISTRATION_FILE_SUFFIX);
			String str = " - for view " + i + ", registration file is: " + registrationFile.getName();
			AffineTransform3D transform = TransformUtils.getTransformFromFile(registrationFile);
			// Try to identify the identity transform
			if (TransformUtils.isIdentity(transform)) {
				str += " - is the identity transform.\n";
				identityTransformIndex= i;
			} else {
				str += ".\n";
			}
			logger.log(str);
			AffineTransform3D zscaling = TransformUtils.getZScalingFromFile(registrationFile);
			transform.concatenate(zscaling);
			transforms[i] = transform;
		}
		
		// Modify transforms to match views between themselves
		/* We will bring back everything to pixel coords in the main imp.
		 * Then we need to take these pixel coords to physical coords. So we need
		 * the calibration transform of the main imp.    */ 
		AffineTransform3D calib1 = TransformUtils.getTransformFromCalibration(imps[identityTransformIndex]);
		/* But the SPIM transforms already have a scaling for the Z-factor. So we do not
		 * need to apply it twice. Since the SPIM transform deals with isotropic pixel 
		 * calibration, we need our physical coords transform be isotropic too. */
		calib1.set(calib1.get(0, 0), 2, 2);
		
		for (int i = 0; i < transforms.length; i++) {
			AffineTransform3D transform = transforms[i];
			transform .preConcatenate(calib1.inverse()); // To have real physical coordinates in 1st referential
			transform = transform.inverse();
			transforms[i] = transform;
		}

		// Store in a map
		Map<ImagePlus, AffineTransform3D> impMap = new HashMap<ImagePlus, AffineTransform3D>(nAngles);
		for (int i = 0; i < nAngles; i++) {
			impMap.put(imps[i], transforms[i]);
		}
		return impMap;
	}
	
	private static final File askForFile(File file, Frame parent, Logger logger) {

		if(IJ.isMacintosh()) {
			// use the native file dialog on the mac
			FileDialog dialog =	new FileDialog(parent, "Open a SPIM series", FileDialog.LOAD);
			if (null != file) {
				dialog.setDirectory(file.getParent());
				dialog.setFile(file.getName());
			}
			dialog.setVisible(true);
			String selectedFile = dialog.getFile();
			if (null == selectedFile) {
				logger.log("Load data aborted.\n");
				return null;
			}
			file = new File(dialog.getDirectory(), selectedFile);
		} else {
			JFileChooser fileChooser;
			if (null != file) {
				fileChooser = new JFileChooser(file.getParent());
				fileChooser.setSelectedFile(file);
			} else {
				fileChooser = new JFileChooser();
			}

			int returnVal = fileChooser.showOpenDialog(parent);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			} else {
				logger.log("Load data aborted.\n");
				return null;  	    		
			}
		}
		return file;
	}

	
	/*
	 * MAIN
	 */
	
	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {
		ImageJ.main(args);
		String rootFolder = "E:/Users/JeanYves/Documents/Projects/PTomancak/Data";
		new MVLauncher<T>().run(rootFolder);
	}
}
