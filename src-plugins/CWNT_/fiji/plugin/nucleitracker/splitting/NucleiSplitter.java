package fiji.plugin.nucleitracker.splitting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.labeling.Labeling;
import mpicbg.imglib.labeling.LabelingType;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.label.FakeType;
import mpicbg.imglib.util.Util;

import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

public class NucleiSplitter extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<LabelingType<Integer>> {

	private static final boolean DEBUG = false;


	/** The labelled image contained the nuclei to split. */
	private Labeling<Integer> source;

	/** Nuclei volume top threshold. Nuclei with a volume larger than this threshold will
	 * be discarded and not considered for splitting. */
	private long volumeThresholdUp = 1000; // Bhavna code
	/** Nuclei volume bottom threshold. Nuclei with a volume smaller than this threshold will
	 * be discarded and not considered for splitting. */
	private long volumeThresholdBottom = 0; // Bhavna code
	/** Volume selectivity: nuclei with a volume larger than mean + this factor times the standard 
	 * deviation will be considered for splitting.	 */
	private double stdFactor = 0.5;

	private ArrayList<Integer> nucleiToSplit;

	private ArrayList<Integer> thrashedLabels;

	private Labeling<Integer> target;

	private Integer nextAvailableLabel;

	/*
	 * CONSTRUCTOR
	 */


	public NucleiSplitter(Labeling<Integer> source) {
		super();
		this.source = source;
		this.target = source.createNewLabeling("Splitted "+source.getName());
	}

	/*
	 * METHODS
	 */

	@Override
	public Labeling<Integer> getResult() {
		return target;
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		
		if (DEBUG) {
			System.out.println("[NucleiSplitter] Starting nuclei splitting with calibration: "+Util.printCoordinates(source.getCalibration()));
		}
		
		long start = System.currentTimeMillis();

		final long volumeEstimate = getVolumeEstimate();

		Thread[] threads = new Thread[numThreads];
		final AtomicInteger ai = new AtomicInteger();

		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("Nuclei splitter thread "+i) {
				public void run() {
					long volume;
					int targetNucleiNumber;
					Integer label;
					for (int j = ai.getAndIncrement(); j < nucleiToSplit.size(); j = ai.getAndIncrement()) {
						label = nucleiToSplit.get(j);
						volume = source.getArea(label);
						targetNucleiNumber = (int) (volume / volumeEstimate);
						if (targetNucleiNumber > 1) {
							split(label, targetNucleiNumber);
						}
					}
				}
			};
		}
		SimpleMultiThreading.startAndJoin(threads);
		
		long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}



	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Split the volume in the source image with the given label in the given
	 * number of nuclei. Splitting is made using K-means++ clustering using calibrated
	 * euclidean distance.
	 */
	private void split(Integer label, int n) {

		// Harvest pixel coordinates in a collection of calibrated clusterable points
		final float[] calibration = source.getCalibration();
		int volume = (int) source.getArea(label);
		Collection<CalibratedEuclideanIntegerPoint> pixels = new ArrayList<CalibratedEuclideanIntegerPoint>(volume);
		LocalizableCursor<FakeType> cursor = source.createLocalizableLabelCursor(label);
		while(cursor.hasNext()) {
			cursor.fwd();
			int[] position = cursor.getPosition();
			pixels.add(new CalibratedEuclideanIntegerPoint(position, calibration));
		}
		cursor.close();

		// Do K-means++ clustering
		KMeansPlusPlusClusterer<CalibratedEuclideanIntegerPoint> clusterer = new KMeansPlusPlusClusterer<CalibratedEuclideanIntegerPoint>(new Random());
		List<Cluster<CalibratedEuclideanIntegerPoint>> clusters = clusterer.cluster(pixels, n, -1);

		// Update source image labels
		LocalizableByDimCursor<LabelingType<Integer>> tc = target.createLocalizableByDimCursor();
		for (Cluster<CalibratedEuclideanIntegerPoint> cluster : clusters) {
			for(CalibratedEuclideanIntegerPoint point : cluster.getPoints()) {
				tc.setPosition(point.getPoint());
				tc.getType().setLabel(nextAvailableLabel);
			}
			nextAvailableLabel = nextAvailableLabel + 1;
		}
		tc.close();
	}

	/**
	 * Get an estimate of the actual single nuclei volume, to use in subsequent steps, when
	 * splitting touching nuclei. 
	 * <p>
	 * Executing this methods also sets the following fields:
	 * <ul>
	 *  	<li> {@link #thrashedLabels} the list of labels that should be erased from the source
	 *  labeling. It contains the label of nuclei that are too big or too small to be enve considered
	 *  for splitting.
	 *  	<li> {@link #nucleiToSplit} the list of labels for the nuclei that should be considered
	 *  for splitting. They are between acceptable bounds, but have a volume too large compared to the
	 *  computed estimate to be made of a single nucleus.
	 *  </ul>
	 * @return  the best volume estimate, as a long primitive 
	 */
	private long getVolumeEstimate() {

		ArrayList<Integer> labels = new ArrayList<Integer>(source.getLabels());

		// Discard nuclei too big or too small;
		thrashedLabels = new ArrayList<Integer>(labels.size()/10);
		long volume;
		for (Integer label : labels) {
			volume = source.getArea(label);
			if (volume >= volumeThresholdUp || volume <= volumeThresholdBottom) {
				thrashedLabels.add(label);
			}
		}
		if (DEBUG) {
			System.out.println("[NucleiSplitter] Removing "+thrashedLabels.size()+" bad nuclei out of "+labels.size());
		}
		labels.removeAll(thrashedLabels);
		int nNuclei = labels.size();


		// Compute mean and std of volume distribution 
		long sum = 0;
		long sum_sqr = 0;
		long v;
		for (Integer label : labels) {
			v = source.getArea(label);
			sum += v;
			sum_sqr += v * v;
		}
		long mean = sum / nNuclei;
		long std = (long) Math.sqrt((sum_sqr - sum*mean)/nNuclei);

		// Harvest suspicious nuclei
		nucleiToSplit = new ArrayList<Integer>(nNuclei/5);
		long splitThreshold = (long) (mean + stdFactor * std);
		for (Integer label : labels) {
			volume = source.getArea(label);
			if (volume >= splitThreshold) {
				nucleiToSplit.add(label);
			}
		}

		// Build non-suspicious nuclei list
		final ArrayList<Integer> nonSuspiciousNuclei = new ArrayList<Integer>(labels);
		nonSuspiciousNuclei.removeAll(nucleiToSplit);
		if (DEBUG) {
			System.out.println("[NucleiSplitter] Found "+nucleiToSplit.size()+" nuclei to split out of "+labels.size());
		}

		// Copy non-suspicious labeling to target image
		Thread[] threads = new Thread[numThreads];
		final AtomicInteger ai = new AtomicInteger();
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("Nuclei splitter thread "+i) {
				public void run() {
					LocalizableByDimCursor<LabelingType<Integer>> targetCursor = target.createLocalizableByDimCursor();
					Integer goodLabel;
					for(int j = ai.getAndIncrement(); j < nonSuspiciousNuclei.size(); j = ai.getAndIncrement()) {
						goodLabel = nonSuspiciousNuclei.get(j);
						LocalizableCursor<FakeType> cursor = source.createLocalizableLabelCursor(goodLabel);
						while (cursor.hasNext()) {
							cursor.fwd();
							targetCursor.setPosition(cursor);
							targetCursor.getType().setLabel(goodLabel);
						}
						cursor.close();
					}
					targetCursor.close();
				}
			};
		}
		SimpleMultiThreading.startAndJoin(threads);

		// Prepare and determine next available label
		TreeSet<Integer> sorted = new TreeSet<Integer>(nonSuspiciousNuclei);
		nextAvailableLabel = sorted.last() + 1;

		// Estimate most probable nucleus volume from non-suspicious nuclei
		long[] volumes = new long[nonSuspiciousNuclei.size()];
		int index = 0;
		for (Integer label : nonSuspiciousNuclei) {
			volumes[index] = source.getArea(label);
			index++;
		}
		long volumeEstimate = Util.computeMedian(volumes);
		if (DEBUG) {
			System.out.println("[NucleiSplitter] Single nucleus volume estimate: "+volumeEstimate+" voxels");
		}

		return volumeEstimate;
	}


}
