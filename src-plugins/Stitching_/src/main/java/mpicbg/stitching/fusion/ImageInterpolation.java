package mpicbg.stitching.fusion;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * This class is necessary as it can create an {@link Interpolator} for an {@link Image} even if hold it as < ? extends RealType< ? > >
 * 
 * @author preibischs
 *
 * @param <T>
 */
public class ImageInterpolation< T extends RealType< T > > 
{
	final Img< T > image;
	final RealRandomAccessible< T > realRandomAccessible;
	
	public ImageInterpolation( final Img< T > image, final InterpolatorFactory< T, RandomAccessible< T > > interpolatorFactory, final OutOfBoundsFactory< T, RandomAccessibleInterval< T > > outofboundsFactory )
	{
		this.image = image;
		this.realRandomAccessible = Views.interpolate( Views.extend( image, outofboundsFactory ), interpolatorFactory );
	}
	
	public Img< T > getImage() { return image; }
	public RealRandomAccess< T > createInterpolator() { return realRandomAccessible.realRandomAccess(); }
}
