package core.function;

import core.exception.NotGrayScaleException;
import core.image.Image;

public class OtsuThresholder {

	private Image image;
	private Integer bestThreshold = null;
	private Histogram histogram;
	private int totalIntensity;

	public OtsuThresholder(Image image) {
		this.image = image;
	}

	public int getBestThreshold() throws NotGrayScaleException {
		if (bestThreshold == null) {
			computeBestThreshold();
		}
		return bestThreshold.intValue();
	}

	public double getBetweenClassVariance(int i) throws NotGrayScaleException {
		createHistogram();
		int start = histogram.getMinIntensity();
		int end = i;

		Data background = new Data();
		Data foreground = new Data();

		for (int intensity = start; intensity < end; intensity++) {
			background.increment(histogram.getCount(intensity), intensity);
			foreground.update(background);
		}

		return getBetweenClassVariance(foreground, background);
	}

	private void createHistogram() throws NotGrayScaleException {
		if (histogram == null) {
			histogram = new Histogram(image);
			totalIntensity = getIntensitySummation();
		}
	}

	private void computeBestThreshold() throws NotGrayScaleException {
		createHistogram();
		int start = histogram.getMinIntensity();
		int end = histogram.getMaxIntensity();

		Data background = new Data();
		Data foreground = new Data();

		int threshold = start;
		double maxBetweenClassVariance = 0;

		for (int intensity = start; intensity < end; intensity++) {
			background.increment(histogram.getCount(intensity), intensity);
			foreground.update(background);
			double betweenClassVariance = getBetweenClassVariance(foreground,
					background);
			if (betweenClassVariance > maxBetweenClassVariance) {
				maxBetweenClassVariance = betweenClassVariance;
				threshold = intensity + 1;
			}
		}

		bestThreshold = new Integer(threshold);
	}

	private double getBetweenClassVariance(Data foreground, Data background) {
		return (background.getWeight() * foreground.getWeight()
				* (background.getMean() - foreground.getMean()) * (background
				.getMean() - foreground.getMean()));
	}

	private int getIntensitySummation() {
		int summation = 0;
		int start = histogram.getMinIntensity();
		int end = histogram.getMaxIntensity();
		for (int intensity = start; intensity <= end; intensity++) {
			summation += intensity * histogram.getCount(intensity);
		}
		return summation;
	}

	private class Data {
		public int totalCount;
		public int count;
		public int intensitySummation;

		public Data() {
			this.totalCount = image.getSize();
		}

		public double getWeight() {
			return (double) count / totalCount;
		}

		public double getMean() {
			double mean = 0;
			if (count > 0) {
				mean = (double) intensitySummation / count;
			}
			return mean;
		}

		public void increment(int count, int intensity) {
			this.count += count;
			this.intensitySummation += count * intensity;
		}

		public void update(Data inverseData) {
			count = totalCount - inverseData.count;
			intensitySummation = totalIntensity
					- inverseData.intensitySummation;
		}
	}
}
