import core.function.EGVD;
import core.function.param.EGVDParams;
import core.function.param.ThresholderParams;

import core.image.IJImage;

import core.progress.ProgressEvent;
import core.progress.ProgressListener;

import core.progress.event.EGVDProgressEvent;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;

import ij.plugin.PlugIn;

/**
 * Evolving generalized Voronoi diagrams for accurate cellular image segmentation.
 *
 * See
 * Yu W, Lee HK, Hariharan S, Bu W, Ahmed S.,
 * Evolving generalized Voronoi diagrams for accurate cellular image segmentation.
 * Cytometry A. 2010 Apr;77(4):379-86.
 */
public class EGVD_ implements PlugIn, ProgressListener {
	public void run(String arg) {
		GenericDialogPlus gd = new GenericDialogPlus("Evolving generalized Voronoi diagrams");
		try {
			gd.addImageChoice("Cell_image", null);
			gd.addImageChoice("Seed_image", null);
		} catch (NullPointerException e) {
			IJ.error("No images open");
			return;
		}
		gd.addNumericField("Cell_threshold_min", 0, 0);
		gd.addNumericField("Cell_threshold_max", 255, 0);
		gd.addNumericField("Cell_threshold_levels", 1, 0);
		gd.addNumericField("Seed_threshold", 128, 0);
		gd.addNumericField("Particle_size", 100, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		EGVDParams params = new EGVDParams();
		params.cellImage = new IJImage(gd.getNextImage());
		params.seedImage = new IJImage(gd.getNextImage());
		params.cellThresholdParams = new ThresholderParams();
		params.cellThresholdParams.min = (int)gd.getNextNumber();
		params.cellThresholdParams.max = (int)gd.getNextNumber();
		params.cellThresholdParams.thresholdLevels = (int)gd.getNextNumber();
		params.seedThreshold = (int)gd.getNextNumber();
		params.particleSize = (int)gd.getNextNumber();

		EGVD egvd = new EGVD(params);
		egvd.addProgressListener(this);
		try {
			egvd.doEGVD();
		} catch (Exception e) {
			//IJ.error("Error occurred: " + e);
			IJ.handleException(e);
			return;
		}
		new IJImage("GVD", egvd.getGVDLines()).show();
		new IJImage("Region image", egvd.getRegionImage()).show();
	}

	public void progressOccurred(ProgressEvent e) {
		EGVDProgressEvent event = (EGVDProgressEvent)e;
		IJ.showStatus("Computing level " + event.currentLevel);
		IJ.showProgress(event.currentLevel, event.totalLevels);
	}
}