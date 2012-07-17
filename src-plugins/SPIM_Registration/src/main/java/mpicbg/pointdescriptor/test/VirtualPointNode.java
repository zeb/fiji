package mpicbg.pointdescriptor.test;

import mpicbg.models.Point;
import net.imglib2.RealLocalizable;

public class VirtualPointNode<P extends Point> implements RealLocalizable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	final P p;
	final int numDimensions;

	public VirtualPointNode( final P p )
	{
		this.p = p;
		this.numDimensions = p.getL().length;
	}

	public P getPoint() { return p; }

	@Override
	public int numDimensions()
	{
		return numDimensions;
	}

	@Override
	public void localize( final float[] position )
	{
		final float[] w = p.getW();
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = w[ d ];
	}

	@Override
	public void localize( final double[] position )
	{
		final float[] w = p.getW();
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = w[ d ];
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return p.getW()[ d ];
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return p.getW()[ d ];
	}
}
