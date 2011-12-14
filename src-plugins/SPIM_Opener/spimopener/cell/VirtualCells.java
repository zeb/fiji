package spimopener.cell;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cells;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.img.list.ListImgFactory;
import net.imglib2.img.list.ListLocalizingCursor;
import net.imglib2.img.list.ListRandomAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class VirtualCells< T extends RealType< T > & NativeType< T >,  A extends ArrayDataAccess< A > > implements Cells< A, VirtualCell< T, A > >
{
	private final int entitiesPerPixel;
	private final int n;
	private final long[] dimensions;
	private final int[] cellDimensions;
	private final ListImg< VirtualCell< T, A > > cells;

	public VirtualCells( final int entitiesPerPixel, final long[] dimensions, final int[] cellDimensions, final T type )
	{
		this.entitiesPerPixel = entitiesPerPixel;
		this.n = dimensions.length;
		this.dimensions = dimensions.clone();
		this.cellDimensions = cellDimensions.clone();

		final long[] numCells = new long[ n ];
		final int[] borderSize = new int[ n ];
		final long[] currentCellOffset = new long[ n ];
		final int[] currentCellDims = new int[ n ];

		for ( int d = 0; d < n; ++d ) {
			numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
			borderSize[ d ] = ( int )( dimensions[ d ] - (numCells[ d ] - 1) * cellDimensions[ d ] );
		}

		cells = new ListImgFactory< VirtualCell< T, A > >().create( numCells, new VirtualCell< T, A >( new int[1], new long[1], entitiesPerPixel, type, null ) );

		final ListLocalizingCursor< VirtualCell< T, A > > cellCursor = cells.localizingCursor();
		int i = 0;
		while ( cellCursor.hasNext() ) {
			cellCursor.fwd();
			cellCursor.localize( currentCellOffset );
			for ( int d = 0; d < n; ++d )
			{
				currentCellDims[ d ] = ( (currentCellOffset[d] + 1 == numCells[d])  ?  borderSize[ d ]  :  cellDimensions[ d ] );
				currentCellOffset[ d ] *= cellDimensions[ d ];
			}
			final String fn = String.format( "/home/tobias/workspace/data/imglib_cell_test/tl_%05d.tif", i++ % 252 );
			cellCursor.set( new VirtualCell< T, A >( currentCellDims, currentCellOffset, entitiesPerPixel, type, fn ) );
		}
	}

	@Override
	public ListRandomAccess< VirtualCell< T, A > > randomAccess()
	{
		return cells.randomAccess();
	}

	@Override
	public ListCursor< VirtualCell< T, A > > cursor()
	{
		return cells.cursor();
	}

	@Override
	public ListLocalizingCursor< VirtualCell< T, A > > localizingCursor()
	{
		return cells.localizingCursor();
	}

	@Override
	public int numDimensions()
	{
		return n;
	}

	@Override
	public void dimensions( final long[] s )
	{
		for ( int i = 0; i < n; ++i )
			s[ i ] = dimensions[ i ];
	}

	@Override
	public long dimension( final int d )
	{
		try { return this.dimensions[ d ]; }
		catch ( final ArrayIndexOutOfBoundsException e ) { return 1; }
	}

	@Override
	public void cellDimensions( final int[] s )
	{
		for ( int i = 0; i < n; ++i )
			s[ i ] = cellDimensions[ i ];
	}

	@Override
	public int cellDimension( final int d )
	{
		try { return this.cellDimensions[ d ]; }
		catch ( final ArrayIndexOutOfBoundsException e ) { return 1; }
	}

	@Override
	public int getEntitiesPerPixel()
	{
		return entitiesPerPixel;
	}
}
