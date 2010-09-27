package core.function;

import core.function.param.ThresholderParams;
import core.image.Image;
import core.image.ImageFactory;
import core.image.iterator.Iterator;

public class Thresholder {

	private ThresholderParams params;
	int[] thresholdLevels;
	int position;

	public Thresholder(ThresholderParams params) {
		this.params = params;
		ensureMinLessThanMax();
		createThresholdLevels();
		populateThresholdLevels();
	}

	private void ensureMinLessThanMax() {
		if (params.min > params.max) {
			int temp = params.min;
			params.min = params.max;
			params.max = temp;
		}
	}

	private void createThresholdLevels() {
		if (params.max - params.min + 1 <= params.thresholdLevels) {
			params.thresholdLevels = params.max - params.min + 1;
		}
		thresholdLevels = new int[params.thresholdLevels];
		position = 0;
	}

	public boolean hasNextThreshold() {
		return (position < thresholdLevels.length);
	}

	public Image nextThreshold() {
		Image result = createResultImage();
		doThreshold(result);
		return result;
	}

	private void doThreshold(Image result) {
		Iterator imageItr = params.image.getIterator();
		Iterator resultItr = result.getIterator();
		int max = params.max;
		while (imageItr.hasNext()) {
			if (isWithinThresholdLimits(imageItr.get(),
					thresholdLevels[position], max)) {
				resultItr.set(result.getMaxPossibleValue());
			} else {
				resultItr.set(result.getMinPossibleValue());
			}
			imageItr.next();
			resultItr.next();
		}
		position++;
	}

	private Image createResultImage() {
		Image result = ImageFactory.createBinaryImage();
		result.create(params.image);
		return result;
	}

	private boolean isWithinThresholdLimits(int value, int min, int max) {
		return (min <= value && value <= max);
	}

	private void populateThresholdLevels() {
		int min = params.min;
		int max = params.max;
		thresholdLevels[0] = max;
		thresholdLevels[thresholdLevels.length - 1] = min;
		if (thresholdLevels.length > 1) {
			double thresholdLevel = (double) (max - min + 1)
					/ (thresholdLevels.length - 1);
			for (int i = 1; i < thresholdLevels.length - 1; i++) {
				thresholdLevels[i] = max - (int) (thresholdLevel * i);
			}
		}
	}

	public int getThresholdLevel() {
		return (thresholdLevels[position]);
	}
}
