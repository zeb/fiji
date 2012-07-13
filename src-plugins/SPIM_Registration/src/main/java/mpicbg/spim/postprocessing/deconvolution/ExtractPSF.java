package mpicbg.spim.postprocessing.deconvolution;

import ij.IJ;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.algorithm.legacy.transform.ImageTransform;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.CoordinateTransform;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.registration.bead.BeadRegistration;

public class ExtractPSF
{
	final ViewStructure viewStructure;
	final ArrayList<Img<FloatType>> pointSpreadFunctions;
	Img<FloatType> avgPSF;
	final boolean computeAveragePSF;
	
	int size = 19;
	boolean isotropic = false;
	
	public ExtractPSF( final SPIMConfiguration config, final boolean computeAveragePSF )
	{
		//
		// load the files
		//
		final ViewStructure viewStructure = ViewStructure.initViewStructure( config, 0, new AffineModel3D(), "ViewStructure Timepoint 0", config.debugLevelInt );						

		for ( ViewDataBeads view : viewStructure.getViews() )
		{
			view.loadDimensions();
			view.loadSegmentation();
			view.loadRegistration();

			BeadRegistration.concatenateAxialScaling( view, ViewStructure.DEBUG_MAIN );
		}
		
		this.viewStructure = viewStructure;
		this.pointSpreadFunctions = new ArrayList<Img<FloatType>>();
		this.computeAveragePSF = computeAveragePSF;
	}
	
	public ExtractPSF( final ViewStructure viewStructure, final boolean computeAveragePSF )
	{		
		this.viewStructure = viewStructure;
		this.pointSpreadFunctions = new ArrayList<Img<FloatType>>();
		this.computeAveragePSF = computeAveragePSF;
	}
	
	/**
	 * Defines the size of the PSF that is extracted
	 * 
	 * @param size - number of pixels in xy
	 * @param isotropic - if isotropic, than same size applies to z (in px), otherwise it is divided by half the z-stretching
	 */
	public void setPSFSize( final int size, final boolean isotropic )
	{
		this.size = size;
		this.isotropic = isotropic;
	}
	
	public ArrayList< Img< FloatType > > getPSFs() { return pointSpreadFunctions; }
	public Img< FloatType > getPSF( final int index ) { return pointSpreadFunctions.get( index ); }
	public Img< FloatType > getAveragePSF() { return avgPSF; }
	
	/**
	 * Get projection along the smallest dimension (which is usually the rotation axis)
	 * 
	 * @return - the averaged, projected PSF
	 */
	public Img< FloatType > getMaxProjectionAveragePSF()
	{
		final long[] dimensions = new long[ avgPSF.numDimensions() ];
		avgPSF.dimensions( dimensions );
		
		int minSize = (int)dimensions[ 0 ];
		int minDim = 0;
		
		for ( int d = 0; d < dimensions.length; ++d )
		{
			if ( avgPSF.dimension( d ) < minSize )
			{
				minSize = (int)avgPSF.dimension( d );
				minDim = d;
			}
		}
		
		final int[] projDim = new int[ dimensions.length - 1 ];
		
		int dim = 0;
		int sizeProjection = 0;
		
		// the new dimensions
		for ( int d = 0; d < dimensions.length; ++d )
			if ( d != minDim )
				projDim[ dim++ ] = (int)dimensions[ d ];
			else
				sizeProjection = (int)dimensions[ d ];
		
		final Img< FloatType > proj = avgPSF.factory().create( projDim, new FloatType() );
		
		final RandomAccess< FloatType > psfIterator = avgPSF.randomAccess();
		final Cursor< FloatType > projIterator = proj.localizingCursor();
		
		final int[] tmp = new int[ avgPSF.numDimensions() ];
		
		while ( projIterator.hasNext() )
		{
			projIterator.fwd();

			dim = 0;
			for ( int d = 0; d < dimensions.length; ++d )
				if ( d != minDim )
					tmp[ d ] = projIterator.getIntPosition( dim++ );

			tmp[ minDim ] = -1;
			
			float maxValue = -Float.MAX_VALUE;
			
			psfIterator.setPosition( tmp );
			for ( int i = 0; i < sizeProjection; ++i )
			{
				psfIterator.fwd( minDim );
				final float value = psfIterator.get().get();
				
				if ( value > maxValue )
					maxValue = value;
			}
			
			projIterator.get().set( maxValue );
		}
		
		return proj;
	}
	
	public void extract()
	{
		final ArrayList<ViewDataBeads > views = viewStructure.getViews();
		final int numDimensions = 3;
		
		final int[] size = Util.getArrayFromValue( this.size, numDimensions );
		if ( !this.isotropic )
			size[ numDimensions - 1 ] /= Math.max( 1, views.get( 0 ).getZStretching()/2 );
		
		//IJ.log ( Util.printCoordinates( size ) );
		
		final int[] maxSize = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
			maxSize[ d ] = 0;
		
		for ( final ViewDataBeads view : views )		
		{			
			final Img<FloatType> psf = getTransformedPSF(view, size); 
			
			for ( int d = 0; d < numDimensions; ++d )
				if ( psf.dimension( d ) > maxSize[ d ] )
					maxSize[ d ] = (int)psf.dimension( d );
			
			pointSpreadFunctions.add( psf );
		}
		
		
		if ( computeAveragePSF )
		{
			avgPSF = pointSpreadFunctions.get( 0 ).factory().create( maxSize, new FloatType() );
			
			final int[] avgCenter = new int[ numDimensions ];		
			for ( int d = 0; d < numDimensions; ++d )
				avgCenter[ d ] = (int)avgPSF.dimension( d ) / 2;
				
			for ( final Img<FloatType> psf : pointSpreadFunctions )
			{
				final RandomAccess<FloatType> avgCursor = avgPSF.randomAccess();
				final Cursor<FloatType> psfCursor = psf.localizingCursor();
				
				final int[] loc = new int[ numDimensions ];
				final int[] psfCenter = new int[ numDimensions ];		
				for ( int d = 0; d < numDimensions; ++d )
					psfCenter[ d ] = (int)psf.dimension( d ) / 2;
				
				while ( psfCursor.hasNext() )
				{
					psfCursor.fwd();
					psfCursor.localize( loc );
					
					for ( int d = 0; d < numDimensions; ++d )
						loc[ d ] = psfCenter[ d ] - loc[ d ] + avgCenter[ d ];
					
					avgCursor.setPosition( loc );
					avgCursor.get().add( psfCursor.get() );				
				}
			}
		}
	}
	
	public static Img<FloatType> getTransformedPSF( final ViewDataBeads view, final int[] size )
	{
		final Img<FloatType> psf = extractPSF( view, size );
		return transformPSF( psf, (AbstractAffineModel3D<?>)view.getTile().getModel() );		
	}
	
	/**
	 * Transforms the extracted PSF using the affine transformation of the corresponding view
	 * 
	 * @param psf - the extracted psf (NOT z-scaling corrected)
	 * @param model - the transformation model
	 * @return the transformed psf which has odd sizes and where the center of the psf is also the center of the transformed psf
	 */
	protected static Img<FloatType> transformPSF( final Img<FloatType> psf, final AbstractAffineModel3D<?> model )
	{
		// here we compute a slightly different transformation than the ImageTransform does
		// two things are necessary:
		// a) the center pixel stays the center pixel
		// b) the transformed psf has a odd size in all dimensions
		
		final int numDimensions = psf.numDimensions();
		
		final int[] dimensions = new int[ numDimensions ];
		for ( int d = 0; d < numDimensions; ++d )
			dimensions[ d ] = (int)psf.dimension( d );
		
		final float[][] minMaxDim = ExtractPSF.getMinMaxDim( dimensions, model );
		final float[] size = new float[ numDimensions ];		
		final int[] newSize = new int[ numDimensions ];		
		final float[] offset = new float[ numDimensions ];
		
		// the center of the psf has to be the center of the transformed psf as well
		// this is important!
		final float[] center = new float[ numDimensions ]; 
		
		for ( int d = 0; d < numDimensions; ++d )
			center[ d ] = psf.dimension( d ) / 2;
		
		model.applyInPlace( center );

		for ( int d = 0; d < numDimensions; ++d )
		{						
			size[ d ] = minMaxDim[ d ][ 1 ] - minMaxDim[ d ][ 0 ];
			
			newSize[ d ] = (int)size[ d ] + 3;
			if ( newSize[ d ] % 2 == 0 )
				++newSize[ d ];
				
			// the offset is defined like this:
			// the transformed coordinates of the center of the psf
			// are the center of the transformed psf
			offset[ d ] = center[ d ] - newSize[ d ]/2;
			
			//System.out.println( MathLib.printCoordinates( minMaxDim[ d ] ) + " size " + size[ d ] + " newSize " + newSize[ d ] );
		}
		// new OutOfBoundsConstantValueFactory<FloatType, RandomAccessibleInterval<FloatType> > ( new FloatType() )
		//new NLinearInterpolatorFactory<FloatType>()
		final ImageTransform<FloatType> transform = new ImageTransform<FloatType>( psf, model, new NLinearInterpolatorFactory<FloatType>(), new OutOfBoundsConstantValueFactory<FloatType, RandomAccessibleInterval<FloatType> > ( new FloatType() ) );
		transform.setOffset( offset );
		transform.setNewImageSize( newSize );
		
		if ( !transform.checkInput() || !transform.process() )
		{
			System.out.println( "Error transforming psf: " + transform.getErrorMessage() );
			return null;
		}
		
		final Img<FloatType> transformedPSF = transform.getResult();
		
		ViewDataBeads.normalizeImage( transformedPSF, "transformedPSF" );
		
		return transformedPSF;
	}
		
	/**
	 * Extracts the PSF by averaging the local neighborhood RANSAC correspondences
	 * @param view - the SPIM view
	 * @param size - the size in which the psf is extracted (in pixel units, z-scaling is ignored)
	 * @return - the psf, NOT z-scaling corrected
	 */
	protected static Img<FloatType> extractPSF( final ViewDataBeads view, final int[] size )
	{
		final int numDimensions = size.length;
		
		final ImgFactory<FloatType> imageFactory = new ArrayImgFactory<FloatType>();
		final Img<FloatType> img = view.getImage();
		final Img<FloatType> psf = imageFactory.create( size, new FloatType() );
		
		final RealRandomAccess<FloatType> interpolator = Views.interpolate( Views.extendMirrorSingle( img ), new NLinearInterpolatorFactory<FloatType>() ).realRandomAccess();
		final Cursor<FloatType> psfCursor = psf.localizingCursor();
		
		final int[] sizeHalf = size.clone();		
		for ( int d = 0; d < numDimensions; ++d )
			sizeHalf[ d ] /= 2;
		
		int numRANSACBeads = 0;
		
		for ( final Bead bead : view.getBeadStructure().getBeadList() )
		{			
			final float[] position = bead.getL().clone();
			final int[] tmpI = new int[ position.length ];
			final float[] tmpF = new float[ position.length ];
			
			// check if it is a true correspondence
			if ( bead.getRANSACCorrespondence().size() > 0 ) 
			{
				++numRANSACBeads;
				psfCursor.reset();
				
				while ( psfCursor.hasNext() )
				{
					psfCursor.fwd();
					psfCursor.localize( tmpI );

					for ( int d = 0; d < numDimensions; ++d )
						tmpF[ d ] = tmpI[ d ] - sizeHalf[ d ] + position[ d ];
					
					interpolator.setPosition( tmpF );
					
					psfCursor.get().add( interpolator.get() );
				}
			}
		}

		// compute the average		
		final FloatType n = new FloatType( numRANSACBeads );
		
		psfCursor.reset();
		while ( psfCursor.hasNext() )
		{
			psfCursor.fwd();
			psfCursor.get().div( n );			
		}	
	
		ViewDataBeads.normalizeImage( psf, "PSF" );
		
		return psf;
	}

	private static float[][] getMinMaxDim( final int[] dimensions, final CoordinateTransform transform )
	{
		final int numDimensions = dimensions.length;
		
		final float[] tmp = new float[ numDimensions ];
		final float[][] minMaxDim = new float[ numDimensions ][ 2 ];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			minMaxDim[ d ][ 0 ] = Float.MAX_VALUE;
			minMaxDim[ d ][ 1 ] = -Float.MAX_VALUE;
		}
		
		// recursively get all corner points of the image, assuming they will still be the extremum points
		// in the transformed image
		final boolean[][] positions = new boolean[ Util.pow( 2, numDimensions ) ][ numDimensions ];
		Util.setCoordinateRecursive( numDimensions - 1, numDimensions, new int[ numDimensions ], positions );
		
		// get the min and max location for each dimension independently  
		for ( int i = 0; i < positions.length; ++i )
		{
			for ( int d = 0; d < numDimensions; ++d )
			{
				if ( positions[ i ][ d ])
					tmp[ d ] = dimensions[ d ] - 1;
				else
					tmp[ d ] = 0;
			}
			
			transform.applyInPlace( tmp );
			
			for ( int d = 0; d < numDimensions; ++d )
			{				
				if ( tmp[ d ] < minMaxDim[ d ][ 0 ]) 
					minMaxDim[ d ][ 0 ] = tmp[ d ];

				if ( tmp[ d ] > minMaxDim[ d ][ 1 ]) 
					minMaxDim[ d ][ 1 ] = tmp[ d ];
			}				
		}
		
		return minMaxDim;
	}		
}
