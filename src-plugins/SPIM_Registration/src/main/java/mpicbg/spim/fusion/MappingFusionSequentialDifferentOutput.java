package mpicbg.spim.fusion;

import ij.ImagePlus;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3f;

import net.imglib2.Cursor;
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
import mpicbg.spim.io.ImgLibSaver;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class MappingFusionSequentialDifferentOutput extends SPIMImageFusion
{
	final Img<FloatType> fusedImages[];
	final int numViews;

	//int angleIndicies[] = new int[]{ 0, 6, 7 };
	int angleIndicies[] = null;

	public MappingFusionSequentialDifferentOutput( final ViewStructure viewStructure, final ViewStructure referenceViewStructure,
			  									   final ArrayList<IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories,
			  									   final ArrayList<CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories )
	{
		super( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories );

		numViews = viewStructure.getNumViews();

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Reserving memory for fused images.");

		if ( angleIndicies == null )
		{
			angleIndicies = new int[ numViews ];

			for ( int view = 0; view < numViews; view++ )
				angleIndicies[ view ] = view;
		}

		fusedImages = new Img[ angleIndicies.length ];
		final ImgFactory<FloatType> fusedImageFactory = conf.outputImageFactory;

		final long size = (4l * imgW * imgH * imgD)/(1000l*1000l);

		for (int i = 0; i < angleIndicies.length; i++)
		{
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Reserving " + size + " MiB for '" + viewStructure.getViews().get( angleIndicies[i] ).getName() + "'" );

			fusedImages[ i ] = fusedImageFactory.create( new int[]{ imgW, imgH, imgD }, new FloatType() );

			if ( fusedImages[i] == null && viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.printErr("MappingFusionSequentialDifferentOutput.constructor: Cannot create output image: " + conf.outputImageFactory );
		}
	}

	@Override
	public void fuseSPIMImages( final int channelIndex )
	{
		// here we do all at once
		if ( channelIndex > 0 )
			return;

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Unloading source images.");

		//
		// get all views of all channels
		//
		final ArrayList<ViewDataBeads> views = viewStructure.getViews();
		final int numViews = views.size();

		// unload images
		for ( final ViewDataBeads view : views )
			view.closeImage();

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Computing output image.");

		// open input images
		for ( int viewIndex = 0; viewIndex < angleIndicies.length; viewIndex++ )
			views.get( angleIndicies[ viewIndex ] ).getImage( false );

		// compute output images in paralell
		final AtomicInteger ai = new AtomicInteger(0);
        final Thread[] threads = SimpleMultiThreading.newThreads(conf.numberOfThreads);
        final int numThreads = threads.length;

		for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                @Override
				public void run()
                {
                	final int myNumber = ai.getAndIncrement();

					for ( int viewIndex = 0; viewIndex < angleIndicies.length; viewIndex++ )
						if ( viewIndex % numThreads == myNumber)
						{
							final ViewDataBeads view = views.get( angleIndicies[ viewIndex ] );

							IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Computing individual registered image for '" + view.getName() + "'" );

							if ( Math.max( view.getTile().getConnectedTiles().size(), view.getViewErrorStatistics().getNumConnectedViews() ) <= 0 && view.getViewStructure().getNumViews() > 1 )
							{
								if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
									IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Cannot use view '" + view.getName() + ", view is not connected to any other view.");

								continue;
							}

							// load the current image
							if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
								IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Loading view: " + view.getName());

							final RandomAccessible< FloatType > r1 = Views.extend( view.getImage( false ), conf.strategyFactoryOutput );
		    				final RealRandomAccessible< FloatType > r2 = Views.interpolate( r1, conf.interpolatorFactorOutput );
							final RealRandomAccess<FloatType> interpolator = r2.realRandomAccess();

							final Point3f tmpCoordinates = new Point3f();

							final long[] imageSize = view.getImageSize();
							final long w = imageSize[ 0 ];
							final long h = imageSize[ 1 ];
							final long d = imageSize[ 2 ];

							final AbstractAffineModel3D<?> model = (AbstractAffineModel3D<?>)view.getTile().getModel();

							// temporary float array
				        	final float[] tmp = new float[ 3 ];

				    		final CombinedPixelWeightener<?>[] combW = new CombinedPixelWeightener<?>[combinedWeightenerFactories.size()];
				    		for (int i = 0; i < combW.length; i++)
				    			combW[i] = combinedWeightenerFactories.get(i).createInstance( views );

							final float[][] loc = new float[ numViews ][ 3 ];
							final boolean[] use = new boolean[ numViews ];

							for ( int v = 0; v < numViews; ++v )
							{
								use[ v ] = true;
								for ( int i = 0; i < 3; ++i )
									loc[ v ][ i ] = viewStructure.getViews().get( v ).getImageSize()[ i ] / 2;
							}

							if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
								IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting fusion for: " + view.getName());

							final Cursor<FloatType> iteratorFused = fusedImages[ viewIndex ].localizingCursor();

							try
							{
								while (iteratorFused.hasNext())
								{
									iteratorFused.next();

									// get the coordinates if cropped
									final int x = iteratorFused.getIntPosition(0) + cropOffsetX;
									final int y = iteratorFused.getIntPosition(1) + cropOffsetY;
									final int z = iteratorFused.getIntPosition(2) + cropOffsetZ;

									tmpCoordinates.x = x * scale + min.x;
									tmpCoordinates.y = y * scale + min.y;
									tmpCoordinates.z = z * scale + min.z;

									mpicbg.spim.mpicbg.Java3d.applyInverseInPlace( model, tmpCoordinates, tmp );

									final int locX = Util.round( tmpCoordinates.x );
									final int locY = Util.round( tmpCoordinates.y );
									final int locZ = Util.round( tmpCoordinates.z );

									// do we hit the source image?
									if (locX >= 0 && locY >= 0 && locZ >= 0 &&
										locX < w  && locY < h  && locZ < d )
										{
											float weight = 1;

											// update combined weighteners
											if (combW.length > 0)
											{
												loc[ viewIndex ][ 0 ] = tmpCoordinates.x;
												loc[ viewIndex ][ 1 ] = tmpCoordinates.y;
												loc[ viewIndex ][ 2 ] = tmpCoordinates.z;

												for (final CombinedPixelWeightener<?> we : combW)
													we.updateWeights( loc, use );

												for (final CombinedPixelWeightener<?> we : combW)
													weight *= we.getWeight( viewIndex );
											}

											tmp[ 0 ] = tmpCoordinates.x;
											tmp[ 1 ] = tmpCoordinates.y;
											tmp[ 2 ] = tmpCoordinates.z;

											interpolator.setPosition( tmp );

											final float intensity = interpolator.get().get();
											iteratorFused.get().set( intensity * weight );
										}
								}
							}
				        	catch (final NoninvertibleModelException e)
				        	{
				        		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
				        			IOFunctions.println( "MappingFusionSequentialDifferentOutput(): Model not invertible for " + viewStructure );
				        	}

							// unload input image
							view.closeImage();
						}
                }
            });

		SimpleMultiThreading.startAndJoin( threads );

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Done computing output image.");
	}

	@Override
	public Img<FloatType> getFusedImage() { return fusedImages[ 0 ]; }

	public Img<FloatType> getFusedImage( final int index ) { return fusedImages[ index ]; }

	@Override
	public boolean saveAsTiffs( final String dir, final String name, final int channelIndex )
	{
		if ( channelIndex > 0 )
			return true;

		boolean success = true;

		for ( int i = 0; i < fusedImages.length; i++ )
		{
			final ViewDataBeads view = viewStructure.getViews().get( angleIndicies[ i ] );

			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Saving '" + name + "_ch" + view.getChannel() + "_angle" + view.getAcqusitionAngle() + "'" );

			success &= ImgLibSaver.saveAsTiffs( fusedImages[ i ], dir, name + "_ch" + view.getChannel() + "_angle" + view.getAcqusitionAngle() );
		}

		return success;
	}
}
