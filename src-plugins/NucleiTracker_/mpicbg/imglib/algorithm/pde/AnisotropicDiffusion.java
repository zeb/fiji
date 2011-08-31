package mpicbg.imglib.algorithm.pde;


import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;

public class AnisotropicDiffusion <T extends RealType<T>> extends MultiThreadedBenchmarkAlgorithm {

	private Image<T> image;
	private float deltat;
	private float kappa;

	/*
	 * CONSTRUCTOR
	 */

	public AnisotropicDiffusion(Image<T> image, float deltat, float kappa) {
		this.image = image;
		this.deltat = deltat;
		this.kappa = kappa;
		this.processingTime = 0;
	}

	/*
	 * METHODS
	 */
	@Override

	public boolean checkInput() {
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


		final AtomicInteger ai = new AtomicInteger(0);			
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(image.getNumPixels(), numThreads);
		final Thread[] threads = SimpleMultiThreading.newThreads(numThreads);

		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread(new Runnable() {

				public void run() {

					int[] centralPosition = new int[ndim];
					int[] position = new int[ndim];
					LocalizableCursor<T> mainCursor = image.createLocalizableCursor();
					LocalizableByDimCursor<T> cursor = image.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<T>());

					final int threadNumber = ai.getAndIncrement();
					final Chunk chunk = chunks.get( threadNumber );
					T increment = mainCursor.getType().createVariable();

					mainCursor.fwd(chunk.getStartPosition());

					for ( long j = 0; j < chunk.getLoopSize(); ++j ) {

						mainCursor.fwd();		
						T centralValue = mainCursor.getType();
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
							double di = cursor.getType().getRealDouble() - centralValue.getRealDouble();

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
						increment.setReal(deltat * amount / nneighbors);
						mainCursor.getType().add(increment);

					}
				}
			});
		}

		SimpleMultiThreading.startAndJoin(threads);

		long end = System.currentTimeMillis();
		processingTime += (end - start);
		return true;
	}

}
