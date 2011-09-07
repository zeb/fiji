package mpicbg.imglib.algorithm;


import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.math.ImageConverter;
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

public class Convolver1D<T extends RealType<T>> extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<FloatType> {

	private Image<T> source;
	private double[] kernel;
	private int dim;
	private Image<FloatType> convolved;

	/*
	 * CONSTRUCTOR
	 */

	public Convolver1D(final Image<T> source, double[] kernel, int dim) {
		super();
		this.source = source;
		this.kernel = kernel;
		this.dim = dim;
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {

		final long startTime = System.currentTimeMillis();

		ImageConverter<T, FloatType> converter = new ImageConverter<T, FloatType>(
				source,
				new ImageFactory<FloatType>(new FloatType(),source.getContainerFactory()),
				new RealTypeConverter<T, FloatType>());
		converter.setNumThreads();
		converter.checkInput();
		converter.process();
		final Image<FloatType> floatImage = converter.getResult();


		final long imageSize = floatImage.getNumPixels();
		convolved = floatImage.createNewImage();

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
					final LocalizableCursor<FloatType> outputIterator = convolved.createLocalizableCursor();

					// Convolve the image in the current dimension using the given cursors
					convolve( inputIterator, outputIterator, myChunk.getStartPosition(), myChunk.getLoopSize() );

					inputIterator.close();
					outputIterator.close();

				}
			}, "Convolver1D thread "+ithread);

		SimpleMultiThreading.startAndJoin(threads);
		processingTime = System.currentTimeMillis() - startTime;
		return true;
	}

	private final void convolve(final LocalizableByDimCursor<FloatType> inputIterator, final LocalizableCursor<FloatType> outputIterator,
			final long startPos, final long loopSize )  {

		// move to the starting position of the current thread
		outputIterator.fwd( startPos );

		final int filterSize = kernel.length;
		final int filterSizeMinus1 = filterSize - 1;
		final int filterSizeHalf = filterSize / 2;
		final int filterSizeHalfMinus1 = filterSizeHalf - 1;
		final int numDimensions = inputIterator.getImage().getNumDimensions();

		final int iteratorPosition = filterSizeHalf;
		final double lastKernelEntry = kernel[ filterSizeMinus1 ];

		final int[] to = new int[ numDimensions ];

		final FloatType sum = new FloatType();
		final FloatType tmp = new FloatType();


		// do as many pixels as wanted by this thread
		for ( long j = 0; j < loopSize; ++j ) {
			outputIterator.fwd();

			// Get the current positon in the output image
			outputIterator.getPosition( to );

			// Set the sum to zero
			sum.setZero();

			// position in the input image is filtersize/2 to the left
			to[ dim ] -= iteratorPosition;

			// set the input cursor to this very position
			inputIterator.setPosition( to );

			// iterate over the kernel length across the input image
			for ( int f = -filterSizeHalf; f <= filterSizeHalfMinus1; ++f )     {

				// get value from the input image
				tmp.set( inputIterator.getType().getRealFloat() );

				// multiply the kernel
				tmp.mul( kernel[ f + filterSizeHalf ] );

				// add up the sum
				sum.add( tmp );

				// move the cursor forward for the next iteration
				inputIterator.fwd( dim );
			}

			//
			// for the last pixel we do not move forward
			//

			// get value from the input image
			tmp.set( inputIterator.getType().getRealFloat() );

			// multiply the kernel
			tmp.mul( lastKernelEntry );

			// add up the sum
			sum.add( tmp );

			outputIterator.getType().setReal( sum.get() );
		}
	}


	@Override
	public Image<FloatType> getResult() {
		return convolved;
	}

}
