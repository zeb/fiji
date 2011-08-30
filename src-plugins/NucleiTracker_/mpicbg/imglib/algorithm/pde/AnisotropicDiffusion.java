package mpicbg.imglib.algorithm.pde;


import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.math.ImageConverter;
import mpicbg.imglib.container.imageplus.ImagePlusContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

public class AnisotropicDiffusion <T extends RealType<T>> extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<FloatType>{

	private Image<T> image;
	private int type;
	private int niter;
	private float deltat;
	private float kappa;
	private Image<FloatType> target;

	/*
	 * CONSTRUCTOR
	 */


	public AnisotropicDiffusion(Image<T> image, int type, int niter, float deltat, float kappa) {
		this.image = image;
		this.type = type;
		this.niter = niter;
		this.deltat = deltat;
		this.kappa = kappa;
	}

	/*
	 * METHODS
	 */
	@Override

	public boolean checkInput() {
		if (!(type == 1 || type == 2)) {
			errorMessage = "Unknwown diffusion function type: "+type+".";
			return false;
		}
		if (niter < 0) {
			errorMessage = "Number of iteration must be at least 0, got "+niter+".";
			return false;
		}
		if (deltat <= 0) {
			errorMessage = "Time interval must bu strictly positive, got "+deltat+".";
			return false;
		}
		if (kappa == 0) {
			errorMessage = "Gradient mgnitude scale can't be 0.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();

		final int ndim = image.getNumDimensions();
		final double nneighbors = Math.pow(3, ndim) - 1 ;

		// Cast to float type for precision
		ImageConverter<T, FloatType> converter = new ImageConverter<T, FloatType>(
				image,
				new ImageFactory<FloatType>(new FloatType(), new ImagePlusContainerFactory()),
				new RealTypeConverter<T, FloatType>());
		converter.setNumThreads();
		converter.checkInput();
		converter.process();
		target = converter.getResult();

		// Main loop
		for (int t = 0; t < niter; t++) {


			final AtomicInteger ai = new AtomicInteger(0);			
			final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(target.getNumPixels(), numThreads);
			final Thread[] threads = SimpleMultiThreading.newThreads(numThreads);

			for (int ithread = 0; ithread < threads.length; ithread++) {

				threads[ithread] = new Thread(new Runnable() {

					public void run() {

						int[] centralPosition = new int[ndim];
						int[] position = new int[ndim];
						LocalizableCursor<FloatType> mainCursor = target.createLocalizableCursor();
						LocalizableByDimCursor<FloatType> cursor = target.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<FloatType>());

						final int threadNumber = ai.getAndIncrement();
						final Chunk chunk = chunks.get( threadNumber );
						
						mainCursor.fwd(chunk.getStartPosition());

						for ( long j = 0; j < chunk.getLoopSize(); ++j ) {

							mainCursor.fwd();		
							double central = mainCursor.getType().get();
							mainCursor.getPosition(centralPosition);

							// Init neighbor cursor position
							position[0] = -2;
							for (int dim = 1; dim < ndim; dim++) {
								position[dim] = -1;
							}

							// Loop over all neighbors
							double amount = 0;
							while (true) {

								// Move to next neighbor
								for (int dim = 0; dim < ndim; dim++) {
									if (position[dim] < 1) {
										position[dim]++;
										break;
									} else {
										position[dim] = -1;
									}
								}

								for (int dim = 0; dim < ndim; dim++) {
									cursor.setPosition(centralPosition[dim] + position[dim], dim);
								}

								// Lattice length
								double dx2 = 0;
								for (int dim = 0; dim < ndim; dim++) {
									dx2 += position[dim] * position[dim];
								}

								if (dx2 == 0) {
									continue; // Skip central point
								}

								// Finite differences
								double di = cursor.getType().getRealDouble() - central;

								// Diffusion function
								double g = Math.exp(- (di*di/kappa/kappa));

								// Amount
								amount += 1/dx2 * g * di;

								// Test if we are finished (all position indices to 1)
								boolean finished = true;
								for (int dim = 0; dim < ndim; dim++) {
									if (position[dim] != 1) {
										finished = false;
										break;
									}
								}
								if (finished) {
									break;
								}

							} // Finished looping over neighbors

							// Update current value

							mainCursor.getType().setReal(mainCursor.getType().get() + deltat * amount / nneighbors);

						}
					}
				});
			}

			SimpleMultiThreading.startAndJoin(threads);
		}
		
		long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public Image<FloatType> getResult() {
		return target;
	}




}
