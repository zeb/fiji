package fiji.plugin.cwnt.segmentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import mpicbg.imglib.algorithm.BenchmarkAlgorithm;
import mpicbg.imglib.algorithm.labeling.AllConnectedComponents;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.planar.PlanarContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.labeling.Labeling;
import mpicbg.imglib.labeling.LabelingType;
import mpicbg.imglib.type.label.FakeType;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.util.Util;

import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class NucleiSplitter extends BenchmarkAlgorithm {

	private static final boolean DEBUG = false;

	/** The labelled image contained the nuclei to split. */
	private final Labeling<Integer> source;
	/** The label generator used to create new labels */
	private final Iterator<Integer> labelGenerator;

	/**
	 * Nuclei volume top threshold. Nuclei with a volume larger than this
	 * threshold will be discarded and not considered for splitting.
	 */
	private long volumeThresholdUp = 1000; // Bhavna code
	/**
	 * Nuclei volume bottom threshold. Nuclei with a volume smaller than this
	 * threshold will be discarded and not considered for splitting.
	 */
	private long volumeThresholdBottom = 0; // Bhavna code
	/**
	 * Volume selectivity: nuclei with a volume larger than mean + this factor
	 * times the standard deviation will be considered for splitting.
	 */
	private double stdFactor = 0.5;
	private ArrayList<Integer> nucleiToSplit;
	private ArrayList<Integer> thrashedLabels;
	private final List<Spot> spots;
	private final float[] calibration;
	/** The physical volume of a voxel. */
	private final float voxelVolume;
	/** If true, will modify the label image to reflect nuclei splitting process. */
	private final boolean doSplitLabelImage;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Instantiate a nuclei splitter with the parameters described below. The splitting scheme
	 * is following Bhavna Rajasekaran idea:
	 * <ul>
	 *   <li> All the nuclei volumes are calculated.
	 *   <li> Nuclei with a volume far too lare or small are discarded (larger than {@link #volumeThresholdUp}
	 *   or smaller than {@link #volumeThresholdBottom}).
	 *   <li> The mean and std of remaining volumes are calculated.
	 *   <li> Nuclei with a volume larger than <code>mean + {@link #stdFactor} × std</code> are flagged for
	 *   splitting. The other ones are kept as is.
	 *   <li> Each flagged nuclei is inspected: 3 histograms of pixels position in X, Y and Z are generated. 
	 *   <li> Each of these histograms is searched for local maxima. 
	 *   <li> A best guess splitting number is derived as being the <u>product</u> of the 3 numbers of maxima in the
	 *   3 histograms.  
	 *   <li> The target nucleus is split in that much sub-nuclei, using {@link KMeansPlusPlusClusterer}.
	 * </ul>
	 * 
	 * 
	 * @param source  a reference to the labeling to operatate on. 
	 * @param calibration  the physical calibration array: this is important to get the clustering right, since 
	 * it is based on physical distance.
	 * @param labelGenerator  the label generator, that will be used to generate new labels after splitting. 
	 * It should be the one that was used to generate the {@link #source} labeling.
	 * @param splitLabelImage  if true, the spliting process will modify the given labeling with new labels
	 * reflecting split objects.
	 * 
	 * @author Bhavna Rajasekarann, Jean-Yves Tinevez 
	 */
	public NucleiSplitter(Labeling<Integer> source, float[] calibration, Iterator<Integer> labelGenerator, boolean splitLabelImage) {
		super();
		this.source = source;
		this.calibration = calibration;
		this.labelGenerator = labelGenerator;
		this.doSplitLabelImage = splitLabelImage;
		this.spots = new ArrayList<Spot>((int) 1.5 * source.getLabels().size());
		this.voxelVolume = calibration[0] * calibration[1] * calibration[2];
	}

	/*
	 * METHODS
	 */

	public List<Spot> getResult() {
		return spots;
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {

		long start = System.currentTimeMillis();

		getVolumeEstimate(); // Harvest non-suspicious spots
		if (DEBUG) {
			System.out.println("[NucleiSplitter] Starting splitting");
		}
		
		int targetNucleiNumber;
		for (int label : nucleiToSplit) {

			targetNucleiNumber = estimateNucleiNumber(label);

			if (targetNucleiNumber > 1) {
				split(label, targetNucleiNumber);
			} else {
				spots.add(createSpotFomLabel(label));
			}
		}

		if (DEBUG) {
			System.out.println("[NucleiSplitter] Splitting done");
		}
				
		long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Instantiate a new {@link Spot} from a blob in the {@link #source} labeling.
	 * @param label the blob label to operate on
	 * @return a new spot object, representing the labeled blob
	 */
	private Spot createSpotFomLabel(Integer label) {
		double nucleusVol = source.getArea(label) * voxelVolume;
		float radius = (float) Math.pow(3 * nucleusVol / (4 * Math.PI), 0.33333);
		float[] coordinates = getCentroid(label);
		Spot spot = new SpotImp(coordinates);
		spot.putFeature(Spot.RADIUS, radius);
		return spot;
	}
	
	/**
	 * Build the histograms of pixel positions for the blob with the given label.
	 * @param label  the target label
	 * @return an array of array of int, one array per dimension
	 */
	private int[][] getPixelPositionHistogramsForLabel(Integer label) {
		// Prepare histogram holders
		int[] minExtents = new int[source.getNumDimensions()];
		int[] maxExtents = new int[source.getNumDimensions()];
		source.getExtents(label, minExtents, maxExtents); 
		
		int[][] histograms = new int[source.getNumDimensions()][];
		for (int i = 0; i < source.getNumDimensions(); i++) {
			histograms[i] = new int[maxExtents[i] - minExtents[i]];
		}
		
		// Build histograms
		int[] position = source.createPositionArray();
		LocalizableCursor<FakeType> cursor = source.createLocalizableLabelCursor(label);
		while(cursor.hasNext()) {
			cursor.fwd();
			cursor.getPosition(position);
			for (int i = 0; i < position.length; i++) {
				histograms[i] [ position[i] - minExtents[i] ] ++;
			}
		}
		cursor.close();
		
		return histograms;
	}

	/**
	 * Estimate the number of nuclei likely to be found in a big blob.
	 * <p>
	 * Here, we apply Bhavna Rajasekaran method: We build the histograms of
	 * pixel position in X, Y and Z, and we assume the most likely number of
	 * nuclei is the sum of peaks in each of these 3 histograms.
	 * 
	 * @param label
	 *            the label of the blob to examine in the {@link #source} image
	 * @return the estimated number of nuclei to split it into
	 */
	private int estimateNucleiNumber(Integer label) {
		
		// Get histograms
		int[][] histograms = getPixelPositionHistogramsForLabel(label);
		
		// Investigate histograms
		int[] nPeaks = new int[source.getNumDimensions()];
		for (int i = 0; i < histograms.length; i++) {
			int[] h = histograms[i];
			boolean wasGoingUp = true;
			nPeaks[i] = 0;
			if (h.length > 1) {
				int previousVal = h[0];
				for (int j = 1; j < h.length; j++) {
					if (h[j] < previousVal && wasGoingUp) { // plateau protection
						// We are going down and were going up, so this is a peak
						nPeaks[i]++;
						wasGoingUp = false;
					} else if (h[j] > previousVal) {
						wasGoingUp = true;
					} else if (h[j] < previousVal){
						wasGoingUp = false;
					}
					previousVal = h[j];
				}
				// Check if last value is a peak as well
				if (h[h.length-1] > h[h.length-2]) {
					nPeaks[i]++;
				}
			}
		}
		int totalPeaks = 1;
		for (int i = 0; i < nPeaks.length; i++) {
			if (nPeaks[i] > 1) {
				totalPeaks *= nPeaks[i];
			}
		}
		
		return totalPeaks;
	}

	/**
	 * Split the volume in the source image with the given label in the given
	 * number of nuclei. Splitting is made using K-means++ clustering using
	 * calibrated euclidean distance.
	 */
	private void split(Integer label, int n) {
		if (label <= 0)
			return; // leave background alone
		
		if (DEBUG) {
			System.out.println("[NucleiSplitter] #split: splitting label "+label+" in "+n);
		}
		
		// Harvest pixel coordinates in a collection of calibrated clusterable points
		int volume = (int) source.getArea(label);
		Collection<CalibratedEuclideanIntegerPoint> pixels = new ArrayList<CalibratedEuclideanIntegerPoint>(volume);
		LocalizableCursor<FakeType> cursor = source.createLocalizableLabelCursor(label);
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] position = cursor.getPosition().clone();
			pixels.add(new CalibratedEuclideanIntegerPoint(position, calibration));
		}
		cursor.close();
		
		// Check bad labels
		if (pixels.size() <= n) {
			Spot spot = createSpotFomLabel(label);
			spot.putFeature(Spot.QUALITY, 0);
			spots.add(spot);
			return;
		}

		// Do K-means++ clustering
		KMeansPlusPlusClusterer<CalibratedEuclideanIntegerPoint> clusterer = 
				new KMeansPlusPlusClusterer<CalibratedEuclideanIntegerPoint>(new Random());
		List<Cluster<CalibratedEuclideanIntegerPoint>> clusters = clusterer.cluster(pixels, n, -1);

		// Create spots from clusters
		for (Cluster<CalibratedEuclideanIntegerPoint> cluster : clusters) {
			float[] centroid = new float[calibration.length];
			int npoints = cluster.getPoints().size();
			for (CalibratedEuclideanIntegerPoint p : cluster.getPoints()) {
				for (int i = 0; i < centroid.length; i++) {
					centroid[i] += (p.getPoint()[i] * calibration[i]) / npoints;
				}
			}
			final double voxelVolume = calibration[0] * calibration[1] * calibration[2];
			double nucleusVol = cluster.getPoints().size() * voxelVolume;
			float radius = (float) Math.pow(3 * nucleusVol / (4 * Math.PI), 0.33333);
			Spot spot = new SpotImp(centroid);
			spot.putFeature(Spot.RADIUS, radius);
			if (DEBUG) {
				System.out.println("[NucleiSplitter] #split: splitting label "+label+", child #"+clusters.indexOf(cluster)+" has a volume of "+nucleusVol);
			}

			spot.putFeature(Spot.QUALITY, (float) 1 / n); // split spot get a quality of 1 over the number of spots in the initial cluster
			spots.add(spot);
		}
		
		if (doSplitLabelImage) {
		
			// Re-label newly split nuclei
			LocalizableByDimCursor<LabelingType<Integer>> sourceCursor = source.createLocalizableByDimCursor();
			for (Cluster<CalibratedEuclideanIntegerPoint> cluster : clusters) {

				int nl = labelGenerator.next();
				List<Integer> newLabel = sourceCursor.getType().intern(nl); 
				if (DEBUG) {
					System.out.println("[NucleiSplitter] #split: relabeling label "+label+", child #"+clusters.indexOf(cluster)+" with new label: " + newLabel);
				}
				for (CalibratedEuclideanIntegerPoint p : cluster.getPoints()) {
					sourceCursor.setPosition(p.getPoint());
					sourceCursor.getType().setLabeling(newLabel);
				}

				if (DEBUG) {
					System.out.println("[NucleiSplitter] #split: relabeling label "+label+", child #"+clusters.indexOf(cluster) + " done");
				}
			}
			sourceCursor.close();
		}
	}

	/**
	 * Get an estimate of the actual single nuclei volume, to use in subsequent
	 * steps, when splitting touching nuclei.
	 * <p>
	 * Executing this methods also sets the following fields:
	 * <ul>
	 * <li> {@link #thrashedLabels} the list of labels that should be erased from
	 * the source labeling. It contains the label of nuclei that are too big or
	 * too small to be even considered for splitting.
	 * <li> {@link #nucleiToSplit} the list of labels for the nuclei that should
	 * be considered for splitting. They are between acceptable bounds, but have
	 * a volume too large compared to the computed estimate to be made of a
	 * single nucleus.
	 * </ul>
	 * 
	 * @return the best volume estimate, as a long primitive
	 */
	private long getVolumeEstimate() {

		ArrayList<Integer> labels = new ArrayList<Integer>(source.getLabels());

		// Discard nuclei too big or too small;
		thrashedLabels = new ArrayList<Integer>(labels.size() / 10);
		long volume;
		for (Integer label : labels) {
			volume = source.getArea(label);
			if (volume >= volumeThresholdUp || volume <= volumeThresholdBottom) {
				thrashedLabels.add(label);
			}
		}
		if (DEBUG) {
			System.out.println("[NucleiSplitter] Removing "	+ thrashedLabels.size() + " bad nuclei out of "	+ labels.size());
		}
		if (doSplitLabelImage) {
			eraseLabelsFromImage(thrashedLabels);
		}
		labels.removeAll(thrashedLabels);
		int nNuclei = labels.size();

		// Compute mean and std of volume distribution
		long sum = 0;
		long sum_sqr = 0;
		long v = source.getArea(0); // pre-compute statistics, otherwise it hangs
		for (Integer label : labels) {
			v = source.getArea(label);
			sum += v;
			sum_sqr += v * v;
		}
		long mean = sum / nNuclei;
		long std = (long) Math.sqrt((sum_sqr - sum * mean) / nNuclei);

		// Harvest suspicious nuclei
		nucleiToSplit = new ArrayList<Integer>(nNuclei / 5);
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
			System.out.println("[NucleiSplitter] Found " + nucleiToSplit.size()+ " nuclei to split out of " + labels.size());
		}

		// Harvest non-suspicious nuclei as spots
		for (Integer label : nonSuspiciousNuclei) {
			Spot spot = createSpotFomLabel(label);
			spot.putFeature(Spot.QUALITY, 1); // non-suspicious spots get a quality of 1
			spots.add(spot);
		}

		long volumeEstimate = mean;
		if (DEBUG) {
			System.out.println("[NucleiSplitter] Single nucleus volume estimate: " + volumeEstimate + " voxels");
		}
		return volumeEstimate;
	}

	/**
	 * Modify the {@link #source} image to physically removed the given list of labels.
	 */
	private void eraseLabelsFromImage(final Iterable<Integer> labels) {
		
		LocalizableByDimCursor<LabelingType<Integer>> tempCursor = source.createLocalizableByDimCursor();
		LabelingType<Integer> t = tempCursor.getType();
        final List<Integer> zero = t.intern(-1); // Make your zero label here.

        final Iterator<Integer> it = labels.iterator();
        LocalizableByDimCursor<LabelingType<Integer>> destCursor = source.createLocalizableByDimCursor();
        while(it.hasNext()) {
        	int label = it.next();
        	LocalizableCursor<FakeType> cursor = source.createLocalizableLabelCursor(label);
        	if (DEBUG) {
        		System.out.println("[NucleiSplitter] #eraseLabelsFromImage: erasing label "+label+" started");
        	}
        	while (cursor.hasNext()) {
        		cursor.fwd();
        		destCursor.setPosition(cursor);
        		destCursor.getType().setLabeling(zero); // Weird, but it seems we have to do that to get a label of 0
        	}
        	cursor.close();
        	if (DEBUG) {
        		System.out.println("[NucleiSplitter] #eraseLabelsFromImage: erasing label "+label+" done");
        	}
        }
        destCursor.close();
        if (DEBUG) {
			System.out.println("[NucleiSplitter] #eraseLabelsFromImage: all done");
		}
	}

	private float[] getCentroid(final Integer label) {
		final float[] centroid = new float[3];
		final int[] position = source.createPositionArray();
		LocalizableCursor<FakeType> cursor = source
				.createLocalizableLabelCursor(label);
		int npixels = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getPosition(position);
			for (int i = 0; i < position.length; i++) {
				centroid[i] += position[i] * calibration[i];
			}
			npixels++;
		}
		for (int i = 0; i < centroid.length; i++) {
			centroid[i] /= npixels;
		}
		return centroid;
	}
	
	/*
	 * MAIN METHOD
	 */
	

	public static void main(String[] args) {
		
		// First we create a blanck image
		int[] dim = new int[] { 50, 40, 40 };
		Image<BitType> img = new ImageFactory<BitType>(new BitType(), new ArrayContainerFactory()).createImage(dim);

		// Then we add 2 blobs, touching in the middle to make 8-like shape.
		float radius = 11;
		float[] center = new float[] { 15, 20, 20 };
		SphereCursor<BitType> cursor = new SphereCursor<BitType>(img, center , radius);
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getType().set(true);
		}
		
		center = new float[] {35, 20, 20 };
		cursor.moveCenterToCoordinates(center);
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getType().set(true);
		}
		cursor.close();
		
		// Labeling
		Iterator<Integer> labelGenerator = AllConnectedComponents.getIntegerNames(0);

		PlanarContainerFactory containerFactory = new PlanarContainerFactory();
		ImageFactory<LabelingType<Integer>> imageFactory = new ImageFactory<LabelingType<Integer>>(new LabelingType<Integer>(), containerFactory);
		Labeling<Integer> labeling = new Labeling<Integer>(imageFactory, img.getDimensions(), "Labels");
		labeling.setCalibration(img.getCalibration());

		// 6-connected structuring element
		int[][] structuringElement = new int[][] { {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1} };
		CrownWearingSegmenter.labelAllConnectedComponents(labeling , img, labelGenerator, structuringElement);
		// Splitter
		Integer label = 0;
		NucleiSplitter splitter = new NucleiSplitter(labeling, img.getCalibration(), labelGenerator, true);
		
		// Prepare histogram holders
		int[] minExtents = new int[labeling.getNumDimensions()];
		int[] maxExtents = new int[labeling.getNumDimensions()];
		labeling.getExtents(label, minExtents, maxExtents);
		System.out.println("Min extend of label "+label+": "+Util.printCoordinates(minExtents));
		System.out.println("Max extend of label "+label+": "+Util.printCoordinates(maxExtents));
		
		// Show histograms
		int[][] histograms = splitter.getPixelPositionHistogramsForLabel(label);
		for (int i = 0; i < histograms.length; i++) {
			System.out.println("For dim "+i+": "+Util.printCoordinates(histograms[i]));
		}
		
		int n = splitter.estimateNucleiNumber(label);
		System.out.println("This one shall be split in "+n);
		
		splitter.split(label, n);
		
		LabelToGlasbey colorer = new LabelToGlasbey(labeling);
		colorer.checkInput();
		colorer.process();
		
		ij.ImageJ.main(args);
		colorer.getImp().show();
		
	}
	

}