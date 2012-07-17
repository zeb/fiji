package mpicbg.spim.registration.detection.descriptor;

import java.util.ArrayList;

import mpicbg.models.Point;
import mpicbg.pointdescriptor.LocalCoordinateSystemPointDescriptor;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import mpicbg.spim.registration.detection.DetectionView;
import net.imglib2.RealLocalizable;
import net.imglib2.collection.KDTree;
import net.imglib2.neighborsearch.KNearestNeighborSearch;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;

public class CoordSystem3d<T extends DetectionView<?,T>> implements CorrespondenceExtraction<T>
{
	@Override
	public ArrayList<PointMatchGeneric<T>> extractCorrespondenceCandidates(
			final ArrayList< T > nodeListA,
			final ArrayList< T > nodeListB,
			final double differenceThreshold,
			final double ratioOfDistance,
			final boolean useAssociatedBeads )
	{
		final int numNeighbors = 3;

		// this is slightly less expressive
		//ratioOfDistance /= 1.25;
		//differenceThreshold *= 1.25;

		final KDTree< T > tree1 = new KDTree< T >( nodeListA, nodeListA );
		final KDTree< T > tree2 = new KDTree< T >( nodeListB, nodeListB );

		final ArrayList< LocalCoordinateSystemPointDescriptor< T > > descriptors1 =
			createLocalCoordinateSystemPointDescriptors( tree1, nodeListA, numNeighbors, false );

		final ArrayList< LocalCoordinateSystemPointDescriptor< T > > descriptors2 =
			createLocalCoordinateSystemPointDescriptors( tree2, nodeListB, numNeighbors, false );

		// create lookup tree for descriptors2
		final KDTree< LocalCoordinateSystemPointDescriptor< T > > lookUpTree2 = new KDTree< LocalCoordinateSystemPointDescriptor< T > >( descriptors2, descriptors2 );

		// store the candidates for corresponding beads
		final ArrayList<PointMatchGeneric<T>> correspondences = new ArrayList<PointMatchGeneric<T>>();

		/* compute matching */
		computeMatching( descriptors1, lookUpTree2, correspondences, differenceThreshold, ratioOfDistance );

		return correspondences;
	}

	protected void computeMatching(
			final ArrayList< LocalCoordinateSystemPointDescriptor< T > > descriptors1,
			final KDTree< LocalCoordinateSystemPointDescriptor< T > > lookUpTree2,
			final ArrayList<PointMatchGeneric<T>> correspondences,
			final double differenceThreshold,
			final double ratioOfDistance )
	{
		//System.out.println( "BeadA" + "\t" + "BeadB1" + "\t" + "BeadB2" + "\t" + "Diff1" + "\t" + "Diff2" );

		final KNearestNeighborSearch< LocalCoordinateSystemPointDescriptor< T > > nnsearch2 = new KNearestNeighborSearchOnKDTree< LocalCoordinateSystemPointDescriptor< T > >( lookUpTree2, 2 );
		for ( final LocalCoordinateSystemPointDescriptor< T > descriptorA : descriptors1 )
		{
			nnsearch2.search( descriptorA );

			final double best = descriptorA.descriptorDistance( nnsearch2.getSampler( 0 ).get() );
			final double secondBest = descriptorA.descriptorDistance( nnsearch2.getSampler( 1 ).get() );
//			final double best = nnsearch2.getSquareDistance( 0 );
//			final double secondBest = nnsearch2.getSquareDistance( 1 );

			if ( best < differenceThreshold && best * ratioOfDistance <= secondBest )
			{
				final T detectionA = descriptorA.getBasisPoint();
				final T detectionB = nnsearch2.getSampler( 0 ).get().getBasisPoint();

				//System.out.println( beadA.getID() + "\t" + matches[ 0 ].getBasisPoint().getID() + "\t" + matches[ 1 ].getBasisPoint().getID() + "\t" + best + "\t" + secondBest );

				detectionA.addPointDescriptorCorrespondence( detectionB, 1 );
				detectionB.addPointDescriptorCorrespondence( detectionA, 1 );

				correspondences.add( new PointMatchGeneric<T>( detectionA, detectionB, 1 ) );
			}
		}

		//System.exit( 0 );
	}

	public static < P extends Point & RealLocalizable > ArrayList< LocalCoordinateSystemPointDescriptor< P > > createLocalCoordinateSystemPointDescriptors(
			final KDTree< P > tree,
            final ArrayList< P > basisPoints,
            final int numNeighbors,
            final boolean normalize )
	{
		final int k = ( int ) Math.min( tree.size(), numNeighbors + 1 );
		final KNearestNeighborSearch< P > nnsearch = new KNearestNeighborSearchOnKDTree< P >( tree, k );
		final ArrayList< LocalCoordinateSystemPointDescriptor< P > > descriptors = new ArrayList< LocalCoordinateSystemPointDescriptor< P > > ( );

		for ( final P point : basisPoints )
		{
			nnsearch.search( point );
			final ArrayList< P > neighbors = new ArrayList< P >( k );

			// the first hit is always the point itself
			for ( int i = 1; i < k; ++i )
				neighbors.add( nnsearch.getSampler( i ).get() );

			try
			{
				descriptors.add( new LocalCoordinateSystemPointDescriptor<P>( point, neighbors, normalize ) );
			}
			catch ( final NoSuitablePointsException e )
			{
				e.printStackTrace();
			}
		}

		return descriptors;
	}

}
