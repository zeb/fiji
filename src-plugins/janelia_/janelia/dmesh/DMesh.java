/**
 * 
 */
package janelia.dmesh;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ini.trakem2.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.in.MinimalTiffReader;
import mpicbg.models.AffineModel2D;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.InverseCoordinateTransformMap2D;
import mpicbg.models.NoninvertibleModelException;

/**
 * Experimantal implementation of an
 * {@link InverseCoordinateTransformMap2D} that is generated from the export
 * of Lou Scheffer's DMesh pairwise registration program.
 *  
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de> and Albert Cardona
 */
public class DMesh extends InverseCoordinateTransformMap2D
{
	static protected ImagePlus openTiff( final String path ) throws Exception
	{
		IFormatReader fr = null;
		try
		{
			fr = new MinimalTiffReader();
			fr.setId( path );
			return new ImagePlus( path, fr.openImage( 0 ) );
		}
		catch ( Exception e ) { throw e; }
		finally
		{
			if ( null != fr )
			{
				try { fr.close(); }
				catch ( IOException ioe ) { IJ.error( "Could not close IFormatReader: " + ioe ); }
			}
		}
	}
	
	static protected float[][] init( final String affinesPath, final String mapPath ) throws Exception
	{
		final ImagePlus map = openTiff( mapPath );
		final ByteProcessor bpMap = ( ByteProcessor )map.getProcessor();
		
		final String[] affinesText = Utils.openTextFileLines( affinesPath );
		if ( affinesText.length == 0 ) throw new Exception( "Affines could not be read." );
		
		final ArrayList< AffineModel2D > affines = new ArrayList< AffineModel2D >();
		for ( final String affineDescription : affinesText )
		{
			final String[] values = affineDescription.split( "\\s+" );
			IJ.log( values[ 1 ] + " " + values[ 2 ] + " " + values[ 3 ] + " " + values[ 4 ] + " " + values[ 5 ] + " " + values[ 6 ] );
			
			final AffineModel2D affine = new AffineModel2D();
			affine.set(
					Float.parseFloat( values[ 1 ] ),
					Float.parseFloat( values[ 2 ] ),
					Float.parseFloat( values[ 3 ] ),
					Float.parseFloat( values[ 4 ] ),
					Float.parseFloat( values[ 5 ] ),
					Float.parseFloat( values[ 6 ] ) );
			affines.add( affine );
		}
		
		final float[][] iMap = new float[ bpMap.getHeight() ][ bpMap.getWidth() * 2 ];
		final float[] l = new float[ 2 ];
		for ( int y = 0; y < bpMap.getHeight(); ++y )
			for ( int x = 0; x < bpMap.getWidth(); ++ x )
			{
				l[ 0 ] = x;
				l[ 1 ] = y;
				final int v = bpMap.get( x, y );
				if ( v < 10 )
					iMap[ y ][ x * 2 ] = iMap[ y ][ x * 2 + 1 ] = Float.NaN;
				else
				{
					affines.get( v - 10 ).applyInPlace( l );
					iMap[ y ][ x * 2 ] = l[ 0 ];
					iMap[ y ][ x * 2 + 1 ] = l[ 1 ];
				}
			}
		
		return iMap;
	}
	
	
	public DMesh( final String affinesPath, final String mapPath ) throws Exception
	{
		super( init( affinesPath, mapPath ) );
	}

}
