package fiji.plugin.flowmate;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.MultiThreaded;
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
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * Implement Simoncelli matched derivative filters from \cite{simoncelli94}.
 * <p>
 * Discrete derivation can be seen as a convolution with a filter, the <i>derivative filter</i> which is the derivation
 * of some smoothing filter, used for the interpolation of the signal, called here the <i>prefilter</i>.  
 * Simoncelli's point when designing multi-dimensional filter is that there is a wide range of
 * situation when the derivative of a signal will be used along the signal itself or with another
 * derivative. In these case, it make sense to smooth the signal using the prefilter mentioned above, so 
 * that it matches the amount of filtering done for the calculation of the derivative. 
 * <p>
 * Simoncelli proposes to design both filter simultaneously, under the following constraints:
 * <ul>
 * 	<li>The derivative filter must be a good approximation to the exact derivation of the prefiltered
 * signal.
 * 	<li>The prefilter should have linear, ideally 0, phase.
 * 	<li>The filters should be separable.
 * </ul>
 * <p>
 * This yields the derivation algorithm implemented here. The image is derived in one dimension using the derivative
 * filter of Simoncelli, and all other dimensions are filtered using the matching prefilter. We proposed the four 
 * kernels calculated by Simoncelli, whose support ranges from 2 to 5.
 * 
 * <p>
 * BibTeX:
 * <pre>
 * &#64;inproceedings{simoncelli94
 * 	author = {Eero P. Simoncelli},
 * 	title = {Design of Multi-Dimensional Derivative Filters},
 * 	booktitle = {International Conference on Image Processing},
 * 	year = {1994},
 * 	pages = {790--794},
 * 	masid = {327150}
 * }
 * </pre>
 *  
 * @author Jean-Yves Tinevez
 * @param <A> the type of the source image. Must a {@link RealType}, for we perform all operations on floats, and
 * must be able to convert to {@link FloatType}. 
 */
public class SimoncelliDerivation<A extends RealType<A>> implements OutputAlgorithm<FloatType>, MultiThreaded, Benchmark {

	private static final String BASE_ERROR_MESSAGE = "SimoncelliDerivation: ";

	private static final double[][] SMOOTHING_KERNELS  = new double[][] {
		{	// Support = 2: central difference
			0.5,
			0.5
		},
		{ // Support = 3
			0.22420981526374817,
			0.5515803694725037,
			0.22420981526374817
		},
		{ // Support = 4
			0.09156766533851624,
			0.40843233466148377,
			0.40843233466148377,
			0.09156766533851624
		},
		{  // Support = 5
			0.035697564482688904, 
			0.24887460470199585,
			0.4308556616306305, 
			0.24887460470199585, 
			0.035697564482688904 
		}
	}; 
	
	private static final double[][] DIFFERENTIATING_KERNELS = new double[][] {
		{ // Support = 2
			-0.6860408186912537,
			0.6860408186912537,
		},
		{ // Support = 3
			-0.45527133345603943,
			0,
			0.45527133345603943
		},
		{ // Support = 4
			-0.2362217784125595,
			-0.27143725752830506,
			0.27143725752830506,
			0.2362217784125595
		},
		{ // Support = 5
			-0.1076628714799881,
			-0.2826710343360901,
			0,
			0.2826710343360901,
			0.1076628714799881		
		}
	};
	
	private static final double[][][] KERNELS = new double[][][] { // Order is important
		SMOOTHING_KERNELS,
		DIFFERENTIATING_KERNELS
	};

    private OutOfBoundsStrategyFactory<FloatType> outOfBoundsFactory = new OutOfBoundsStrategyMirrorFactory<FloatType>();
	private long processingTime;
	private int numThreads = 1;
	private Image<A> image ;
	private Image<FloatType> floatImage;
	private Image<FloatType> convolved;
	private String errorMessage;

	private int targetDim;

	/**
	 * The kernel size we will use. In this class, we have the kernel values only for support which size is ranging
	 * from 2 to 5. 
	 */
	private int support;
	/** Temporary holder needed to share data between threads. */
	private Image<FloatType> source;


	
	public SimoncelliDerivation(Image<A> image, final int kernelSize) {
		this.image = image;
		this.support = kernelSize;
		ImageConverter<A, FloatType> converter = new ImageConverter<A, FloatType>(
				image, 
				new ImageFactory<FloatType>(new FloatType(), new ImagePlusContainerFactory()), 
				new RealTypeConverter<A, FloatType>());
		converter.setNumThreads();
		converter.checkInput();
		converter.process();
		this.floatImage = converter.getResult();
		setNumThreads();
	}
	
	
	/**
	 * Set the target dimension, along which the derivation will be calculated. 
	 * Simoncelli's smoothing kernel will be applied in all other dimension, and
	 * Simoncelli's differentiating kernel will be applied in this direction  
	 * @param targetDim  the dimension along which to differentiate
	 */
	public void setDerivationDimension(int targetDim) {
		this.targetDim = targetDim;
	}
	

	@Override
	public boolean checkInput() {
		if ( image == null ) {
			errorMessage = BASE_ERROR_MESSAGE+ "[Image<T> img] is null.";
			return false;
		}
		if (0 > targetDim || targetDim >= image.getNumDimensions()) {
			errorMessage = BASE_ERROR_MESSAGE + "Target derivation dimension is incorrect.";
			return false;
		}
		if (support < 2 || support > 5) {
			errorMessage = BASE_ERROR_MESSAGE + "Only kernel size ranging from 2 to 5 is supported, for "+support+".";
			return false;
		}
		return true;
	}

	@Override
	public boolean process() {

		final long startTime = System.currentTimeMillis();
		final long imageSize = floatImage.getNumPixels();
		convolved = floatImage.clone();
		
		final int[] sequence = new int[floatImage.getNumDimensions()];
		sequence[targetDim] = 1; // will point to derivation kernel; 0s point to smoothing kernel
		
		// divide the image into chunks
        final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
        
        for ( int dim = 0; dim < floatImage.getNumDimensions(); dim++ ) {
        	source = convolved.clone(); // Source for next iteration
        	final int currentDim = dim;
        	final double[] kernel = KERNELS[sequence[currentDim]][support-2];
        	
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

        				final LocalizableByDimCursor<FloatType> inputIterator = source.createLocalizableByDimCursor(outOfBoundsFactory);
        				final LocalizableCursor<FloatType> outputIterator = convolved.createLocalizableCursor();

        				// Convolve the image in the current dimension using the given cursors
        				convolve( inputIterator, outputIterator, currentDim, kernel, myChunk.getStartPosition(), myChunk.getLoopSize() );

        				inputIterator.close();
        				outputIterator.close();
        				
        			}
        		});

        	SimpleMultiThreading.startAndJoin(threads);
        	
        }
        source = null;

        String name = image.getName()+"_";
        for (int i = 0; i < sequence.length; i++) {
			if (sequence[i] > 0)
				name += 'D';
			else
				name += 'S';
		}
        convolved.setName(name);
        processingTime += System.currentTimeMillis() - startTime;
        return true;        
	}

	
	private final void convolve(final LocalizableByDimCursor<FloatType> inputIterator, final LocalizableCursor<FloatType> outputIterator, 
			final int dim, final double[] kernel, final long startPos, final long loopSize )	{		
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
    	
    	final FloatType sum = inputIterator.getType().createVariable();
    	final FloatType tmp = inputIterator.getType().createVariable();
        
    	
        // do as many pixels as wanted by this thread
        for ( long j = 0; j < loopSize; ++j ) {
        	outputIterator.fwd();			                			                	

        	// set the sum to zero
        	sum.setZero();
        	
        	// We move filtersize/2 of the convolved pixel in the input image
        	
        	// get the current positon in the output image
    		outputIterator.getPosition( to );
    		
    		// position in the input image is filtersize/2 to the left
    		to[ dim ] -= iteratorPosition;
    		
    		// set the input cursor to this very position
    		inputIterator.setPosition( to );

    		// iterate over the kernel length across the input image
        	for ( int f = -filterSizeHalf; f <= filterSizeHalfMinus1; ++f )	{
        		// get value from the input image
        		tmp.set( inputIterator.getType() );

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
    		tmp.set( inputIterator.getType() );
    		    		
    		// multiply the kernel
    		tmp.mul( lastKernelEntry );
    		
    		// add up the sum
    		sum.add( tmp );
    		    		
            outputIterator.getType().set( sum );			                		        	
        }
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}
	
	@Override
	public Image<FloatType> getResult() {
		 return convolved;
	}	
 	
	@Override
	public long getProcessingTime() { return processingTime; }
	
	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }

}
