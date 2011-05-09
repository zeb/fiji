package fiji.plugin.flowmate;

import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.real.FloatType;

public class NormSquareSummer extends MultiThreadedBenchmarkAlgorithm {
	
	private static final String BASE_ERROR_MESSAGE = "NormSquare: ";
	
	
	private Image<FloatType> X;
	private Image<FloatType> Y;


	private float[] sum;
	private int[] count;
	
	public NormSquareSummer(final Image<FloatType> X, final Image<FloatType> Y) {
		this.X = X;
		this.Y = Y;
	}
		
	@Override
	public boolean checkInput() {
		if (X == null) {
			errorMessage = BASE_ERROR_MESSAGE + "X is null.";
			return false;
		}
		if (Y == null) {
			errorMessage = BASE_ERROR_MESSAGE + "Y is null.";
			return false;
		}
		if (X.getNumDimensions() != 3) {
			errorMessage = BASE_ERROR_MESSAGE + "X must be a 3D image (2D over time).";
			return false;
		}
		if (Y.getNumDimensions() != 3) {
			errorMessage = BASE_ERROR_MESSAGE + "Y must be a 3D image (2D over time).";
			return false;
		}
		for(int i = 0; i < X.getNumDimensions(); i++ ) {
			if (X.getDimension(i) != Y.getDimension(i)) {
				errorMessage = BASE_ERROR_MESSAGE + "X and Y have different size.";
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean process() {
		final long startTime = System.currentTimeMillis();
		count = new int[X.getDimension(2)];
		sum = new float[X.getDimension(2)];
		
		// Create one thread per frame. A lot, but let us try
		final int nSlices = X.getDimension(2); // 2D case: time is 3rd dimension, which is nbr 2 
		final Thread[] threads = SimpleMultiThreading.newThreads( nSlices );
		
		final AtomicInteger ai = new AtomicInteger(0);					
    	for (int ithread = 0; ithread < threads.length; ++ithread) {

    		// Build Thread array
    		threads[ithread] = new Thread(new Runnable() {
    			public void run() {
    				final int currentSlice = ai.getAndIncrement();
    				processOneSlice(currentSlice);
    			}
    		});
    	}
    	SimpleMultiThreading.startAndJoin(threads);
    	processingTime += System.currentTimeMillis() - startTime;
		return true;
	}
	
	/**
	 * Return the square norm calculated at each frame from the 2 images given.
	 * <p>
	 * The return value is a float array, for which each element is the sum of x²+y² over
	 * all non-float pixels of the corresponding frame. 
	 * @see #getCount()
	 */
	public float[] getSquareNorm() {
		return sum;
	}

	/**
	 * Return the number of pixels taken into account in calculating the sum of the squared norm,
	 * at each frame.
	 * @see NormSquareSummer #getSquareNorm()
	 */
	public int[] getCount() {
		return count;
	}
	
	/**
	 * Return the mean square norm calculated at each frame from the 2 images given.
	 * <p>
	 * The return value is a float array, for which each element is the mean of x²+y² over
	 * all non-float pixels of the corresponding frame. 
	 * @see #getCount()
	 * @see #getSquareNorm()
	 */
	public float[] getMeanSquareNorm() {
		float[] msn = new float[sum.length];
		for (int i = 0; i < msn.length; i++) 
			msn[i] = sum[i] / count[i];
		return msn;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	
	private void processOneSlice(final int slice) {
		LocalizableByDimCursor<FloatType> cx = X.createLocalizableByDimCursor();
		LocalizableByDimCursor<FloatType> cy = Y.createLocalizableByDimCursor();
		cx.setPosition(slice, 2);
		cy.setPosition(slice, 2);
		
		float tsum = 0;
		int tcount = 0;
		float fx, fy;
		for (int i = 0; i < X.getDimension(0); i++) {
			cx.setPosition(i, 0);
			cy.setPosition(i, 0);
			
			for (int j = 0; j < X.getDimension(1); j++) {
				cx.setPosition(i, 1);
				cy.setPosition(i, 1);

				fx = cx.getType().get();
				fy = cy.getType().get();
				if (Float.isNaN(fx) || Float.isNaN(fy))
					continue;

				tsum += fx*fx + fy*fy;
				tcount++;
			}
		}

		cx.close();
		cy.close();
		this.sum[slice] = tsum;
		this.count[slice] = tcount;

	}

}
