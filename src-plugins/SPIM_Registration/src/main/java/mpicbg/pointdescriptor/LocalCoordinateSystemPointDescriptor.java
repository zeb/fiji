package mpicbg.pointdescriptor;

import java.util.ArrayList;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import net.imglib2.RealLocalizable;

public class LocalCoordinateSystemPointDescriptor < P extends Point > extends AbstractPointDescriptor< P, LocalCoordinateSystemPointDescriptor<P> >
		implements RealLocalizable
{
	final protected boolean normalize;
	public float ax = 1, bx, by, cx, cy, cz;

	public LocalCoordinateSystemPointDescriptor( final P basisPoint, final ArrayList<P> orderedNearestNeighboringPoints,
												 final boolean normalize ) throws NoSuitablePointsException
	{
		super( basisPoint, orderedNearestNeighboringPoints, null, null );

		if ( numDimensionsOfPoints != 3 )
			throw new NoSuitablePointsException( "LocalCoordinateSystemPointDescriptor does not support dim = " + numDimensionsOfPoints + ", only dim = 3 is valid." );

		/* check that number of points is at least model.getMinNumMatches() */
		if ( numNeighbors() != 3 )
			throw new NoSuitablePointsException( "Only 3 nearest neighbors is supported by a LocalCoordinateSystemPointDescriptor : num neighbors = " + numNeighbors() );

		this.normalize = normalize;

		buildLocalCoordinateSystem( descriptorPoints, normalize );
	}

	@Override
	public double descriptorDistance( final LocalCoordinateSystemPointDescriptor< P > pointDescriptor )
	{
		float difference = 0;

		if ( !normalize )
			difference += ( ax - pointDescriptor.ax ) * ( ax - pointDescriptor.ax );

		difference += ( bx - pointDescriptor.bx ) * ( bx - pointDescriptor.bx );
		difference += ( by - pointDescriptor.by ) * ( by - pointDescriptor.by );
		difference += ( cx - pointDescriptor.cx ) * ( cx - pointDescriptor.cx );
		difference += ( cy - pointDescriptor.cy ) * ( cy - pointDescriptor.cy );
		difference += ( cz - pointDescriptor.cz ) * ( cz - pointDescriptor.cz );

		return difference;// / 3.0;
	}

	/**
	 * Not necessary as the main matching method is overwritten
	 */
	@Override
	public Object fitMatches( final ArrayList<PointMatch> matches )  { return null; }

	public void buildLocalCoordinateSystem( final ArrayList< LinkedPoint< P > > neighbors, final boolean normalize )
	{
		// most distant point
		final Vector3f b = new Vector3f( neighbors.get( 0 ).getL() );
		final Vector3f c = new Vector3f( neighbors.get( 1 ).getL() );
		final Vector3f d = new Vector3f( neighbors.get( 2 ).getL() );

		final Vector3f x = new Vector3f( d );
		x.normalize();

//		IOFunctions.println( "Input" );
//		IOFunctions.println( b );
//		IOFunctions.println( c );
//		IOFunctions.println( d );

		if ( normalize )
		{
			final float lengthD = 1.0f / d.length();

			b.scale(lengthD);
			c.scale(lengthD);
			d.scale(lengthD);

//			IOFunctions.println( "Scaled" );
//			IOFunctions.println( b + "(" + b.length() + ")");
//			IOFunctions.println( c + "(" + c.length() + ")");
//			IOFunctions.println( d + "(" + d.length() + ")");
		}
		else
		{
			ax = d.length();
		}

		// get normal vector of ab and ad ( which will be the z-axis)
		final Vector3f n = new Vector3f();
		n.cross(b, x);
		n.normalize();

//		IOFunctions.println( "Normal vector (z-axis)" );
//		IOFunctions.println( n );

		// check if the normal vector points into the direction of point c
		if ( n.dot( c ) < 0 )
		{
			n.negate();
//			IOFunctions.println( "Negated normal vector (z-axis)" );
//			IOFunctions.println( n );
		}

		// get the inverse of the matrix that maps the vectors into the local coordinate system
		// where the x-axis is vector(ad), the z-axis is n and the y-axis is cross-product(x,z)
		final Vector3f y = new Vector3f();
		y.cross( n, x );
		y.normalize();

//		IOFunctions.println( "X - axis" );
//		IOFunctions.println( x );
//
//		IOFunctions.println( "Y - axis" );
//		IOFunctions.println( y );

		final Matrix3f m = new Matrix3f();
		m.m00 = x.x; m.m01 = y.x; m.m02 = n.x;
		m.m10 = x.y; m.m11 = y.y; m.m12 = n.y;
		m.m20 = x.z; m.m21 = y.z; m.m22 = n.z;

		try
		{
			m.invert();
		}
		catch ( final Exception e )
		{
			bx = by = cx = cy = cz = 0;
			return;
		}


		// get the positions in the local coordinate system
		final Vector3f bl = new Vector3f( b );
		final Vector3f cl = new Vector3f( c );

		m.transform( bl );
		m.transform( cl );

//		IOFunctions.println( "In local coordinate system" );
//		IOFunctions.println( bl );
//		IOFunctions.println( cl );

		bx = bl.x;
		by = bl.y;
		cx = cl.x;
		cy = cl.y;
		cz = cl.z;

//		System.out.println( "NEW" );
//		System.out.println( ax );
//		System.out.println( bx );
//		System.out.println( by );
//		System.out.println( cx );
//		System.out.println( cy );
//		System.out.println( cz );
//
//		System.exit( 0 );
	}

	@Override
	public boolean resetWorldCoordinatesAfterMatching() { return true; }

	@Override
	public boolean useWorldCoordinatesForDescriptorBuildUp() { return false; }

	@Override
	public int numDimensions()
	{
		if ( normalize )
			return 5;
		else
			return 6;
	}

	@Override
	public void localize( final float[] position )
	{
		if ( normalize )
		{
			position[ 0 ] = bx;
			position[ 1 ] = by;
			position[ 2 ] = cx;
			position[ 3 ] = cy;
			position[ 4 ] = cz;
		}
		else
		{
			position[ 0 ] = ax;
			position[ 1 ] = bx;
			position[ 2 ] = by;
			position[ 3 ] = cx;
			position[ 4 ] = cy;
			position[ 5 ] = cz;
		}
	}

	@Override
	public void localize( final double[] position )
	{
		if ( normalize )
		{
			position[ 0 ] = bx;
			position[ 1 ] = by;
			position[ 2 ] = cx;
			position[ 3 ] = cy;
			position[ 4 ] = cz;
		}
		else
		{
			position[ 0 ] = ax;
			position[ 1 ] = bx;
			position[ 2 ] = by;
			position[ 3 ] = cx;
			position[ 4 ] = cy;
			position[ 5 ] = cz;
		}
	}

	@Override
	public float getFloatPosition( final int d )
	{
		if ( normalize )
		{
			if ( d == 0 )
				return bx;
			else if ( d == 1 )
				return by;
			else if ( d == 2 )
				return cx;
			else if ( d == 3 )
				return cy;
			else
				return cz;
		}
		else
		{
			if ( d == 0 )
				return ax;
			else if ( d == 1 )
				return bx;
			else if ( d == 2 )
				return by;
			else if ( d == 3 )
				return cx;
			else if ( d == 4 )
				return cy;
			else
				return cz;
		}
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return getFloatPosition( d );
	}
}
