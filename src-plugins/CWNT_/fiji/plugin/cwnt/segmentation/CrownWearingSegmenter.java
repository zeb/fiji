package fiji.plugin.cwnt.segmentation;

import ij.IJ;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.algorithm.labeling.AllConnectedComponents;
import mpicbg.imglib.container.planar.PlanarContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.labeling.Labeling;
import mpicbg.imglib.labeling.LabelingType;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.IntegerType;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.util.TMUtils;

public class CrownWearingSegmenter<T extends IntegerType<T>>  extends MultiThreadedBenchmarkAlgorithm implements SpotSegmenter<T> {
	
	private Image<T> masked;
	private Image<T> source;
	private Image<BitType> thresholded;
	private Labeling<Integer> labeling;
	private double[] param;
	private List<Spot> spots;
	private float[] calibration;

	/*
	 * CONSTRUCTOR	
	 */

	public CrownWearingSegmenter() {
		super();
	}
	
	/*
	 * METHODS
	 */
	

	@Override
	public void setImage(Image<T> image) {
		this.source = image;
	}

	@Override
	public void setCalibration(float[] calibration) {
		this.calibration = calibration;
	}

	@Override
	public List<Spot> getResult(Settings settings) {
		return TMUtils.translateSpots(spots, settings);
	}

	@Override
	public Image<T> getIntermediateImage() {
		return masked;
	}

	/**
	 * Not implemented.
	 * @see #setParameters(double[])
	 */
	@Override
	public SegmenterSettings getSettings() {
		return null;
	}

	public void setParameters(double[] param) {
		this.param = param;
	}


	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();
		boolean check;
		
		// Crown wearing mask
		NucleiMasker<T> masker = new NucleiMasker<T>(source);
		masker.setNumThreads(numThreads);
		masker.setParameters(param);
		check = masker.process();
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
		
		// Labeling
		Iterator<Integer> labelGenerator = AllConnectedComponents.getIntegerNames(0);
		
		PlanarContainerFactory containerFactory = new PlanarContainerFactory();
		ImageFactory<LabelingType<Integer>> imageFactory = new ImageFactory<LabelingType<Integer>>(new LabelingType<Integer>(), containerFactory);
		labeling = new Labeling<Integer>(imageFactory, thresholded.getDimensions(), "Labels");
		labeling.setCalibration(source.getCalibration());
		
		// 6-connected structuring element
		int[][] structuringElement = new int[][] { {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1} };
		labelAllConnectedComponents(labeling , thresholded, labelGenerator, structuringElement);
		
		// Splitting and spot creation
		NucleiSplitter splitter = new NucleiSplitter(labeling, calibration);
		if (!(splitter.checkInput() && splitter.process())) {
			IJ.error("Problem with splitter: "+splitter.getErrorMessage());
			return false;
		}
		spots = splitter.getResult();
		
		
		processingTime = System.currentTimeMillis() - start;
		return true;
	}

	@Override
	public List<Spot> getResult() {
		return spots;
	}

	public Labeling<Integer> getLabeling() {
		return labeling;
	}
	
	
	/*
	 * STATIC METHODS
	 */
	

	
	/**
	 * Label all connected components in the given image using an arbitrary
	 * structuring element.
	 * @param <T> the type of the labels to apply
	 * @param labeling Assign labels to this labeling space 
	 * @param img a binary image where true indicates parts of components
	 * @param names supplies names for the different components as needed
	 * @param structuringElement an array of offsets to a pixel of the
	 * pixels which are considered connected. For instance, a 4-connected
	 * structuring element would be "new int [][] {{-1,0},{1,0},{0,-1},{0,1}}".
	 * @throws NoSuchElementException if there are not enough names
	 * @author Lee Klaminsky - I took it from its imglib package to fix a bug in
	 * imglib1.
	 */
	private static final <T extends Comparable<T>> void labelAllConnectedComponents(
			Labeling<T> labeling, 
			Image<BitType> img,	
			Iterator<T> names, 
			int [][] structuringElement) throws NoSuchElementException	{
		
		LocalizableCursor<BitType> c			 	= img.createLocalizableCursor();
		LocalizableByDimCursor<BitType> bc 			= img.createLocalizableByDimCursor();
		LocalizableByDimCursor<LabelingType<T>> destCursor = labeling.createLocalizableByDimCursor();
		
		int [] srcPosition = img.createPositionArray();
		int [] destPosition = img.createPositionArray();
		int [] dimensions = labeling.getDimensions();
		
		PositionStack toDoList = new PositionStack(img.getNumDimensions()); 
		
		for (BitType t:c) {
			
			if (t.get()) {
				
				c.getPosition(srcPosition);
				
				boolean outOfBounds = false;
				for (int i=0; i<dimensions.length; i++) {
					if (srcPosition[i] >= dimensions[i]) {
						outOfBounds = true;
						break;
					}
				}
				
				if (outOfBounds) continue;
				
				destCursor.setPosition(srcPosition);
				
				/*
				 * Assign a label if no label has yet been assigned.
				 */
				if (destCursor.getType().getLabeling().isEmpty()) {
					
					List<T> currentLabel = destCursor.getType().intern(names.next());
					destCursor.getType().setLabeling(currentLabel);
					toDoList.push(srcPosition);
					
					while ( !toDoList.isEmpty() ) {
						
						/*
						 * Find neighbors at the position
						 */
						toDoList.pop(srcPosition);
						
						for (int [] offset:structuringElement) {
							
							outOfBounds = false;
							for (int i=0; i<offset.length; i++) {
								destPosition[i] = srcPosition[i] + offset[i];
								if ((destPosition[i] < 0) || (destPosition[i] >= dimensions[i])){
									outOfBounds = true;
									break;
								}
							}
							if (outOfBounds) continue;
							
							bc.setPosition(destPosition);
							if (bc.getType().get()) { 
								destCursor.setPosition(destPosition);
								
								if (destCursor.getType().getLabeling().isEmpty()) {
									destCursor.getType().setLabeling(currentLabel);
									toDoList.push(destPosition);
								}
								
							}
						}
					}
				}
			}
		}
		
		c.close();
		bc.close();
		destCursor.close();
	}
	
	
	private static final class PositionStack {
		private final int dimensions;
		private int [] storage;
		private int position = 0;
		
		public PositionStack(int dimensions) {
			this.dimensions = dimensions;
			storage = new int [100 * dimensions];
		}
		
		public void push(int [] position) {
			
			int insertPoint = this.position * dimensions;
			
			if (storage.length <= (insertPoint+dimensions)) {
				int [] newStorage = new int [storage.length * 3 / 2];
				System.arraycopy(storage, 0, newStorage, 0, storage.length);
				storage = newStorage;
			
			}
			
			System.arraycopy(position, 0, storage, insertPoint, dimensions);
			this.position++;
		}
		
		public void pop(int [] position) {
			this.position--;
			System.arraycopy(storage, this.position * dimensions, position, 0, dimensions);
		}
		
		public boolean isEmpty() {
			return position == 0;
		}
	}

	
}
