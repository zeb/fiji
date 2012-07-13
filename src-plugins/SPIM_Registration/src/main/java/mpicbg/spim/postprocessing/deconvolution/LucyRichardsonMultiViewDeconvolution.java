package mpicbg.spim.postprocessing.deconvolution;

import ij.IJ;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.util.RealSum;
import net.imglib2.Cursor;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;

public class LucyRichardsonMultiViewDeconvolution
{
	public static boolean debug = false;
	public static int debugInterval = 10;
	
	public static Img<FloatType> lucyRichardsonMultiView( final ArrayList<LucyRichardsonFFT> data, final int minIterations, final int maxIterations, final boolean multiplicative, final double lambda, final int numThreads )
	{
		//final int numThreads = Runtime.getRuntime().availableProcessors();
		final int numViews = data.size();
		//final long numPixels = data.get( 0 ).getImage().getNumPixels();		
		//final double minValue = (1.0 / ( 10000.0 * numPixels ) );		
		final double minValue = 0.0001;
		
		final Img<FloatType> template = data.get( 0 ).getImage();
		final Img<FloatType> psi = template.factory().create( template, template.firstElement() );		
		
		//
		// for every image the integral of all pixel values
		//
		final AtomicInteger ai = new AtomicInteger(0);					
        Thread[] threads = SimpleMultiThreading.newThreads( numThreads );

        for ( int ithread = 0; ithread < threads.length; ++ithread )
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                    final int myNumber = ai.getAndIncrement();
		
					for ( int i = 0; i < data.size(); ++i )
						if ( i%numThreads == myNumber )
						{
							//IJ.log( new Date( System.currentTimeMillis() ) + " Norming image " + (i+1) );							
							//normImage( data.get( i ).getImage() );
							
							IJ.log( new Date( System.currentTimeMillis() ) + " Norming kernel " + (i+1) );							
							normImage( data.get( i ).getKernel() );
						}
                }
            });

        //IJ.log( "NumThreads: " + threads.length );
        SimpleMultiThreading.startAndJoin( threads );
        
        // the overlapping area has the same energy
        final double avg = normAllImages( data );
        
        //for ( LucyRichardsonFFT img : data )
        //{
        //	img.getImage().getDisplay().setMinMax();
        //	ImageJFunctions.copyToImagePlus( img.getImage() ).show();
        //}
        //SimpleMultiThreading.threadHaltUnClean();
        
        IJ.log( "Average intensity in overlapping area: " + avg );        
	
		//
		// the real data image psi is initialized with the average 
		//		
		final Cursor<FloatType> cursorPsiGlobal = psi.cursor();				
		//final float avgFloat = (float)( 1.0 / (double)numPixels );
		final float avgFloat = (float)avg;
		
		while ( cursorPsiGlobal.hasNext() )
		{
			cursorPsiGlobal.fwd();
			cursorPsiGlobal.get().set( avgFloat );
		}

		cursorPsiGlobal.reset();
				
		final Img<FloatType> nextPsi = psi.factory().create( psi, psi.firstElement() );
		final Cursor<FloatType> cursorNextPsiGlobal = nextPsi.cursor();
		
		//
		// Start iteration
		//
		double sumChange = 0;
		int i = 0;
		
		do
		{
			IJ.log( "iteration: " + i++ + " (" + new Date(System.currentTimeMillis()) + ")" );
			
			//
			// Set next psi to 1, we then multiply the results from the different views
			//			
			cursorNextPsiGlobal.reset();
			
			while ( cursorNextPsiGlobal.hasNext() )
			{
				cursorNextPsiGlobal.fwd();				
				cursorNextPsiGlobal.get().set( 1 );
			}		
			
			//
			// For each view we have to divide the image by the blurred image and convolve with the kernel
			// Then we multiply the result to psi to get the new estimate of psi
			//	
			ai.set( 0 );					
	        threads = SimpleMultiThreading.newThreads( numThreads );

	        for ( int ithread = 0; ithread < threads.length; ++ithread )
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                    final int myNumber = ai.getAndIncrement();
	                    
	        			for ( int view = 0; view < numViews; ++view )
		                    if ( view%numThreads == myNumber )
		        			{
		        				final LucyRichardsonFFT processingData = data.get( view );
		        								
		        				// convolve psi (current guess of the image) with the PSF of the current view
		        				final FFTConvolution<FloatType> fftConvolution = processingData.getFFTConvolution();
		        				final Img<FloatType> psiBlurred = psi.copy();
		        				fftConvolution.setImg( psiBlurred );
		        				fftConvolution.run(); //in-place
		        				
		        				//psiBlurred.getDisplay().setMinMax();
		        				//psiBlurred.setName( "psiBlurred " + view );
		        				//ImageJFunctions.copyToImagePlus( psiBlurred ).show();

		        				// compute quotient img/psiBlurred
		        				final Cursor<FloatType> cursorImg = processingData.getImage().cursor();
		        				final Cursor<FloatType> cursorPsiBlurred = psiBlurred.cursor();
	
		        				while ( cursorImg.hasNext() )
		        				{
		        					cursorImg.fwd(); cursorPsiBlurred.fwd();
		        					
		        					final float imgValue = cursorImg.get().get();
		        					final float psiBlurredValue = cursorPsiBlurred.get().get();
		        										
		        					cursorPsiBlurred.get().set( imgValue / psiBlurredValue );
		        				}	
	
		        				//psiBlurred.getDisplay().setMinMax();
		        				//psiBlurred.setName( "img/psiBlurred " + view );
		        				//ImageJFunctions.copyToImagePlus( psiBlurred ).show();

		        				// blur the residuals image with the kernel
		        				fftConvolution.setImg( psiBlurred );
		        				fftConvolution.run(); //in-place
		        				processingData.setViewContribution( psiBlurred );

		        				//fftConvolution.getResult().getDisplay().setMinMax();
		        				//fftConvolution.getResult().setName( "conv(img/psiBlurred) " + view );
		        				//ImageJFunctions.copyToImagePlus( fftConvolution.getResult() ).show();
		        			}
	                }
	            });
	        
	        SimpleMultiThreading.startAndJoin( threads );
	        	        			
			//
			// multiply residualsBlurred with nextPsi and compute the n-root of each pixel ( where n is the number of views )
			// this cannot be done in multithreaded as it would collide
			//
			final ArrayList<Cursor<FloatType>> blurredResidualsCursors = new ArrayList<Cursor<FloatType>>();
			//final ArrayList<Cursor<FloatType>> imageCursors = new ArrayList<Cursor<FloatType>>();
			final ArrayList<Cursor<FloatType>> weightCursors = new ArrayList<Cursor<FloatType>>();

			for ( int view = 0; view < numViews; ++view )
			{
				blurredResidualsCursors.add( data.get( view ).getViewContribution().cursor() );
				//imageCursors.add( data.get( view ).getImage().cursor() );
				
				if ( data.get( view ).getWeight() != null )	
					weightCursors.add( data.get( view ).getWeight().cursor() );
				
				//data.get( view ).getWeight().getDisplay().setMinMax();
				//data.get( view ).getWeight().setName( "weight " + view );
				//ImageJFunctions.copyToImagePlus( data.get( view ).getWeight() ).show();				
			}
			
			cursorNextPsiGlobal.reset();
			cursorPsiGlobal.reset();
						
			while ( cursorNextPsiGlobal.hasNext() )
			{
				cursorNextPsiGlobal.fwd();
				
				double value = cursorNextPsiGlobal.get().get();
				
				if ( !multiplicative )
					value = 0;
				
				double num = 0;
				
				if ( weightCursors.size() > 0 )
				{
					for ( int h = 0; h < numViews; ++h )
					{
						final Cursor<FloatType> cursorResidualsBlurred = blurredResidualsCursors.get( h );					
						//final Cursor<FloatType> cursorImage = imageCursors.get( h );
						final Cursor<FloatType> cursorWeight = weightCursors.get( h );
											
						cursorResidualsBlurred.fwd();
						//cursorImage.fwd();
						cursorWeight.fwd();
						
						final float weight = cursorWeight.get().get();
						if ( weight > 0 )
						{
							if ( multiplicative )
								value *= Math.pow( cursorResidualsBlurred.get().get(), weight );
							else
								value += cursorResidualsBlurred.get().get() * weight;

							num += weight;
						}
					}
				}
				else
				{
					for ( int h = 0; h < numViews; ++h )
					{
						final Cursor<FloatType> cursorResidualsBlurred = blurredResidualsCursors.get( h );					
						//final Cursor<FloatType> cursorImage = imageCursors.get( h );
											
						cursorResidualsBlurred.fwd();
						//cursorImage.fwd();
						
						value *= cursorResidualsBlurred.get().get();
						num++;
					}					
				}
				
				cursorPsiGlobal.fwd();
				
				if ( num > 0 )
				{
					if ( multiplicative )
						value = (double)cursorPsiGlobal.get().get() * Math.pow( value, 1.0/num );
					else
						value = (double)cursorPsiGlobal.get().get() * cursorNextPsiGlobal.get().get() * value/num;
				}
				else
				{
					// maybe that works ...
					value = minValue; //(double)cursorPsiGlobal.get().get() * cursorNextPsiGlobal.get().get();					
				}
				
				cursorNextPsiGlobal.get().set( (float)value );
			}

			//nextPsi.getDisplay().setMinMax();
			//nextPsi.setName( "nextPsi " );
			//ImageJFunctions.copyToImagePlus( nextPsi ).show();

			//SimpleMultiThreading.threadHaltUnClean();

			//
			// perform Tikhonov regularization if desired
			//		
			if ( lambda > 0 )
			{
				cursorNextPsiGlobal.reset();
				
				while ( cursorNextPsiGlobal.hasNext() )
				{
					cursorNextPsiGlobal.fwd();
					final float f = cursorNextPsiGlobal.get().get();
					final float reg = (float)( (Math.sqrt( 1.0 + 2.0*lambda*f ) - 1.0) / lambda );
					cursorNextPsiGlobal.get().set( reg );
				}
			}
			
			//
			// Update psi for next iteration
			//						
			cursorPsiGlobal.reset();
			cursorNextPsiGlobal.reset();
			
			sumChange = 0;
			double maxChange = -1;
			
			while ( cursorNextPsiGlobal.hasNext() )
			{
				cursorPsiGlobal.fwd(); 
				cursorNextPsiGlobal.fwd();
				
				final float lastPsiValue = cursorPsiGlobal.get().get();
				
				final float nextPsiValue;
				if ( Float.isNaN( cursorNextPsiGlobal.get().get() ) )
					nextPsiValue = (float)minValue;
				else
					nextPsiValue = (float)Math.max( minValue, cursorNextPsiGlobal.get().get() );
				
				cursorPsiGlobal.get().set( nextPsiValue );
				
				final float change = Math.abs( lastPsiValue - nextPsiValue );				
				sumChange += change;
				maxChange = Math.max( maxChange, change );
			}

			IJ.log("------------------------------------------------");
			IJ.log(" Change: " + sumChange );
			IJ.log(" Max Change per Pixel: " + maxChange );
			IJ.log("------------------------------------------------");			
			
			System.out.println( i + "\t" + sumChange + "\t" + maxChange );
			
			
			if ( debug && i % debugInterval == 0 )
				ImageJFunctions.show( psi.copy() ).setTitle( "Iteration " + i + " l=" + lambda );
		}
		while ( i < maxIterations );
	
		return psi;
	}
	
	public static double normAllImages( final ArrayList<LucyRichardsonFFT> data )
	{
		// the individual sums of the overlapping area
		//final double[] sums = new double[ data.size() ];
		
		final RealSum sum = new RealSum();
		// the number of overlapping pixels
		long count = 0;
		
		final ArrayList<Cursor<FloatType>> cursorsImage = new ArrayList<Cursor<FloatType>>();
		final ArrayList<Cursor<FloatType>> cursorsWeight = new ArrayList<Cursor<FloatType>>();
		
		for ( final LucyRichardsonFFT fft : data )
		{
			cursorsImage.add( fft.getImage().cursor() );
			if ( fft.getWeight() != null )
				cursorsWeight.add( fft.getWeight().cursor() );
		}
		
		final Cursor<FloatType> cursor = cursorsImage.get( 0 );

		// sum overlapping area individually
A:		while ( cursor.hasNext() )
		{
			for ( final Cursor<FloatType> c : cursorsImage )
				c.fwd();
			
			for ( final Cursor<FloatType> c : cursorsWeight )
				c.fwd();
			
			// only sum if all views overlap
			//for ( final Cursor<FloatType> c : cursorsWeight )
			//	if ( c.get().get() == 0 )
			//		continue A;
			
			// sum up individual intensities
			double sumLocal = 0;
			int countLocal = 0;
			
			for ( int i = 0; i < cursorsImage.size(); ++i )
			{
				if ( cursorsWeight.get( i ).get().get() != 0 )
				{
					sumLocal += cursorsImage.get( i ).get().get();
					countLocal++;
				}
			}

			// at least two overlap
			if ( countLocal > 1 )
			{
				sum.add( sumLocal );
				count += countLocal;
			}
		}

		if ( count == 0 )
			return 1;
		
		// compute the average sum
		final double avg = sum.getSum() / (double)count;
		
		/*
		for ( double d : sums )
			avgSum += d;		
		avgSum /= (double)(sums.length);

		// adjust so that the sum of each overlapping area equals the average sum
		for ( int i = 0; i < sums.length; ++i )
		{
			sums[ i ] = avgSum / sums[ i ];
			IJ.log( "Normalizing view " + (i+1) + " with " + sums[ i ] );
		}

		// apply to data over the whole image
		for ( final Cursor<FloatType> c : cursorsImage )
			c.reset();

		while ( cursor.hasNext() )
		{
			int i = 0;
			for ( final Cursor<FloatType> c : cursorsImage )
			{
				c.fwd();
				// TODO: this is removed for testing only!
				//c.get().set( (float)(c.get().get() / sums[ i++ ]) );
			}
		}
		*/
		
		// return the average intensity in the overlapping area
		return avg;
	}

	
	final private static BigDecimal sumImage( final Img<FloatType> img )
	{
		BigDecimal sum = new BigDecimal( 0, MathContext.UNLIMITED );
		
		final Cursor<FloatType> cursorImg = img.cursor();
		
		while ( cursorImg.hasNext() )
		{
			cursorImg.fwd();
			sum = sum.add( BigDecimal.valueOf( (double)cursorImg.get().get() ) );
		}

		return sum;
	}
	
	final private static void normImage( final Img<FloatType> img )
	{
		final BigDecimal sum = sumImage( img );	
		
		final Cursor<FloatType> cursor = img.cursor();
					
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.get().set( (float) ((double)cursor.get().get() / sum.doubleValue()) );
		}
	}
}
