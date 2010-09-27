package core.function;

import core.exception.NotGrayScaleException;
import core.image.IJImage;
import core.image.Image;
import core.image.ImageType;
import core.image.iterator.Iterator;
import core.util.IConstants;

public class Histogram {

	private Image image;
	private int[] intensities;
	private int minIntensity;
	private int maxIntensity;

	public Histogram(Image image) throws NotGrayScaleException {
		if (!isGrayScale(image)) {
			throw new NotGrayScaleException();
		}
		this.image = image;
		intensities = new int[IConstants.TWO_POWER_16];
		minIntensity = Integer.MAX_VALUE;
		maxIntensity = Integer.MIN_VALUE;
		generateHistogram();
	}

	private boolean isGrayScale(Image image) {
		boolean isGrayScale = true;
		if (image.getImageType() == ImageType.IJ) {
			isGrayScale = ((IJImage) image).getDataSize() != 24;
		}
		return isGrayScale;
	}

	private void generateHistogram() {
		int intensity;
		Iterator itr = image.getIterator();
		while (itr.hasNext()) {
			intensity = itr.get();
			intensities[intensity]++;
			updateMinIntensity(intensity);
			updateMaxIntensity(intensity);
			itr.next();
		}
	}

	private void updateMaxIntensity(int intensity) {
		if (intensity > maxIntensity) {
			maxIntensity = intensity;
		}
	}

	private void updateMinIntensity(int intensity) {
		if (intensity < minIntensity) {
			minIntensity = intensity;
		}
	}

	public int getMinIntensity() {
		return minIntensity;
	}

	public int getMaxIntensity() {
		return maxIntensity;
	}

	public int getCount(int intensity) {
		return intensities[intensity];
	}

}
