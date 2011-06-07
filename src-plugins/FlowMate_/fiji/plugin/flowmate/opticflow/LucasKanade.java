package fiji.plugin.flowmate.opticflow;

import ij.gui.Roi;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.real.FloatType;

public class LucasKanade extends MultiThreadedBenchmarkAlgorithm {

	private static final String BASE_ERROR_MESSAGE = "LucasKanade: ";
	/** Default window size: 2D 3x3. */
	private static final int[] DEFAULT_WINDOW_SIZE = new int[] {3, 3, 1}; 
	/** Default window coeffs: flat average. */ 
	private static final float[] DEFAULT_WINDOW_COEFFS = new float[] { 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f };
	
	
	private List<Image<FloatType>> derivatives;
	private OutOfBoundsStrategyFactory<FloatType> factory= new OutOfBoundsStrategyMirrorFactory<FloatType>();
	private Image<FloatType> ux;
	private Image<FloatType> uy;
	private Image<FloatType> lambda1;
	private Image<FloatType> lambda2;
	/** 
	 * If true, we will accept normal velocities when the LK matrix is rank deficient. If false, 
	 * we will reject it.
	 */
	private boolean acceptNormals = true;
	
	// ROI

	/** The window size, defined for each dimension. */
	private int [] windowSize = DEFAULT_WINDOW_SIZE;
	/** The window coefficients, organized in a linear array. */
	private float[] windowCoeffs = DEFAULT_WINDOW_COEFFS;
	private double threshold;
	private Roi roi;
	private boolean computeAccuracy;

	/*
	 * CONSTRUCTOR
	 */
	
	public LucasKanade(List<Image<FloatType>> derivatives) {
		this(derivatives, null);
	}	

	public LucasKanade(List<Image<FloatType>> derivatives, Roi roi) {
		this.derivatives = derivatives;
		this.roi = roi;
	}	

	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public boolean checkInput() {
		if (null == derivatives) {
			errorMessage = BASE_ERROR_MESSAGE + "Derivatives are null.";
			return false;
		}
		if (derivatives.size() < 2) {
			errorMessage = BASE_ERROR_MESSAGE + "Need at least 2 derivatives, one over space, one over time, got "+derivatives.size()+".";
			return false;
		}
		int ncoeffs = windowSize[0];
		for (int i = 1; i < windowSize.length; i++) 
			ncoeffs = ncoeffs * windowSize[i];
		if (ncoeffs != windowCoeffs.length) {
			errorMessage = BASE_ERROR_MESSAGE + "Window coefficient number does not match window size. Expecting "+ncoeffs+", got "+windowCoeffs.length+".";
			return false;
		}
		
		return true;
	}

	@Override
	public boolean process() {
		final long startTime = System.currentTimeMillis();

		boolean returnValue = false;
		if (derivatives.size() == 3)   {
			
			// Prepare result holders
			final Image<FloatType> Ix = derivatives.get(0);
			ux = Ix.createNewImage();
			uy = Ix.createNewImage();
			ux.setName("Vx");
			uy.setName("Vy");
			if (computeAccuracy) {
				lambda1 = Ix.createNewImage();
				lambda2 = Ix.createNewImage();
				lambda1.setName("Lambda1");
				lambda2.setName("Lambda2");
			}
			
			// Prepare for multi-threading
			final long imageSize = Ix.getNumPixels();
			final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
			final AtomicInteger ai = new AtomicInteger(0);					
			final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
			
			for (int ithread = 0; ithread < threads.length; ++ithread) {
				
				// Build Thread array
				threads[ithread] = new Thread(new Runnable() {

					public void run() {

						// Thread ID
						final int myNumber = ai.getAndIncrement();

						// Get chunk of pixels to process
						final Chunk myChunk = threadChunks.get( myNumber );

						process2D(myChunk.getStartPosition(), myChunk.getLoopSize() );

					}

				});
			}
			
			SimpleMultiThreading.startAndJoin(threads);
						
		}
		
		processingTime = System.currentTimeMillis() - startTime;
		return returnValue;
	}
	
	public List<Image<FloatType>> getResults() {
		List<Image<FloatType>> results = new ArrayList<Image<FloatType>>(derivatives.get(0).getNumDimensions()-1);
		results.add(ux);
		results.add(uy);
		return results;
	}
	
	public Image<FloatType> getAccuracyImage() {
		if (acceptNormals)
			return lambda2;
		else
			return lambda1;
	}
	
	/**
	 * Set the Lucas and Kanade window value and size.
	 * @param coeffs  the value of each coefficient in the window, in a linear array. The float array length
	 * must match the size set in the <code>size</code> parameter.
	 * @param size  the size of the window for each dimension.
	 */
	public void setWindow(final float[] coeffs, final int[] size) {
		this.windowCoeffs = coeffs;
		this.windowSize = size;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private boolean process2D(final long startPos, final long loopSize) {
		
		final Image<FloatType> Ix = derivatives.get(0);
		final Image<FloatType> Iy = derivatives.get(1);
		final Image<FloatType> It = derivatives.get(2);		
		final LocalizableByDimCursor<FloatType> cx = Ix.createLocalizableByDimCursor(factory);
		final LocalizableByDimCursor<FloatType> cy = Iy.createLocalizableByDimCursor(factory);
		final LocalizableByDimCursor<FloatType> ct = It.createLocalizableByDimCursor(factory);
		final LocalizableCursor<FloatType> cux = ux.createLocalizableCursor();
		LocalizableByDimCursor<FloatType> cuy = uy.createLocalizableByDimCursor();
		LocalizableByDimCursor<FloatType> cl1 = null;
		LocalizableByDimCursor<FloatType> cl2 = null;
		if (computeAccuracy) {
			cl1 = lambda1.createLocalizableByDimCursor();
			cl2 = lambda2.createLocalizableByDimCursor();
		}
		
		int[] offset = new int[windowSize.length];
		for (int i = 0; i < offset.length; i++) 
			offset[i] = (windowSize[i]-1) / 2;
		float coeff;
		
		RegionOfInterestCursor<FloatType> lct = ct.createRegionOfInterestCursor(offset, windowSize);
		final int[] position = cux.createPositionArray();
		final int[] offsetPos = lct.createPositionArray();
		
		// Forward cursors to beginning of the chunk
		cux.fwd(startPos); // the other are slave to this position
		
		// Do as many pixels as wanted by this thread
		 for ( long j = 0; j < loopSize; ++j ) {

			 // Move cursors to next pixel
			cux.fwd();
			cuy.setPosition(cux);
			ct.setPosition(cux);
			if (computeAccuracy) {
				cl1.setPosition(cux);
				cl2.setPosition(cux);
			}
			
			// Check if current location is within the ROI. If not, pass.
			cux.getPosition(position);
			if (roi != null && !roi.contains(position[0], position[1])) {
				cux.getType().set(Float.NaN);
				cuy.getType().set(Float.NaN);
				continue;
			}

			// Gather neighborhood data
			float M11 = 0;
			float M12 = 0;
			float M22 = 0;
			float B1 = 0;
			float B2 = 0;
			float det;
			float tx, ty, tt;
			
			lct.reset(positionOffset(position, offsetPos, offset));
			while (lct.hasNext()) {
				lct.fwd();
				cx.setPosition(ct);
				cy.setPosition(ct);
				ct.setPosition(ct);

				tx = cx.getType().get();
				ty = cy.getType().get();
				tt = ct.getType().get();
				
				int index = 0;
				int factor;
				for (int i = 0; i < offsetPos.length; i++) {
					factor = 1;
					for (int k = 0; k < i; k++) {
						factor = factor * windowSize[k];
					}
					index = index + factor * lct.getPosition(i);
				}
				coeff = windowCoeffs[index];
				
				M11 += coeff * tx * tx;
				M22 += coeff * ty * ty; 
				M12 += coeff * tx * ty;
				B1  += - coeff * tx * tt;
				B2  += - coeff * ty * tt;
			}
			lct.close();
			
			// Determinant
			det = M11 * M22 - M12 * M12;
			
			// Inverse matrix
			float Minv11 = M22 / det;
			float Minv12 = - M12 / det; // = Minv21
			float Minv22 = M11 / det;
			
			// Eigenvalues
			double T = M11+M22;
			double l2 = T/2 + Math.sqrt(T*T/4 - det); // The large eigenvalue
			double l1 = T/2 - Math.sqrt(T*T/4 - det); // The small eigenvalue
			if (computeAccuracy) {
				cl1.getType().set((float) l1);
				cl2.getType().set((float) l2);
			}
			
			// Threshold
			float vx, vy;
			vx = Minv11 * B1 + Minv12 * B2;
			vy = Minv12 * B1 + Minv22 * B2;
			if (l1  < threshold) { // We do not accept the full velocity
				if (acceptNormals  && l2 > threshold) {
					// We take the raw normal velocity, by projecting on the large eigenvector
					double e2x = l2 - M22; // eigenvector for eigenvalue 2 (the big one)
					double e2y = M12;
					double e2normSquare = e2x*e2x+e2y*e2y;
					double ux = (vx * e2x + vy * e2y) * e2x / e2normSquare;
					double uy = (vx * e2x + vy * e2y) * e2y / e2normSquare;
					vx = (float) ux;
					vy = (float) uy;
				} else {
					// We reject all -> no flow
					vx = Float.NaN;
					vy = Float.NaN;
				}
			}
			
			cux.getType().set(vx);
			cuy.getType().set(vy);
			
		}
		cux.close();
		cuy.close();
		cx.close();
		cy.close();
		ct.close();
		if (computeAccuracy) {
			cl1.close();
			cl2.close();		
		}
		
		return true;
	}

	
	
	/**
	 * Offsets the given position to reflect the origin of the patch being in its center, rather
	 * than at the top-left corner as is usually the case.
	 * <p>
	 * For instance 
	 * <pre>
	 * 	positionOffset({14, 25, 36}, {0, 0, 0}, {-1, -1, 0});
	 * </pre>
	 * will return the array
	 * <pre>
	 * 	{13, 24, 36}
	 * </pre>
	 * @param position the position to be offset
	 * @param target an int array to contain the newly offset position coordinates
	 * @param origin  an int array containing the quantity to offset each dimension
	 * @return target, for convenience.
	 */
	private static final int[] positionOffset(final int[] position, final int[] target, final int[] origin)	{
			
		for (int i = 0; i < position.length; ++i)
			target[i] = position[i] - origin[i];
		return target;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
		
	}

	public void setComputeAccuracy(boolean computeAccuracy) {
		this.computeAccuracy = computeAccuracy;
	}

}
