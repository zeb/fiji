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

public class SimoncelliDerivation  < A extends RealType<A>> implements OutputAlgorithm<FloatType>, MultiThreaded, Benchmark {

	private static final String BASE_ERROR_MESSAGE = "SimoncelliDerivation: ";

	private static final double[] SMOOTHING_KERNEL  = new double[] { 
		0.035697564482688904, 
		0.24887460470199585,
		0.4308556616306305, 
		0.24887460470199585, 
		0.035697564482688904
	}; 
	
	private static final double[] DIFFERENTIATING_KERNEL = new double[] {
		-0.1076628714799881,
		-0.2826710343360901,
		0,
		0.2826710343360901,
		0.1076628714799881		
	};
	
	private static final double[][] KERNELS = new double[][] { // Order is important
		SMOOTHING_KERNEL,
		DIFFERENTIATING_KERNEL
	};

    private OutOfBoundsStrategyFactory<FloatType> outOfBoundsFactory = new OutOfBoundsStrategyMirrorFactory<FloatType>();
	private long processingTime;
	private int numThreads = 1;
	private Image<A> image ;
	private Image<FloatType> floatImage;
	private Image<FloatType> convolved;
	private String errorMessage;

	private int targetDim;

	private Image<FloatType> tmp;


	
	public SimoncelliDerivation(Image<A> image) {
		this.image = image;
		ImageConverter<A, FloatType> converter = new ImageConverter<A, FloatType>(
				image, 
				new ImageFactory<FloatType>(new FloatType(), new ImagePlusContainerFactory()), 
				new RealTypeConverter<A, FloatType>());
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
		return true;
	}

	@Override
	public boolean process() {

		final long startTime = System.currentTimeMillis();
		final long imageSize = floatImage.getNumPixels();
		convolved = floatImage.createNewImage();
		tmp = floatImage.clone();
		
		final int[] sequence = new int[floatImage.getNumDimensions()];
		sequence[targetDim] = 1; // will point to derivation kernel; 0s point to smoothing kernel
		
		// divide the image into chunks
        final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
        
        for ( int dim = 0; dim < floatImage.getNumDimensions(); dim++ ) {
        	final int currentDim = dim;
        	final double[] kernel = KERNELS[sequence[currentDim]];
        	
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

        				final LocalizableByDimCursor<FloatType> inputIterator = tmp.createLocalizableByDimCursor(outOfBoundsFactory);
        				final LocalizableCursor<FloatType> outputIterator = convolved.createLocalizableCursor();

        				// Convolve the image in the current dimension using the given cursors
        				convolve( inputIterator, outputIterator, currentDim, kernel, myChunk.getStartPosition(), myChunk.getLoopSize() );

        				inputIterator.close();
        				outputIterator.close();
        				
        			}
        		});

        	SimpleMultiThreading.startAndJoin(threads);
        	tmp = convolved.clone(); // Source for next iteration
        	
        }

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
