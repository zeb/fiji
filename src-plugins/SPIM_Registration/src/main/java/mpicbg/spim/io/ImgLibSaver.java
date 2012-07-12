package mpicbg.spim.io;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.iterator.ZeroMinIntervalIterator;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.FloatType;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * 
 * @author Stephan Preibisch
 *
 */
public class ImgLibSaver 
{
	public static boolean saveAsTiffs( final Img< FloatType > img, String directory, final String name )
	{
		boolean everythingOK = true;

		if ( directory == null )
			directory = "";

		directory = directory.replace('\\', '/');
		directory = directory.trim();
		if (directory.length() > 0 && !directory.endsWith("/"))
			directory = directory + "/";

		final int numDimensions = img.numDimensions();

		final int[] dimensionPositions = new int[ numDimensions ];

		// x dimension for save is x
		final int dimX = 0;
		// y dimensins for save is y
		final int dimY = 1;

		if ( numDimensions <= 2 )
		{
			final ImageProcessor ip = new FloatProcessor( (int)img.dimension( dimX ), (int)img.dimension( dimY ), extractSliceFloat( img, dimX, dimY, dimensionPositions ), null);
			final ImagePlus slice = new ImagePlus( name + ".tif", ip);
        	final FileSaver fs = new FileSaver( slice );
        	everythingOK = everythingOK && fs.saveAsTiff(directory + slice.getTitle());

        	slice.close();
		}
		else // n dimensions
		{
			final long extraDimensions[] = new long[ numDimensions - 2 ];
			final int extraDimPos[] = new int[ extraDimensions.length ];

			for ( int d = 2; d < numDimensions; ++d )
				extraDimensions[ d - 2 ] = (int)img.dimension( d );

			// the max number of digits for each dimension
			final int maxLengthDim[] = new int[ extraDimensions.length ];

			for ( int d = 2; d < numDimensions; ++d )
			{
				final String num = "" + (img.dimension( d ) - 1);
				maxLengthDim[ d - 2 ] = num.length();
			}

			//
			// Here we "misuse" a ArrayLocalizableCursor to iterate through the dimensions (d > 2),
			// he will iterate all dimensions as we want ( iterate through d=3, inc 4, iterate through 3, inc 4, ... )
			//
			final ZeroMinIntervalIterator cursor = new ZeroMinIntervalIterator( extraDimensions );
			
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.localize( extraDimPos );

				for ( int d = 2; d < numDimensions; ++d )
					dimensionPositions[ d ] = extraDimPos[ d - 2 ];

				final ImageProcessor ip = new FloatProcessor( (int)img.dimension( dimX ), (int)img.dimension( dimY ), extractSliceFloat( img, dimX, dimY, dimensionPositions ), null);

	        	String desc = "";

				for ( int d = 2; d < numDimensions; ++d )
				{
		        	String descDim = "" + dimensionPositions[ d ];
		        	while( descDim.length() < maxLengthDim[ d - 2 ] )
		        		descDim = "0" + descDim;
		        	
		        	if ( d == 2 )
		        		desc = desc + "_z" + descDim;
		        	else
		        		desc = desc + "_" + descDim;
				}

	        	final ImagePlus slice = new ImagePlus( name + desc + ".tif", ip);

	        	final FileSaver fs = new FileSaver( slice );
	        	everythingOK = everythingOK && fs.saveAsTiff(directory + slice.getTitle());

	        	slice.close();

			}
		}

		return everythingOK;
	}

    private final static float[] extractSliceFloat( final Img<FloatType> img, final int dimX, final int dimY, final int[] dimensionPositions )
    {
		final int sizeX = (int)img.dimension( dimX );
		final int sizeY = (int)img.dimension( dimY );
    	
    	final RandomAccess<FloatType> cursor = img.randomAccess();
    	final int[] tmp = dimensionPositions.clone();
    	
		// store the slice image
    	float[] sliceImg = new float[ sizeX * sizeY ];
    	
    	int i = 0;
    	
    	if ( dimY < img.numDimensions() )
    	{    		
    		for ( int y = 0; y < sizeY; ++y )
    		{
    	    	tmp[ dimX ] = 0;
    	    	tmp[ dimY ] = y;
    	    	cursor.setPosition( dimensionPositions );
    	    	
        		for ( int x = 0; x < sizeX - 1; ++x )
        		{
        			sliceImg[ i++ ] = cursor.get().get();
        			cursor.fwd( dimX );
        		}
    			sliceImg[ i++ ] = cursor.get().get();
    		}
    	}
    	else // only a 1D image
    	{
	    	tmp[ dimX ] = 0;
	    	cursor.setPosition( dimensionPositions );
    		
    		for ( int x = 0; x < sizeX - 1; ++x )
    		{
    			sliceImg[ i++ ] = cursor.get().get();
    			cursor.fwd( dimX );		
    		}
			sliceImg[ i++ ] = cursor.get().get();    		
    	}

    	return sliceImg;
    }

}
