package spimopener.cell;

import ij.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.numeric.real.FloatType;

public class VirtualCellTest
{
	public static void main( final String[] args )
	{
		final long[] dimensions = new long[] { 277, 407, 100*252};
		final int[] cellDimensions = new int[] { 277, 407, 1 };
		final VirtualCells< FloatType, FloatArray > cells = new VirtualCells< FloatType, FloatArray >( 1, dimensions, cellDimensions, new FloatType() );
		final CellImg< FloatType, FloatArray, VirtualCell< FloatType, FloatArray > > img =
				new CellImg< FloatType, FloatArray, VirtualCell< FloatType, FloatArray > >( null, cells );
		img.setLinkedType( new FloatType( img ) );

		new ImageJ();
		ImageJFunctions.show( img );

		Img< FloatType > img2 = null;
		final ImgOpener io = new ImgOpener();
		try
		{
			img2 = io.openImg( "/home/tobias/workspace/data/DM_MV_110629/angle0/tl_120.tif", new ArrayImgFactory< FloatType >(), new FloatType() ).getImg();
		}
		catch ( final ImgIOException e )
		{
			e.printStackTrace();
		}
		ImageJFunctions.show( img2 );

	}
}
