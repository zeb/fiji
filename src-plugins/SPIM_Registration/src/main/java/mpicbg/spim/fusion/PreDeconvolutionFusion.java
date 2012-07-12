package mpicbg.spim.fusion;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3f;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class PreDeconvolutionFusion extends SPIMImageFusion
{
	final Img<FloatType> images[], weights[];
	final int numViews;
	
	public PreDeconvolutionFusion( final ViewStructure viewStructure, final ViewStructure referenceViewStructure, 
								  final ArrayList<IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories, 
								  final ArrayList<CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories )
	{
		super( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories );				
		
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Reserving memory for fused image.");
		
		final ImgFactory<FloatType> imageFactory = conf.outputImageFactory;
		numViews = viewStructure.getNumViews();
		
		images = new Img[ numViews ];
		weights = new Img[ numViews ];
		
		for ( int view = 0; view < numViews; view++ )
		{
			weights[ view ] = imageFactory.create( new int[]{ imgW, imgH, imgD }, new FloatType() );
			images[ view ] = imageFactory.create( new int[]{ imgW, imgH, imgD }, new FloatType() ); 
			
			if ( images[ view ] == null || weights[ view ] == null )
			{
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
					IOFunctions.println("PreDeconvolutionFusion.constructor: Cannot create output image: " + conf.outputImageFactory.getClass().getSimpleName() );

				return;
			}

		}		
	}

	@Override
	public void fuseSPIMImages( final int channelIndex )
	{
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("Loading source images (Channel " + channelIndex +  ").");
		
		//
		// update views so that only the current channel is being fused
		//
		final ArrayList<ViewDataBeads> views = new ArrayList<ViewDataBeads>();
		
		for ( final ViewDataBeads view : viewStructure.getViews() )
			if ( view.getChannelIndex() == channelIndex )
				views.add( view );
		
		final int numViews = views.size();
		
		// this is only single channel for noew
		if ( channelIndex > 0 )
			return;
		
		// load images 
		for ( final ViewDataBeads view : views )
			view.getImage();
			
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN && isolatedWeightenerFactories.size() > 0 )
		{
			String methods = "(" + isolatedWeightenerFactories.get(0).getDescriptiveName();			
			for ( int i = 1; i < isolatedWeightenerFactories.size(); ++i )
				methods += ", " + isolatedWeightenerFactories.get(i).getDescriptiveName();			
			methods += ")";
			
			IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Init isolated weighteners for all views " + methods );
		}
		
		// init isolated pixel weighteners
		final AtomicInteger ai = new AtomicInteger(0);					
        Thread[] threads = SimpleMultiThreading.newThreads(conf.numberOfThreads);
        final int numThreads = threads.length;

        // compute them all in paralell ( computation done while opening )
		IsolatedPixelWeightener<?>[][] isoWinit = new IsolatedPixelWeightener<?>[ isolatedWeightenerFactories.size() ][ numViews ];
		for (int j = 0; j < isoWinit.length; j++)		
		{
			final int i = j;
			
			final IsolatedPixelWeightener<?>[][] isoW = isoWinit;
			
			for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                	final int myNumber = ai.getAndIncrement();
	                	
						for (int view = 0; view < numViews; view++)
							if ( view % numThreads == myNumber)
							{
								IOFunctions.println( "Computing " + isolatedWeightenerFactories.get( i ).getDescriptiveName() + " for " + views.get( view ) );
								isoW[i][view] = isolatedWeightenerFactories.get(i).createInstance( views.get(view) );
							}
	                }
	            });
			
			SimpleMultiThreading.startAndJoin( threads );
		}
		
		// test if the isolated weighteners were successfull...		
		try
		{
			boolean successful = true;
			for ( IsolatedPixelWeightener[] iso : isoWinit )
				for ( IsolatedPixelWeightener i : iso )
					if ( i == null )
						successful = false;
						
			if ( !successful )
			{
				IOFunctions.println( "Not enough memory for running the content-based fusion, running without it" );
				isoWinit = new IsolatedPixelWeightener[ 0 ][ 0 ];
			}
		}
		catch (Exception e)
		{				
			IOFunctions.println( "Not enough memory for running the content-based fusion, running without it" );
			isoWinit = new IsolatedPixelWeightener[ 0 ][ 0 ];
		}
		
		final IsolatedPixelWeightener<?>[][] isoW = isoWinit;
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Computing output image (Channel " + channelIndex +  ").");

		// cache the views, imageSizes and models that we use
		final boolean useView[] = new boolean[ numViews ];
		final AbstractAffineModel3D<?> models[] = new AbstractAffineModel3D[ numViews ];
		
		for ( int i = 0; i < numViews; ++i )
		{
			useView[ i ] = Math.max( views.get( i ).getViewErrorStatistics().getNumConnectedViews(), views.get( i ).getTile().getConnectedTiles().size() ) > 0 || views.get( i ).getViewStructure().getNumViews() == 1;
			
			// if a corresponding view that was used for registration is valid, this one is too
			if ( views.get( i ).getUseForRegistration() == false )
			{
				final int angle = views.get( i ).getAcqusitionAngle();
				final int timepoint = views.get( i ).getViewStructure().getTimePoint();
				
				for ( final ViewDataBeads view2 : viewStructure.getViews() )
					if ( view2.getAcqusitionAngle() == angle && timepoint == view2.getViewStructure().getTimePoint() && view2.getUseForRegistration() == true )
						useView[ i ] = true;
			}
			
			models[ i ] = (AbstractAffineModel3D<?>)views.get( i ).getTile().getModel(); 
		}
		
		final long[][] imageSizes = new long[numViews][];		
		for ( int i = 0; i < numViews; ++i )
			imageSizes[ i ] = views.get( i ).getImageSize();
		
		ai.set( 0 );					
        threads = SimpleMultiThreading.newThreads( numThreads );

		for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	try
                	{
                        final int myNumber = ai.getAndIncrement();

                        // temporary float array
		            	final float[] tmp = new float[ 3 ];
		            	
		        		// init combined pixel weighteners
		        		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN && combinedWeightenerFactories.size() > 0 )
		        		{
		        			String methods = "(" + combinedWeightenerFactories.get(0).getDescriptiveName();			
		        			for ( int i = 1; i < combinedWeightenerFactories.size(); ++i )
		        				methods += ", " + combinedWeightenerFactories.get(i).getDescriptiveName();			
		        			methods += ")";
		        			
		        			if ( myNumber == 0 )
		        				IOFunctions.println("Initialize combined weighteners for all views " + methods );
		        		}
		        		
		        		final CombinedPixelWeightener<?>[] combW = new CombinedPixelWeightener<?>[combinedWeightenerFactories.size()];
		        		for (int i = 0; i < combW.length; i++)
		        			combW[i] = combinedWeightenerFactories.get(i).createInstance( views );
		            	
						// get iterators for isolated weights
		        		final RandomAccess<FloatType> isoIterators[][] = new RandomAccess[ isoW.length ][ numViews ];            		
						for (int i = 0; i < isoW.length; i++)
							for (int view = 0; view < isoW[i].length; view++)
								isoIterators[i][view] = isoW[i][view].randomAccess();
		        		
		    			final Point3f[] tmpCoordinates = new Point3f[ numViews ];
		    			final int[][] loc = new int[ numViews ][ 3 ];
		    			final float[][] locf = new float[ numViews ][ 3 ];
		    			final boolean[] use = new boolean[ numViews ];
		    			
		    			for ( int i = 0; i < numViews; ++i )
		    				tmpCoordinates[ i ] = new Point3f();
		    			
		    			final Cursor<FloatType> outIntensity[] = new Cursor[ numViews ];
		    			final Cursor<FloatType> outWeights[] = new Cursor[ numViews ];
		    			
		    			final float[] tmpWeights = new float[ numViews ];
		    			
		    			for ( int i = 0; i < numViews; ++i )
		    			{
		    				outIntensity[ i ] = images[ i ].localizingCursor();
		    				outWeights[ i ] = weights[ i ].localizingCursor();
		    			}
		    			
		    			final Cursor<FloatType> firstCursor = outIntensity[ 0 ];
		    			
		    			// create Interpolated Iterators for the input images (every thread need own ones!)
		    			final RealRandomAccess<FloatType>[] interpolators = new RealRandomAccess[ numViews ];
		    			for (int view = 0; view < numViews ; view++)
		    			{
		    				final RandomAccessible< FloatType > r1 = Views.extend( views.get( view ).getImage( false ), conf.strategyFactoryOutput );
		    				final RealRandomAccessible< FloatType > r2 = Views.interpolate( r1, conf.interpolatorFactorOutput );
		    				interpolators[ view ] = r2.realRandomAccess(); 
		    				//interpolators[ view ] = views.get( view ).getImage().createInterpolator( conf.interpolatorFactorOutput );
		    			}
		    			
		    			while ( firstCursor.hasNext() )
		    			{
			    			for ( int i = 0; i < numViews; ++i )
			    			{
			    				outIntensity[ i ].fwd();
			    				outWeights[ i ].fwd();
			    			}
		    				
		        			if ( firstCursor.getIntPosition(2) % numThreads == myNumber )
		        			{
		        				// get the coordinates if cropped (all coordinates are the same, so we only use the first cursor)
		        				final int x = firstCursor.getIntPosition( 0 ) + cropOffsetX;
		        				final int y = firstCursor.getIntPosition( 1 ) + cropOffsetY;
		        				final int z = firstCursor.getIntPosition( 2 ) + cropOffsetZ;

		        				// how many view contribute at this position
								int num = 0;
								for ( int i = 0; i < numViews; ++i )
								{
									if ( useView[ i ] )
									{	            							
		    							tmpCoordinates[ i ].x = x * scale + min.x;
		    							tmpCoordinates[ i ].y = y * scale + min.y;
		    							tmpCoordinates[ i ].z = z * scale + min.z;
		
		    							mpicbg.spim.mpicbg.Java3d.applyInverseInPlace( models[i], tmpCoordinates[i], tmp );
			
		    							loc[i][0] = Util.round( tmpCoordinates[i].x );
		    							loc[i][1] = Util.round( tmpCoordinates[i].y );
		    							loc[i][2] = Util.round( tmpCoordinates[i].z );	

		    							locf[i][0] = tmpCoordinates[i].x;
		    							locf[i][1] = tmpCoordinates[i].y;
		    							locf[i][2] = tmpCoordinates[i].z;	

		    							// do we hit the source image?
										if ( loc[ i ][ 0 ] >= 0 && loc[ i ][ 1 ] >= 0 && loc[ i ][ 2 ] >= 0 && 
											 loc[ i ][ 0 ] < imageSizes[ i ][ 0 ] && 
											 loc[ i ][ 1 ] < imageSizes[ i ][ 1 ] && 
											 loc[ i ][ 2 ] < imageSizes[ i ][ 2 ] )
		    							{
		    								use[ i ] = true;
		    								++num;
		    							}	
		    							else
		    							{
		    								use[ i ] = false;
		    							}
									}
								}
		    				
								if ( num > 0 )
								{
		    						// update combined weighteners
									if ( combW.length > 0 )
										for ( final CombinedPixelWeightener<?> w : combW )
											w.updateWeights(locf, use);
		
									float sumWeights = 0;
		    						//float value = 0;
		
		    						for ( int view = 0; view < numViews; ++view )
		    						{
		    							if ( use[view] )
		    							{
		    								float weight = 1;
		    								
		    								// multiplicate combined weights
		    								if (combW.length > 0)
		    									for (final CombinedPixelWeightener<?> w : combW)
		    										weight *= w.getWeight(view);
		    								
		    								// multiplicate isolated weights
		    								for (int i = 0; i < isoW.length; i++)
		    								{
		    									isoIterators[ i ][ view ].setPosition( loc[ view ] );
												weight *= isoIterators[ i ][ view ].get().get();
		    								}        									      
		    								
		    								tmp[ 0 ] = tmpCoordinates[view].x;
		    								tmp[ 1 ] = tmpCoordinates[view].y;
		    								tmp[ 2 ] = tmpCoordinates[view].z;
		    								
		    								interpolators[view].setPosition( tmp );
		    								
		    								//value += weight * interpolators[view].getType().get(); 
		    								sumWeights += weight;
		    								
		    								// set the intensity, remember the weight
		    								tmpWeights[ view ] = weight;
		    								outIntensity[ view ].get().set( interpolators[view].get().get() );
		    							}
		    						}
		    						
		    						// set the normalized weights
		    						if ( sumWeights > 0 )
		    						{
			    						for ( int view = 0; view < numViews; ++view )
			    						{
			    							if ( use[view] )
			    							{
			    								outWeights[ view ].get().set( tmpWeights[ view ]/sumWeights );
			    							}
			    						}
		    						}
		    									
	    						}
	    						
	            			} // myThread loop       				
	        			} // iterator loop
	        			
		    			for ( int i = 0; i < numViews; ++i )
		    			{
		    				outIntensity[ i ] = null;
		    				outWeights[ i ] = null;
	        				interpolators[ i ] = null;
		    			}
		    			
	        			// close combined pixel weighteners
	        			for (int i = 0; i < combW.length; i++)
	        				combW[i].close();     
	        			
	        			// close isolated iterators
						for (int i = 0; i < isoW.length; i++)
							for (int view = 0; view < isoW[i].length; view++)
								isoIterators[i][view] = null;
         			
                	}
                	catch (NoninvertibleModelException e)
                	{
                		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
                			IOFunctions.println( "MappingFusionParalell(): Model not invertible for " + viewStructure );
                	}
                }// Thread.run loop
            });
        
        SimpleMultiThreading.startAndJoin(threads);	

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Closing all input images (Channel " + channelIndex +  ").");

		// do not unload images, we need them to extract the beads!
		//for ( final ViewDataBeads view : views ) 
		//	view.closeImage();
			
		// close weighteners		
		// close isolated pixel weighteners
		try
		{
			for (int i = 0; i < isoW.length; i++)
				for (int view = 0; view < numViews; view++)
					isoW[i][view].close();
		}
		catch (Exception e )
		{
			// this will fail if there was not enough memory...
		}
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Done computing output image (Channel " + channelIndex +  ").");
	}

	@Override
	public Img<FloatType> getFusedImage() { return null; }
	
	public Img<FloatType> getFusedImage( final int index ) { return images[ index ]; }
	public Img<FloatType> getWeightImage( final int index ) { return weights[ index ]; }
}
