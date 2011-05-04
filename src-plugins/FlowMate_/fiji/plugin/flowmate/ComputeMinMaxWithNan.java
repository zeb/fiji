package fiji.plugin.flowmate;

import mpicbg.imglib.algorithm.math.ComputeMinMax;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

public class ComputeMinMaxWithNan extends ComputeMinMax<FloatType> {

	private Image<FloatType> image;

	public ComputeMinMaxWithNan(Image<FloatType> image) {
		super(image);
		this.image = image; // Damn you, who make your field default and not protected!
	}

	@Override
	protected void compute(long startPos, long loopSize, FloatType min, FloatType max) {
		
		final Cursor<FloatType> cursor = image.createCursor();
		
		cursor.fwd();
		
		min.set( Float.POSITIVE_INFINITY );
		max.set( Float.NEGATIVE_INFINITY );
		
		cursor.reset();

		// move to the starting position of the current thread
		cursor.fwd( startPos );		

        // do as many pixels as wanted by this thread
        for ( long j = 0; j < loopSize; ++j )
        {
			cursor.fwd();
			
			final FloatType value = cursor.getType();
			if (value.get() == Float.NaN)
				continue;
			
			if ( Util.min( min, value ) == value )
				min.set( value );
			
			if ( Util.max( max, value ) == value )
				max.set( value );
		}
		
		cursor.close();
		
	}
	

}
