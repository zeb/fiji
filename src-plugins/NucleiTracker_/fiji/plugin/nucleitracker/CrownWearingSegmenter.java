package fiji.plugin.nucleitracker;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.IntegerType;

public class CrownWearingSegmenter<T extends IntegerType<T>>  extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<BitType> {
	private Image<T> masked;
	private Image<T> source;
	private Image<BitType> thresholded;

	/*
	 * CONSTRUCTOR	
	 */


	public CrownWearingSegmenter(final Image<T> source) {
		super();
		this.source = source;
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();

		// Crown wearing mask
		NucleiMasker<T> masker = new NucleiMasker<T>(source);
		masker.setNumThreads(numThreads);
		boolean check = masker.process();
		if (check) {
			masked = masker.getResult();
		} else {
			errorMessage = masker.getErrorMessage();
			return false;
		}
		
		// Thresholding
		OtsuThresholder2D<T> thresholder = new OtsuThresholder2D<T>(masked);
		thresholder.setNumThreads(numThreads);
		check = thresholder.process();
		if (check) {
			thresholded = thresholder.getResult();
		} else {
			errorMessage = thresholder.getErrorMessage();
			return false;
		}
		
		processingTime = System.currentTimeMillis() - start;
		return true;
	}

	@Override
	public Image<BitType> getResult() {
		return thresholded;
	}


}
