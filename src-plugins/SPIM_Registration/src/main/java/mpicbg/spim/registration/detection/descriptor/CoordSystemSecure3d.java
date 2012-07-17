package mpicbg.spim.registration.detection.descriptor;

import java.util.ArrayList;

import mpicbg.pointdescriptor.LocalCoordinateSystemPointDescriptor;
import mpicbg.pointdescriptor.ModelPointDescriptor;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import mpicbg.pointdescriptor.matcher.Matcher;
import mpicbg.pointdescriptor.matcher.SimpleMatcher;
import mpicbg.pointdescriptor.model.TranslationInvariantModel;
import mpicbg.pointdescriptor.model.TranslationInvariantRigidModel3D;
import mpicbg.pointdescriptor.similarity.SimilarityMeasure;
import mpicbg.pointdescriptor.similarity.SquareDistance;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import mpicbg.spim.registration.detection.DetectionView;
import net.imglib2.collection.KDTree;
import net.imglib2.neighborsearch.KNearestNeighborSearch;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;


public class CoordSystemSecure3d<T extends DetectionView<?,T>> extends CoordSystem3d<T>
{
	final int numExtractions;

	public CoordSystemSecure3d( final int numExtractions )
	{
		this.numExtractions = numExtractions;
	}

	@Override
	protected void computeMatching(
			final ArrayList< LocalCoordinateSystemPointDescriptor< T > > descriptors1,
			final KDTree< LocalCoordinateSystemPointDescriptor< T > > lookUpTree2,
			final ArrayList<PointMatchGeneric<T>> correspondences,
			final double differenceThreshold,
			final double ratioOfDistance )
	{
		//System.out.println( "BeadA" + "\t" + "BeadB1" + "\t" + "BeadB2" + "\t" + "Diff1" + "\t" + "Diff2" );

		final int k = ( int ) Math.min( lookUpTree2.size(), 2 + numExtractions );
		final KNearestNeighborSearch< LocalCoordinateSystemPointDescriptor< T > > nnsearch2 = new KNearestNeighborSearchOnKDTree< LocalCoordinateSystemPointDescriptor< T > >( lookUpTree2, k );
		for ( final LocalCoordinateSystemPointDescriptor< T > descriptorA : descriptors1 )
		{
			final ModelPointDescriptor< T > modelPointDescriptorA = transformDescriptor( descriptorA );

			if( modelPointDescriptorA == null )
				continue;

			nnsearch2.search( descriptorA );

			// create standard model point descriptors from the coordinate system descriptors
			final ArrayList< ModelPointDescriptor< T > > modelPointDescriptorsB = new ArrayList<ModelPointDescriptor<T>>( k );

			for ( int i = 0; i < k; ++i )
			{
				final LocalCoordinateSystemPointDescriptor< T > match = nnsearch2.getSampler( i ).get();
				final ModelPointDescriptor< T > modelPointDescriptorB = transformDescriptor( match );
				if ( modelPointDescriptorB != null )
					modelPointDescriptorsB.add( modelPointDescriptorB );
			}

			if ( modelPointDescriptorsB.size() < 2 )
				continue;

			double bestDifference = Double.MAX_VALUE;
			double secondBestDifference = Double.MAX_VALUE;

			ModelPointDescriptor< T > bestMatch = null;
			ModelPointDescriptor< T > secondBestMatch = null;

			for ( final ModelPointDescriptor< T > modelPointDescriptorB : modelPointDescriptorsB )
			{
				final double difference = modelPointDescriptorA.descriptorDistance( modelPointDescriptorB );

				if ( difference < secondBestDifference )
				{
					secondBestDifference = difference;
					secondBestMatch = modelPointDescriptorB;

					if ( secondBestDifference < bestDifference )
					{
						final double tmpDiff = secondBestDifference;
						final ModelPointDescriptor< T > tmpMatch = secondBestMatch;

						secondBestDifference = bestDifference;
						secondBestMatch = bestMatch;

						bestDifference = tmpDiff;
						bestMatch = tmpMatch;
					}
				}
			}

			if ( bestDifference < differenceThreshold && bestDifference * ratioOfDistance <= secondBestDifference )
			{
				final T detectionA = descriptorA.getBasisPoint();
				final T detectionB = bestMatch.getBasisPoint();

				//System.out.println( beadA.getID() + "\t" + matches[ 0 ].getBasisPoint().getID() + "\t" + matches[ 1 ].getBasisPoint().getID() + "\t" + bestDifference + "\t" + secondBestDifference );

				detectionA.addPointDescriptorCorrespondence( detectionB, 1 );
				detectionB.addPointDescriptorCorrespondence( detectionA, 1 );

				correspondences.add( new PointMatchGeneric<T>( detectionA, detectionB, 1 ) );
			}
		}

		//System.exit( 0 );
	}

	final private static TranslationInvariantModel<?> model = new TranslationInvariantRigidModel3D();
	final private static Matcher matcher = new SimpleMatcher( 3 );
	final private static SimilarityMeasure similarityMeasure = new SquareDistance();

	final private static <T extends DetectionView<?,T>> ModelPointDescriptor< T > transformDescriptor( final LocalCoordinateSystemPointDescriptor< T > desc )
	{
		final T basisPoint = desc.getBasisPoint();
		final ArrayList< T > orderedNearestNeighboringPoints = desc.getOrderedNearestNeighboringPoints();

		try
		{
			return new ModelPointDescriptor<T>( basisPoint, orderedNearestNeighboringPoints, model, similarityMeasure, matcher );
		}
		catch (final NoSuitablePointsException e)
		{
			return null;
		}
	}

}
