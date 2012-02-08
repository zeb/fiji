package fiji.plugin.cwnt.segmentation;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.labeling.Labeling;

public class CWNTFrameSegmenter extends MultiThreadedBenchmarkAlgorithm {

	private static final boolean DEBUG = true;
	private CWNTPanel source;
	private ImagePlus imp;

	/*
	 * CONSTRUCTOR
	 */
	
	public CWNTFrameSegmenter(CWNTPanel panel) {
		super();
		this.source = panel;
		this.imp = panel.getTargetImagePlus();
	}
	
	
	/*
	 * METHODS
	 */
	
	@Override
	public boolean checkInput() {
		return true;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean process() {
		
		final long start = System.currentTimeMillis();
		final int frame = imp.getFrame();
		final Settings settings = new Settings(imp);
		final Image img = TMUtils.getSingleFrameAsImage(imp, frame-1, settings.segmentationChannel, settings);
		final CrownWearingSegmenter cws = new CrownWearingSegmenter();
		cws.setTarget(img, settings.getCalibration(), source.getSegmenterSettings());
		
		if (!cws.process()) {
			errorMessage = cws.getErrorMessage();
			return false;
		}
		
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		
		if (DEBUG) {
			System.out.println("["+this.getClass().getSimpleName()+"] #process: coloring label started");
		}
		
		Labeling labels = cws.getLabeling();
		LabelToGlasbey converter = new LabelToGlasbey(labels);
		converter.process();
		ImagePlus result = converter.getImp();
		result.show();

		if (DEBUG) {
			System.out.println("["+this.getClass().getSimpleName()+"] #process: coloring label done");
		}
		
		int tmin = (int) Math.ceil(processingTime / 1e3 / 60); //min 
		source.labelDurationEstimate.setText("Total duration rough estimate: "+tmin+" min.");
		
		return true;
	}

}
