package mpicbg.icp;


import java.util.ArrayList;
import java.util.List;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.RealLocalizable;
import net.imglib2.collection.KDTree;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.util.Util;

public class SimplePointMatchIdentification < P extends Point & RealLocalizable > implements PointMatchIdentification<P>
{
	float distanceThresold;

	public SimplePointMatchIdentification( final float distanceThreshold )
	{
		this.distanceThresold = distanceThreshold;
	}

	public SimplePointMatchIdentification()
	{
		this.distanceThresold = Float.MAX_VALUE;
	}

	public void setDistanceThreshold( final float distanceThreshold ) { this.distanceThresold = distanceThreshold; }
	public float getDistanceThreshold() { return this.distanceThresold; }

	@Override
	public ArrayList<PointMatch> assignPointMatches( final List<P> target, final List<P> reference )
	{
		final ArrayList<PointMatch> pointMatches = new ArrayList<PointMatch>();

		final KDTree< P > kdTreeTarget = new KDTree< P >( target, target );
		final NearestNeighborSearch< P > nnSearchTarget = new NearestNeighborSearchOnKDTree< P >( kdTreeTarget );

		for ( final P point : reference )
		{
			nnSearchTarget.search( point );
			final P correspondingPoint = nnSearchTarget.getSampler().get();

			if ( Util.computeDistance( correspondingPoint, point ) <= distanceThresold )
				pointMatches.add( new PointMatch ( correspondingPoint, point ) );
		}

		return pointMatches;
	}
}
