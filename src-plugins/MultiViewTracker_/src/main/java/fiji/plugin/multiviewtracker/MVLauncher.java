package fiji.plugin.multiviewtracker;

import fiji.plugin.trackmate.Logger;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

import loci.formats.FormatException;
import loci.plugins.LociImporter;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.Importer;
import loci.plugins.in.ImporterOptions;

public class MVLauncher implements PlugIn {

	public MVLauncher() {}

	@Override
	public void run(String arg) {
		
		File folder = null;
		if (null != arg ) {
			folder = new File(arg);
		}
		
		File file = askForFile(folder, null, Logger.IJ_LOGGER);
		ImagePlus[] imps;
		try {
			imps = openSPIM(file, true);
			for (int i = 0; i < imps.length; i++) {
				Logger.IJ_LOGGER.log(imps[i].toString());
				imps[i].show();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	
	/*
	 * STATIC METHODS
	 */
	
	private static ImagePlus[] openSPIM(File path, boolean useVirtual) throws IOException, FormatException {
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
		
		ImagePlus[] imps = importer.readPixels(reader, options, displayHandler);
		importer.finish(process);
		return imps;
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
	
	public static void main(String[] args) {
		ImageJ.main(args);
		String rootFolder = "E:/Users/JeanYves/Documents/Projects/PTomancak/Data";
		new MVLauncher().run(rootFolder);
	}
}
