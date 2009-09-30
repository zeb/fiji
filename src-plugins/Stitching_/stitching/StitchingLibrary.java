package stitching;

import static stitching.CommonFunctions.colorList;

import ij.ImagePlus;

import stitching.plugin.Stitch_Image_Collection;


public class StitchingLibrary
{
	public static CorrelationResult computePhaseCorrelations( final ImagePlus a, final ImagePlus b, final CorrelationResult cr  )
	{
		final CorrelationResult c = cr == null ? new CorrelationResult() : cr;
		
		final int dim;
		
		if ( a.getStackSize() > 1 || b.getStackSize() > 1 )
			dim = 3;
		else
			dim = 2;
		
		final ImageInformation i1 = new ImageInformation( dim, 0, null );
		final ImageInformation i2 = new ImageInformation( dim, 1, null );
		
		i1.imp = a;
		i2.imp = b;
		
		OverlapProperties op = new OverlapProperties( i1, i2 ); 
		
		Stitch_Image_Collection.computePhaseCorrelations( op , CommonFunctions.colorList[ colorList.length - 1 ], CommonFunctions.rgbTypes[ 0 ], false );		
		
		c.R = op.R;		
		c.translation = new float[ dim ];
		
		if ( dim == 2 )
		{
			c.translation[ 0 ] = op.translation2D.x;
			c.translation[ 1 ] = op.translation2D.y;
		}
		else
		{
			c.translation[ 0 ] = op.translation3D.x;
			c.translation[ 1 ] = op.translation3D.y;			
			c.translation[ 2 ] = op.translation3D.z;			
		}
		
		return c;
	}
}
