package mpicbg.spim.registration.bead;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.legacy.scalespace.DifferenceOfGaussian;
import net.imglib2.algorithm.legacy.scalespace.DifferenceOfGaussianPeak;
import net.imglib2.algorithm.legacy.scalespace.SubpixelLocalization;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.laplace.LaPlaceFunctions;
import mpicbg.spim.registration.threshold.ComponentProperties;
import mpicbg.spim.registration.threshold.ConnectedComponent;

public class BeadSegmentation
{	
	public static final boolean debugBeads = false;
	public ViewStructure viewStructure;
	
	public BeadSegmentation( final ViewStructure viewStructure ) 
	{
		this.viewStructure = viewStructure;
	}	
	
	public void segment( )
	{
		segment( viewStructure.getSPIMConfiguration(), viewStructure.getViews() );
	}
	
	public void segment( final SPIMConfiguration conf, final ArrayList<ViewDataBeads> views )
	{
		final float threshold = conf.threshold;
				
		//
		// Extract the beads
		// 		
		if ( conf.multiThreadedOpening )
		{
			final int numThreads = views.size();
			
			for ( final ViewDataBeads view : views )
				if ( view.getUseForRegistration() )
					view.getImage();

			final AtomicInteger ai = new AtomicInteger(0);					
	        Thread[] threads = SimpleMultiThreading.newThreads( numThreads );

			for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                    final int myNumber = ai.getAndIncrement();

	                    final ViewDataBeads view = views.get( myNumber );
	                    
	                    if ( view.getUseForRegistration() )
	                    {	                    
		        			if (conf.useScaleSpace)					
		        			{
		        	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		        	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Scale Space Bead Extraction for " + view.getName() );
		        				
		        	    		view.setBeadStructure( extractBeadsLaPlaceImgLib( view, conf ) );

		        				view.closeImage();
		        			}
		        			else
		        			{
		        	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		        	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Threshold Bead Extraction");					
		        				
		        				view.setBeadStructure( extractBeadsThresholdSegmentation( view, threshold, conf.minSize, conf.maxSize, conf.minBlackBorder) );
		        				
		        				view.closeImage();				
		        			}
		        				
		        			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		        				IOFunctions.println( "Found peaks (possible beads): " + view.getBeadStructure().getBeadList().size() );
		        			
		        			//
		        			// Store segmentation in a file
		        			//
		        			if ( conf.writeSegmentation )
		        				IOFunctions.writeSegmentation( view, conf.registrationFiledirectory );
	                    }
	                }
	            });
			
			SimpleMultiThreading.startAndJoin( threads );
		}
		else
		{		
			for ( final ViewDataBeads view : views )
			{
				if (conf.useScaleSpace)					
				{
		    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Scale Space Bead Extraction for " + view.getName() );
					
		    		view.setBeadStructure( extractBeadsLaPlaceImgLib( view, conf ) );
		    		
		    		if ( debugBeads )
		    		{
						Img<FloatType> img = getFoundBeads( view );				
						ImageJFunctions.show( img );				
						SimpleMultiThreading.threadHaltUnClean();		    			
		    		}
		    		
					view.closeImage();
				}
				else
				{
		    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Threshold Bead Extraction");					
					
					view.setBeadStructure( extractBeadsThresholdSegmentation( view, threshold, conf.minSize, conf.maxSize, conf.minBlackBorder) );
					
					view.closeImage();				
				}
					
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
					IOFunctions.println( "Found peaks (possible beads): " + view.getBeadStructure().getBeadList().size() );
				
				//
				// Store segmentation in a file
				//
				if ( conf.writeSegmentation )
					IOFunctions.writeSegmentation( view, conf.registrationFiledirectory );										
			}
		}
	}
		
	public Img<FloatType> getFoundBeads( final ViewDataBeads view )
	{
		// display found beads
		ImgFactory<FloatType> factory = view.getViewStructure().getSPIMConfiguration().imageFactory;
		Img<FloatType> img = factory.create( view.getImageSize(), new FloatType() );
		RandomAccessible< FloatType > r = Views.extendValue( img , new FloatType() );
		
		for ( Bead bead : view.getBeadStructure().getBeadList())
		{
			final float[] pos = bead.getL();
			final Point p = new Point( pos.length );
			
			for ( int i = 0; i < pos.length; ++i )
				p.setPosition( Math.round( pos[ i ] ), i );
				
			final HyperSphere< FloatType > hs = new HyperSphere<FloatType>( r, p, 1 );
			
			for ( final FloatType f : hs )
				f.setOne();			
		}
	
		return img;
	}
			
	protected BeadStructure extractBeadsLaPlaceImgLib( final ViewDataBeads view, final SPIMConfiguration conf )
	{
		// load the image
		final ImgPlus<FloatType> img = view.getImage();

        float imageSigma = conf.imageSigma;
        float initialSigma = view.getInitialSigma();

        final float minPeakValue;
        final float minInitialPeakValue;

        // adjust for 12bit images
        // we stop doing that for now...
        if ( view.getMaxValueUnnormed() > 256 )
        {
		minPeakValue = view.getMinPeakValue();///3;
		minInitialPeakValue = view.getMinInitialPeakValue();///3;
        }
        else
        {
            minPeakValue = view.getMinPeakValue();
            minInitialPeakValue = view.getMinInitialPeakValue();
        }        

        IOFunctions.println( view.getName() + " sigma: " + initialSigma + " minPeakValue: " + minPeakValue );

        final float k = LaPlaceFunctions.computeK(conf.stepsPerOctave);
        final float K_MIN1_INV = LaPlaceFunctions.computeKWeight(k);
        final int steps = conf.steps;

        //
        // Compute the Sigmas for the gaussian folding
        //
        final float[] sigma = LaPlaceFunctions.computeSigma(steps, k, initialSigma);
        final float[] sigmaDiff = LaPlaceFunctions.computeSigmaDiff(sigma, imageSigma);
         
		// compute difference of gaussian
		final DifferenceOfGaussian<FloatType> dog = new DifferenceOfGaussian<FloatType>( img.getImg(), conf.imageFactory, conf.strategyFactoryGauss, sigmaDiff[0], sigmaDiff[1], minInitialPeakValue, K_MIN1_INV );
		dog.setKeepDoGImg( true );
		
		if ( !dog.checkInput() || !dog.process() )
		{
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Cannot compute difference of gaussian for " + dog.getErrorMessage() );
			
			return new BeadStructure();
		}

		// remove all minima
        final ArrayList< DifferenceOfGaussianPeak<FloatType> > peakList = dog.getPeaks();
        for ( int i = peakList.size() - 1; i >= 0; --i )
        	if ( peakList.get( i ).isMin() )
        		peakList.remove( i );
		
		final SubpixelLocalization<FloatType> spl = new SubpixelLocalization<FloatType>( dog.getDoGImg(), dog.getPeaks() );
		spl.setAllowMaximaTolerance( true );
		spl.setMaxNumMoves( 10 );
		
		if ( !spl.checkInput() || !spl.process() )
		{
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Warning! Failed to compute subpixel localization " + spl.getErrorMessage() );
		}
			
        final BeadStructure beads = new BeadStructure();
        int id = 0;
        final float[] pos = new float[ img.numDimensions() ];
        
        int peakTooLow = 0;
        int invalid = 0;
        int max = 0;
                
        for ( DifferenceOfGaussianPeak<FloatType> maximum : dog.getPeaks() )
        {
        	if ( !maximum.isValid() )
        		invalid++;
        	if ( maximum.isMax() )
        		max++;
        	
        	if ( maximum.isMax() ) 
        	{
        		if ( Math.abs( maximum.getValue().get() ) >= minPeakValue )
        		{
	        		maximum.getSubPixelPosition( pos );
		        	final Bead bead = new Bead( id, new Point3d( pos[ 0 ], pos[ 1 ], pos[ 2 ] ), view );
		        	beads.addDetection( bead );
		        	id++;
        		}
        		else
        		{
        			peakTooLow++;
        		}
        	}
        }
        
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
		{
	        IOFunctions.println( "number of peaks: " + dog.getPeaks().size() );        
	        IOFunctions.println( "invalid: " + invalid );
	        IOFunctions.println( "max: " + max );
	        IOFunctions.println( "peak to low: " + peakTooLow );
		}
		
		return beads;
		
	}
	
	protected BeadStructure extractBeadsThresholdSegmentation( final ViewDataBeads view, final float thresholdI, final int minSize, final int maxSize, final int minBlackBorder)
	{
		final SPIMConfiguration conf = view.getViewStructure().getSPIMConfiguration();
		final Img<FloatType> img  = view.getImage( false );
		
		//
		// Extract connected components
		//
		
		ImgFactory<IntType> imageFactory;
		try 
		{
			imageFactory = img.factory().imgFactory( new IntType() );
		} 
		catch (IncompatibleTypeException e1) 
		{
			imageFactory = new CellImgFactory<IntType>( 256 );
		}
		final Img<IntType> connectedComponents = imageFactory.create( img, new IntType() );

		int label = 0;
		
		final int maxValue = (int) Math.round( view.getMaxValueUnnormed() );

		final float thresholdImg;

		if ( !conf.useFixedThreshold )
		{
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
    			IOFunctions.println( view.getName() + " maximum value: " + maxValue + " means a threshold of " + thresholdI*maxValue );
    		
			thresholdImg = thresholdI*maxValue;
		}
		else
		{
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
    			IOFunctions.println( view.getName() + " maximum value: " + maxValue + " using a threshold of " + conf.fixedThreshold );
    		
			thresholdImg = conf.fixedThreshold;
		}
		
		final ArrayList<Point3i> neighbors = getVisitedNeighbors();
		final ConnectedComponent components = new ConnectedComponent();
		
		final long w = connectedComponents.dimension( 0 );
		final long h = connectedComponents.dimension( 1 );
		final long d = connectedComponents.dimension( 2 );

		final RandomAccess<FloatType> cursorImg = img.randomAccess();
		final RandomAccess<IntType> cursorComponents = connectedComponents.randomAccess();
		
		final int[] tmp = new int[ 3 ];
		
		for (int z = 0; z < d; z++)
		{
			//IOFunctions.println("Processing z: " + z);

			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
				{
					tmp[ 0 ] = x; tmp[ 1 ] = y; tmp[ 2 ] = z;
					cursorImg.setPosition( tmp );
					
					// is above threshold?
					if ( cursorImg.get().get() > thresholdImg)
					{
						// check previously visited neighboring pixels if they
						// have a label assigned
						ArrayList<Integer> neighboringLabels = getNeighboringLabels(connectedComponents, neighbors, x, y, z);

						// if there are no neighors, introduce new label
						if (neighboringLabels == null || neighboringLabels.size() == 0)
						{
							label++;
							tmp[ 0 ] = x; tmp[ 1 ] = y; tmp[ 2 ] = z;
							cursorComponents.setPosition( tmp );
							cursorComponents.get().set( label );
							components.addLabel(label);
						}
						else if (neighboringLabels.size() == 1) 
						// if there is only one neighboring label, set this one
						{
							cursorComponents.get().set( neighboringLabels.get(0) );
						}
						else
						// remember all the labels are actually the same
						// because they touch
						{
							int label1 = neighboringLabels.get(0);

							try
							{
								for (int i = 1; i < neighboringLabels.size(); i++)
									components.addEqualLabels(label1, neighboringLabels.get(i));
							}
							catch (Exception e)
							{
								e.printStackTrace();
								IOFunctions.printErr("\n" + x + " " + y);
								System.exit(0);
							}

							cursorComponents.get().set( label1 );
						}
					}
				}
		}
				
		// merge components
		components.equalizeLabels(connectedComponents);
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("Found " + components.distinctLabels.size() + " distinct labels out of " + label + " labels");
		
		// remove invalid components
		ArrayList<ComponentProperties> segmentedBeads = components.getBeads(connectedComponents, img, minSize, maxSize, minBlackBorder, conf.useCenterOfMass, conf.circularityFactor);
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("Fount Beads: " + segmentedBeads.size());

		final BeadStructure beads = new BeadStructure();
		int id = 0;
		
		for ( final ComponentProperties comp : segmentedBeads )
		{
			Bead bead = new Bead( id, comp.center, view );
			beads.addDetection( bead );
			id++;
		}
		
		return beads;
	}
	
	protected ArrayList<Integer> getNeighboringLabels( final Img<IntType> connectedComponents, final ArrayList<Point3i> neighbors, final int x, final int y, final int z )
	{
		final ArrayList<Integer> labels = new ArrayList<Integer>();
		final Iterator<Point3i> iterateNeighbors = neighbors.iterator();
		
		final long w = connectedComponents.dimension( 0 );
		final long h = connectedComponents.dimension( 1 );
		final long d = connectedComponents.dimension( 2 );
		
		final RandomAccess<IntType> cursor = connectedComponents.randomAccess();
		final int[] tmp = new int[ 3 ];
		
		while (iterateNeighbors.hasNext())
		{
			Point3i neighbor = iterateNeighbors.next();

			int xp = x + neighbor.x;
			int yp = y + neighbor.y;
			int zp = z + neighbor.z;

			if (xp >= 0 && yp >= 0 && zp >= 0 && xp < w && yp < h && zp < d )
			{
				tmp[ 0 ] = xp; tmp[ 1 ] = yp; tmp[ 2 ] = zp;
				cursor.setPosition( tmp );
				int label = cursor.get().get();

				if (label != 0 && !labels.contains(neighbor)) 
					labels.add(label);
			}
		}

		return labels;
	}

	protected ArrayList<Point3i> getVisitedNeighbors()
	{
		ArrayList<Point3i> visitedNeighbors = new ArrayList<Point3i>();

		int z = -1;

		for (int y = -1; y <= 1; y++)
			for (int x = -1; x <= 1; x++)
				visitedNeighbors.add(new Point3i(x, y, z));

		visitedNeighbors.add(new Point3i(-1, 0, 0));
		visitedNeighbors.add(new Point3i(-1, -1, 0));
		visitedNeighbors.add(new Point3i(0, -1, 0));
		visitedNeighbors.add(new Point3i(1, -1, 0));

		return visitedNeighbors;
	}	
	
	
}
