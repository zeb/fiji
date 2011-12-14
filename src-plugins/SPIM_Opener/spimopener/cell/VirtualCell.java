package spimopener.cell;

import java.lang.ref.SoftReference;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.AbstractCell;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class VirtualCell< T extends RealType< T > & NativeType< T >, A extends ArrayDataAccess< A > > extends AbstractCell< A >
{
	private final T type;

	private SoftReference< A > dataRef;

	private final String fn;

	public VirtualCell( final int[] dimensions, final long[] min, final int entitiesPerPixel, final T type, final String fn )
	{
		super( dimensions, min );
		this.type = type;
		this.dataRef = new SoftReference< A >( null );
		this.fn = fn;
	}

	@Override
	public A getData()
	{
		A data = dataRef.get();
		if ( data == null )
		{
			final ArrayImg< T, A > img;
			final ImgOpener io = new ImgOpener();
			try
			{
				System.out.println( "loading " + fn );
				img = ( ArrayImg< T, A > ) io.openImg( fn, new ArrayImgFactory< T >(), type.createVariable() ).getImg();
				data = img.update( null );
				dataRef = new SoftReference< A >( data );
			}
			catch ( final ImgIOException e )
			{
				e.printStackTrace();
			}
		}
		return data;
	}
}
