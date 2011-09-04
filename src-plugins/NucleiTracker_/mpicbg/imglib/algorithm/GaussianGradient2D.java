package mpicbg.imglib.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.gauss.GaussianConvolutionReal2D;
import mpicbg.imglib.algorithm.math.ImageConverter;
import mpicbg.imglib.cursor.Cursor;
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

public class GaussianGradient2D <T extends RealType<T>> extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<FloatType> {

	private Image<T> source;
	private double sigma;
	private Image<FloatType> Dx;
	private Image<FloatType> Dy;
	private List<Image<FloatType>> components = new ArrayList<Image<FloatType>>(2);


	/*
	 * CONSTRUCTOR
	 */


	public GaussianGradient2D(Image<T> source, double sigma) {
		super();
		this.source = source;
		this.sigma = sigma;
	}


	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean process() {
		long start = System.currentTimeMillis();

		// Filter by a 2D gaussian
		GaussianConvolutionReal2D<T> gaussFilter = new GaussianConvolutionReal2D<T>(
				source, 
				new OutOfBoundsStrategyMirrorFactory<T>(), 
				new double[] { sigma, sigma} );

		boolean check = gaussFilter.checkInput() && gaussFilter.process();
		Image<T> filtered;
		if (check) {
			filtered = gaussFilter.getResult();
		} else {
			errorMessage = gaussFilter.getErrorMessage();
			return false;
		}

		// Compute gradients 

		// Convert to float; needed to handle negative value properly
		final Image<FloatType> floatImage;
		if (filtered.createType().getClass().equals(FloatType.class)) {
			floatImage = (Image<FloatType>) filtered;
		} else {
			ImageConverter<T, FloatType> converter = new ImageConverter<T, FloatType>(
					filtered,
					new ImageFactory<FloatType>(new FloatType(), filtered.getContainerFactory()),
					new RealTypeConverter<T, FloatType>());
			converter.setNumThreads();
			converter.checkInput();
			converter.process();
			floatImage = converter.getResult();
		}

		final long imageSize = floatImage.getNumPixels();
		Dx = floatImage.createNewImage("Gx");
		Dy = floatImage.createNewImage("Gy");

		// divide the image into chunks
		final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
		final AtomicInteger ai = new AtomicInteger(0);
		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );

		for (int ithread = 0; ithread < threads.length; ++ithread)

			// Build Thread array
			threads[ithread] = new Thread(new Runnable() {

				public void run() {

					// Thread ID
					final int myNumber = ai.getAndIncrement();

					// Get chunk of pixels to process
					final Chunk myChunk = threadChunks.get( myNumber );

					final LocalizableByDimCursor<FloatType> inputIterator = floatImage.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<FloatType>());
					final LocalizableByDimCursor<FloatType> cx = Dx.createLocalizableByDimCursor();
					final LocalizableCursor<FloatType> cy = Dy.createLocalizableCursor();

					// Convolve the image in the current dimension using the given cursors
					convolve( inputIterator, cx, cy, myChunk.getStartPosition(), myChunk.getLoopSize() );

					inputIterator.close();
					cx.close();
					cy.close();

				}
			}, "GaussianGradient2D thread "+ithread);

		SimpleMultiThreading.startAndJoin(threads);

		components.clear();
		components.add(Dx);
		components.add(Dy);

		long end = System.currentTimeMillis();
		processingTime = end-start;
		return true;
	}

	private final void convolve(
			final LocalizableByDimCursor<FloatType> inputIterator, 
			final LocalizableByDimCursor<FloatType> ix,
			final LocalizableCursor<FloatType> iy,
			final long startPos, final long loopSize )  {

		// move to the starting position of the current thread
		iy.fwd( startPos );

		final int[] to = new int[ source.getNumDimensions() ];

		for ( long j = 0; j < loopSize; ++j ) {

			iy.fwd();
			iy.getPosition(to);
			ix.setPosition(iy);

			// The more simple and stupid we can do: iterate "manually" over the kernel,
			// which is just -1 0 1;

			// Along X
			to[0]--;
			inputIterator.setPosition(to);
			ix.getType().sub(inputIterator.getType());

			to[0]++;
			to[0]++;
			inputIterator.setPosition(to);
			ix.getType().add(inputIterator.getType());

			to[0]--;

			// Along Y
			to[1]--;
			inputIterator.setPosition(to);
			iy.getType().sub(inputIterator.getType());

			to[1]++;
			to[1]++;
			inputIterator.setPosition(to);
			iy.getType().add(inputIterator.getType());

		}
	}


	public List<Image<FloatType>> getGradientComponents() {
		return components;
	}


	/**
	 * Return the gradient norm
	 */
	@Override
	public Image<FloatType> getResult() {
		Image<FloatType> norm = Dx.createNewImage("Gradient norm");
		Cursor<FloatType> cx = Dx.createCursor();
		Cursor<FloatType> cy = Dy.createCursor();
		Cursor<FloatType> cn = norm.createCursor();

		double x, y;
		while(cn.hasNext()) {
			cn.fwd();
			cx.fwd();
			cy.fwd(); // Ok because we have identical containers
			x = cx.getType().get();
			y = cy.getType().get();
			cn.getType().setReal(Math.sqrt(x*x+y*y));
		}
		cx.close();
		cy.close();
		cn.close();

		return norm;
	}

}
