
import java.io.FileNotFoundException;

import java.util.InputMismatchException;
import java.util.Scanner;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.io.FileInfo;
import ij.io.OpenDialog;

import ij.plugin.PlugIn;

import java.io.File;

/**
 * Class for reading Meta Image .mhd files.
 * 
 * Since plugins do not have return values, the only way to let ImageJ handle
 * the display of the resulting window (or not, if we're in batch mode) is to
 * extend ImagePlus.
 */
public class Open_Meta_Image extends ImagePlus implements PlugIn {

	protected int stackSlices = 0;

	/**
	 * This method gets called by ImageJ / Fiji.
	 * 
	 * @param arg
	 *            is supposed to be a path if it is not empty
	 */
	public void run(String path) {

		boolean needToShow = false;

		// get the file
		File file;
		if (path == null || path.equals("")) {

			OpenDialog dialog = new OpenDialog("Open .mhd file", null);
			if (dialog.getDirectory() == null)
				return; // canceled
			file = new File(dialog.getDirectory(), dialog.getFileName());

			/*
			 * Since no path was passed, assume that it was run interactively
			 * rather than from HandleExtraFileTypes
			 */
			needToShow = true;
		} else
			file = new File(path);

		// read the file
		parse(file);

		if (needToShow)
			show();
	}

	private void parse(File file) {

		int[] dimensions     = null;
		double[] position    = null;
		double[] orientation = null;
		double[] voxelSize   = null;
		String name          = null;
		boolean binary       = false;
		double minValue      = 0.0;
		double maxValue      = 255.0;
		int channels         = 1;
		String filePattern   = null; 
		boolean isPattern    = false;
		int minPattern       = 0;
		int maxPattern       = 0;
		int stepPattern      = 1;

		Scanner scanner;
		try {
			scanner = new Scanner(file);
		} catch (FileNotFoundException e) {
			IJ.error("File " + file.getPath() + " not found.");
			return;
		}

		boolean parseOptions = true;

		// parse one token after another
		while (parseOptions && scanner.hasNext()) {

			// we always start with a string telling us which option we are
			// about to read
			try {
				String option = scanner.next();

				if (!scanner.next().equals("=")) {
					IJ.error("Expected '=' after option " + option + " in file " + file.getPath());
					return;
				}

				// comment
				if (option.equals("Comment")) {
					// skip this line
					scanner.nextLine();

				// object type
				} else if (option.equals("ObjectType")) {
					String type = scanner.next();
					if (!type.equals("Image")) {
						IJ.error("This file does not contain an image.");
						return;
					}

				// number of dimensions
				} else if (option.equals("NDims")) {
					dimensions = new int[scanner.nextInt()];
					for (int d = 0; d < dimensions.length; d++)
						dimensions[d] = 1;

				// size of the image
				} else if (option.equals("DimSize")) {
					if (dimensions == null) {
						IJ.error("'NDims' has to be set prior to 'DimSize'");
						return;
					}
					for (int d = 0; d < dimensions.length; d++)
						dimensions[d] = scanner.nextInt();

				// minimum value in the image
				} else if (option.equals("ElementMin")) {
					minValue = scanner.nextDouble();

				// maximum value in the image
				} else if (option.equals("ElementMax")) {
					maxValue = scanner.nextDouble();

				// number of channels
				} else if (option.equals("ElementNumberOfChannels")) {
					channels = scanner.nextInt();

				// the data (file(s))
				} else if (option.equals("ElementDataFile")) {
					filePattern = scanner.next();
					if (filePattern.equals("LIST")) {
						// a list of images is waiting for us on the next lines
						parseOptions = false;

					} else if (scanner.hasNextInt()) {
						isPattern   = true;
						minPattern  = scanner.nextInt();
						maxPattern  = scanner.nextInt();
						if (scanner.hasNextInt())
							stepPattern = scanner.nextInt();
					}

				// name
				} else if (option.equals("Name")) {
					name = scanner.next();

				// is the data binary
				} else if (option.equals("BinaryData")) {
					binary = scanner.nextBoolean();

				// position of image origin
				} else if (option.equals("Position")) {
					if (dimensions == null) {
						IJ.error("'NDims' has to be set prior to 'Position'");
						return;
					}
					position = new double[dimensions.length];
					for (int d = 0; d < dimensions.length; d++)
						position[d] = scanner.nextDouble();

				// voxel sizes
				} else if (option.equals("ElementSize")) {
					if (dimensions == null) {
						IJ.error("'NDims' has to be set prior to 'ElementSpacing'");
						return;
					}
					voxelSize = new double[dimensions.length];
					for (int d = 0; d < dimensions.length; d++)
						voxelSize[d] = scanner.nextDouble();

				// orientation of image axises
				} else if (option.equals("Orientation")) {
					if (dimensions == null) {
						IJ.error("'NDims' has to be set prior to 'Orientation'");
						return;
					}
					orientation = new double[dimensions.length*dimensions.length];
					for (int d = 0; d < dimensions.length*dimensions.length; d++)
						orientation[d] = scanner.nextDouble();

				// unknown
				} else {
					System.out.println("Meta Image Reader: encountered unknown option " + option +
									   " in file " + file.getPath() + ", ignoring it...");
					scanner.nextLine();
				}

			} catch (InputMismatchException e) {
				IJ.error("The input file does not appear to be a valid .mhd file.");
				return;
			}
		}

		int width    = (dimensions.length > 0 ? dimensions[0] : 1);
		int height   = (dimensions.length > 1 ? dimensions[1] : 1);
		int zslices  = (dimensions.length > 2 ? dimensions[2] : 1);
		int frames   = (dimensions.length > 3 ? dimensions[3] : 1);
		int slices   = channels*zslices*frames;

		ImageStack stack = new ImageStack(width, height);

		// read data
		int slicesAdded = 0;
		if (isPattern) {
			// create all filenames
			for (int i = minPattern; i <= maxPattern; i += stepPattern) {
				String filename = file.getParent() + File.separator + String.format(filePattern, i);
				// append all slices from this image to the stack
				slicesAdded += appendToStack(stack, filename);
			}

		} else if (filePattern.equals("LOCAL")) {
			// TODO:
			// read local data
			IJ.error("Sorry, reading of data from within the .mhd file is not implemented yet.");

		} else if (filePattern.equals("LIST")) {
			// scanner didn't finish the file, there are still image filenames
			while (scanner.hasNext()) {
				String filename = file.getParent() + File.separator + scanner.next();
				slicesAdded += appendToStack(stack, filename);
			}

		} else {
			// just a single file
			slicesAdded += appendToStack(stack, file.getParent() + File.separator + filePattern);
		}

		if (slices != slicesAdded) {
			IJ.error("According to the .mhd file, " + slices +
			         " should have been added. However, " + slicesAdded +
			         " could be found in the data files.");
		}

		// now set the contents of the ImagePlus
		setStack(stack);
		setDimensions(channels, zslices, frames);
		if (channels*zslices*frames > 1)
			setOpenAsHyperStack(true);

		setTitle(file.getName());

		// setting the FileInfo is optional
		FileInfo info = new FileInfo();
		info.fileName = file.getAbsolutePath();
		info.width = width;
		info.height = height;
		info.nImages = slices;
		setFileInfo(info);
	}

	private int appendToStack(ImageStack stack, String filename) {

		ImagePlus imp = null;
		try {
			imp = IJ.openImage(filename);
		} catch (Exception e) {
			IJ.log("Error when opening " + filename + ".");
		}
		if (imp == null) {
			IJ.error("Image file " + filename + " could not be opened.");
			return 0;
		}
		ImageStack newStack = imp.getStack();

		if (newStack.getWidth() != stack.getWidth() || newStack.getHeight() != stack.getHeight()) {
			IJ.error("Image " + filename + " does have invalid dimensions.");
			return 0;
		}

		if (stackSlices == 0)
			stackSlices = newStack.getSize();
		else if (stackSlices != newStack.getSize())
			IJ.error("Image " + filename + " does have " + newStack.getSize() + " slices, the other ones had " + stackSlices);

		for (int s = 1; s <= newStack.getSize(); s++)
			stack.addSlice("", newStack.getProcessor(s));

		return newStack.getSize();
	}
}
