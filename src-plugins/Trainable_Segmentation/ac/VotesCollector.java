package ac;

import java.util.concurrent.Callable;
import weka.classifiers.Classifier;
import weka.core.Utils;

/**
 * Used to retrieve the out-of-bag vote of an ensemble classifier for a single
 * instance. In classification, does not return the class distribution but only
 * class index of the dominant class.
 *
 * Implements callable so it can be run in multiple threads.
 *
 * Adapted by Ignacio Arganda-Carreras from the Fran Supek's version to work on BalancedRandomTree objects
 */
public class VotesCollector implements Callable<Double> 
{

	protected final BalancedRandomTree[] tree;
	protected final int instanceIdx;
	protected final SortedInstances si;
	protected final boolean[][] inBag;


	public VotesCollector(
			BalancedRandomTree[] tree, 
			int instanceIdx,
			SortedInstances si, 
			boolean[][] inBag)
	{
		this.tree = tree;
		this.instanceIdx = instanceIdx;
		this.si = si;
		this.inBag = inBag;
	}


	/** Determine predictions for a single instance. */
	@Override
	public Double call() throws Exception 
	{
		double[] classProbs = new double[si.numClasses];

		for (int treeIdx = 0; treeIdx < tree.length; treeIdx++) 
		{

			if (inBag[treeIdx][instanceIdx])
				continue;

			double[] curDist = tree[treeIdx].evaluate( si, instanceIdx );

			for ( int classIdx = 0; classIdx < curDist.length; classIdx++ )
				classProbs[ classIdx ] += curDist[ classIdx ];

		}

		double vote = Utils.maxIndex(classProbs);   // consensus - for classification

		return vote;
	}
}
