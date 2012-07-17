package mpicbg.spim.registration.detection;

import mpicbg.imglib.util.Util;
import mpicbg.models.Point;
import net.imglib2.RealLocalizable;

public abstract class AbstractDetection< T extends AbstractDetection< T > > extends Point implements RealLocalizable
{
	private static final long serialVersionUID = 1L;

	final protected int id;
	protected double weight;
	protected boolean useW = false;

	// used for display
	protected float distance = -1;

	// used for recursive parsing
	protected boolean isUsed = false;

	public AbstractDetection( final int id, final float[] location )
	{
		super( location );
		this.id = id;
	}

	public AbstractDetection( final int id, final float[] location, final double weight )
	{
		super( location );
		this.id = id;
		this.weight = weight;
	}

	public void setWeight( final double weight ){ this.weight = weight; }
	public double getWeight(){ return weight; }
	public int getID() { return id; }
	public void setDistance( final float distance )  { this.distance = distance; }
	public float getDistance() { return distance; }
	public boolean isUsed() { return isUsed; }
	public void setUsed( final boolean isUsed ) { this.isUsed = isUsed; }

	public boolean equals( final AbstractDetection<?> otherDetection )
	{
		if ( useW )
		{
			for ( int d = 0; d < 3; ++d )
				if ( w[ d ] != otherDetection.w[ d ] )
					return false;
		}
		else
		{
			for ( int d = 0; d < 3; ++d )
				if ( l[ d ] != otherDetection.l[ d ] )
					return false;
		}

		return true;
	}

	public void setW( final float[] wn )
	{
		for ( int i = 0; i < w.length; ++i )
			w[ i ] = wn[ i ];
	}

	public void resetW()
	{
		for ( int i = 0; i < w.length; ++i )
			w[i] = l[i];
	}

	public double getDistance( final Point point2 )
	{
		double distance = 0;
		final float[] a = getL();
		final float[] b = point2.getW();

		for ( int i = 0; i < getL().length; ++i )
		{
			final double tmp = a[ i ] - b[ i ];
			distance += tmp * tmp;
		}

		return Math.sqrt( distance );
	}

	public void setUseW( final boolean useW ) { this.useW = useW; }
	public boolean getUseW() { return useW; }

	@Override
	public int numDimensions()
	{
		return w.length;
	}

	@Override
	public void localize( final float[] position )
	{
		final int n = w.length;
		if ( useW )
			for ( int d = 0; d < n; ++d )
				position[ d ] = w[ d ];
		else
			for ( int d = 0; d < n; ++d )
				position[ d ] = l[ d ];
	}

	@Override
	public void localize( final double[] position )
	{
		final int n = w.length;
		if ( useW )
			for ( int d = 0; d < n; ++d )
				position[ d ] = w[ d ];
		else
			for ( int d = 0; d < n; ++d )
				position[ d ] = l[ d ];
	}

	@Override
	public float getFloatPosition( final int d )
	{
		if ( useW )
			return w[ d ];
		else
			return l[ d ];
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return getFloatPosition( d );
	}

	@Override
	public String toString()
	{
		final String desc = "Detection " + getID() + " l"+ Util.printCoordinates( getL() ) + "; w"+ Util.printCoordinates( getW() );
		return desc;
	}
}
