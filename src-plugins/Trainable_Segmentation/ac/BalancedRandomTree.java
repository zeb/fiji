package ac;

/**
 *
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), 
 * 	    Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import ij.IJ;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;

import weka.core.Instance;

/**
 * This class implements a random tree based on the split
 * function specified by the template in Splitter
 * 
 */
public class BalancedRandomTree implements Serializable
{
	/** Generated serial version UID */
	private static final long serialVersionUID = 41518309467L;
	/** root node */
	private final BaseNode rootNode;

	/**
	 * Build random tree for a balanced random forest  
	 * 
	 * @param si The reference sorted instances stash.
	 * @param instanceIndices The instances to use.
	 * @param splitter split function generator.
	 */
	public BalancedRandomTree(final SortedInstances si, final int[] instanceIndices, final Splitter splitter)
	{
		this.rootNode = createNode( si, instanceIndices, splitter );
	}

	/**
	 * Build the random tree based on the data specified 
	 * in the constructor 
	 */
	private final BaseNode createNode(final SortedInstances si, final int[] instanceIndices, final Splitter splitter)
	{
		final long start = System.currentTimeMillis();
		try {
			return createTree(si, instanceIndices, 0, splitter);
		} finally {
			final long end = System.currentTimeMillis();
			IJ.log("Creating tree took: " + (end-start) + "ms");
		}
	}

	/**
	 * Evaluate sample
	 * 
	 * @param SortedInstances Pointer to all instances
	 * @param ith The index of the instance to evaluate
	 * @return array of class probabilities
	 */
	public double[] evaluate(final SortedInstances si, final int ith)
	{
		if (null == rootNode)
			return null;
		return rootNode.eval(si, ith);
	}

	public double[] evaluate(final Instance instance) {
		if (null == rootNode)
			return null;
		return rootNode.eval(instance);
	}

	/**
	 * Basic node of the tree
	 *
	 */
	abstract class BaseNode implements Serializable
	{

		/** serial version ID */
		private static final long serialVersionUID = 46734234231L;
		/**
		 * Evaluate an instance
		 * @param SortedInstances Pointer to all instances
		 * @param ith The index of the instance to evaluate
		 * @return class probabilities
		 */
		public abstract double[] eval( SortedInstances si, final int ith );

		/**
		 * Evaluate an instance
		 * @return class probabilities
		 */
		public abstract double[] eval( Instance instance );
		/**
		 * Get the node depth
		 * 
		 * @return tree depth at that node
		 */
		public int getDepth()
		{
			return 0;
		}
	} // end class BaseNode

	/**
	 * Leaf node in the tree 
	 *
	 */
	class LeafNode extends BaseNode implements Serializable
	{
		/** serial version ID */
		private static final long serialVersionUID = 2019873470157L;
		/** class probabilites */
		double[] probability;

		@Override
		public double[] eval(final SortedInstances si, final int ith)   // TODO isn't this an error? instance is ignored -- or it's just the end, so it's done?
		{		
			return probability;
		}
		@Override
		public double[] eval(final Instance ins)
		{		
			return probability;
		}
		/**
		 * Create a leaf node
		 * 
		 * @param probability class probabilities
		 */
		public LeafNode(double[] probability)
		{
			this.probability = probability;
		}

		/**
		 * Create leaf node based on the current split data
		 *  
		 * @param data pointer to original data
		 * @param indices indices at this node
		 */
		public LeafNode(
				final SortedInstances si,
				final int[] indices,
				final int count,
				final int numClasses)
		{
			this.probability = new double[ numClasses ];

			// Class values are accessed directly by index
			final double[] classValues = si.values[si.classIndex];

			for(int i=0; i<count; i++)
			{
				this.probability[ (int) classValues[indices[i]] ] ++;
			}
			// Divide by the number of elements
			for(int i=0; i<probability.length; i++)
				this.probability[i] /= count;
		}

	} //end class LeafNode
	
	/**
	 * Interior node of the tree
	 *
	 */
	class InteriorNode extends BaseNode implements Serializable
	{
		/** serial version ID */
		private static final long serialVersionUID = 9972970234021L;
		/** left son */
		BaseNode left;
		/** right son */
		BaseNode right;
		/** node depth */
		final int depth;
		/** split function that divides the samples into left and right sons */
		final SplitFunction splitFn;

		/**
		 * Constructs an interior node of the random tree
		 * 
		 * @param depth tree depth at this node
		 * @param splitFn split function
		 */
		private InteriorNode(int depth, SplitFunction splitFn) 
		{
			this.depth = depth;
			this.splitFn = splitFn;
		}

		/**
		 * Evaluate sample at this node
		 */
		public double[] eval(final SortedInstances si, final int ith) 
		{
			if( null != right)
			{
				if (this.splitFn.evaluate( si, ith ) )
				{
					return left.eval( si, ith );
				}
				else
					return right.eval( si, ith );
			}
			else // leaves are always left nodes 
				return left.eval( si, ith );
		}

		/**
		 * Evaluate sample at this node
		 */
		public final double[] eval(final Instance instance)
		{
			if( null != right)
			{
				if(this.splitFn.evaluate( instance ) )
				{
					return left.eval(instance);
				}
				else
					return right.eval(instance);
			}
			else // leaves are always left nodes 
				return left.eval(instance);				
		}

		/**
		 * Get node depth
		 */
		public int getDepth()
		{
			return this.depth;
		}
	}

	/** Below, quicksort has nicer dynamics. */
	static public final int LIMIT = 5000;

	/**
	 * Create random tree (non-recursively)
	 * 
	 * @param data original data
	 * @param indices indices of the samples to use
	 * @param depth starting depth
	 * @param splitFnProducer split function producer
	 * @return root node 
	 */
	private InteriorNode createTree(
			final SortedInstances si,
			int[] instanceIndices,
			final int depth,
			final Splitter splitFnProducer)
	{
		int maxDepth = depth;

		// Create root node
		boolean[] complete = new boolean[si.size]; // the mask
		for (int i=0; i<instanceIndices.length; i++) complete[instanceIndices[i]] = true;
		BoundedArray range = new BoundedArray(instanceIndices, instanceIndices.length, complete); // si.createIndexRange());
		InteriorNode root = new InteriorNode(depth, splitFnProducer.getSplitFunction(si, range));
		// Create list of nodes to process and add the root to it
		final LinkedList<InteriorNode> remainingNodes = new LinkedList<InteriorNode>();
		remainingNodes.add(root);

		// Create list of indices to process (it must match all the time with the node list)
		final LinkedList<BoundedArray> remainingIndices = new LinkedList<BoundedArray>();
		remainingIndices.add(range);

		// Forget the array:
		range = null;
		instanceIndices = null;
		complete = null;

		// While there still are nodes to process
		while (!remainingNodes.isEmpty())
		{
			final InteriorNode currentNode = remainingNodes.removeFirst(); // remove first, to forget the large arrays quickly
			final BoundedArray currentBA = remainingIndices.removeFirst();
			final int[] currentIndices = currentBA.a;
			final int currentSize = currentBA.size;

			// new arrays for the left and right branches
			final int[] leftArray = new int[currentSize];
			final int[] rightArray = new int[currentSize];
			int leftCount = 0,
			    rightCount = 0;

			// Complete si.size arrays with false values for instances not in use.
			final boolean[] completeLeftArray, completeRightArray;

			// split data

			if (currentSize > LIMIT) { // TODO determine a proper cut
			//if (true) { // TODO determine a proper cut, and implement GiniFunction ability to cope with the lack of a complete non-null array by resorting.
				//
				completeLeftArray = new boolean[si.size]; // all false by default
				completeRightArray = new boolean[si.size];
				//
				for(int i=0; i<currentSize; i++)
				{
					final int k = currentIndices[i];
					if (currentNode.splitFn.evaluate( si, k ))
					{
						leftArray[leftCount++] = k;
						completeLeftArray[k] = true;
					} else {
						rightArray[rightCount++] = k;
						completeRightArray[k] = true;
					}
				}
			} else {
				completeLeftArray = null;
				completeRightArray = null;
				//
				for(int i=0; i<currentSize; i++)
				{
					final int k = currentIndices[i];
					if (currentNode.splitFn.evaluate( si, k ))
					{
						leftArray[leftCount++] = k;
					} else {
						rightArray[rightCount++] = k;
					}
				}
			}

			//System.out.println("total left = " + leftCount + ", total right = " + rightCount + ", depth = " + currentNode.depth);

			/*
			//debug:
			// All indices should have a true entry in complete
			int rmatches1 = 0, rmatches2 = 0;
			int lmatches1 = 0, lmatches2 = 0;
			final HashSet<Integer> differentIndices = new HashSet<Integer>();
			for (int i=0; i<leftCount; i++) {
				if (completeLeftArray[leftArray[i]]) lmatches1++;
				differentIndices.add(leftArray[i]);
			}
			for (int i=0; i<rightCount; i++) {
				if (completeRightArray[rightArray[i]]) rmatches1++;
			}
			for (int i=0; i<completeLeftArray.length; i++) {
				if (completeLeftArray[i]) lmatches2++;
				if (completeRightArray[i]) rmatches2++;
			}
			System.out.println((lmatches1 == leftCount) +", " + (lmatches1 == lmatches2) + ", " + (rmatches1 == rightCount) +", " + (rmatches1 == rmatches2));
			System.out.println(leftCount + ", " + lmatches1 + ", " + lmatches2 + ", -- " + rightCount + ", " + rmatches1 + ", " + rmatches2);
			System.out.println("different indices in left array: " + differentIndices.size());
			*/


			// Update maximum depth (for the record)
			if(currentNode.depth > maxDepth)
				maxDepth = currentNode.depth;

			if (0 == leftCount)
			{
				currentNode.left = new LeafNode(si, rightArray, rightCount, si.numClasses);
				//System.out.println("Created leaf with feature " + currentNode.splitFn.index);
			}
			else if (0 == rightCount)
			{
				currentNode.left = new LeafNode(si, leftArray, leftCount, si.numClasses);
				//System.out.println("Created leaf with feature " + currentNode.splitFn.index);
			}
			else
			{
				final BoundedArray leftBA = new BoundedArray(leftArray, leftCount, leftCount > LIMIT ? completeLeftArray : null);
				currentNode.left = new InteriorNode(currentNode.depth+1, splitFnProducer.getSplitFunction(si, leftBA));
				remainingNodes.add((InteriorNode)currentNode.left);
				remainingIndices.add(leftBA);

				final BoundedArray rightBA = new BoundedArray(rightArray, rightCount, rightCount > LIMIT ? completeRightArray : null);
				currentNode.right = new InteriorNode(currentNode.depth+1, splitFnProducer.getSplitFunction(si, rightBA));
				remainingNodes.add((InteriorNode)currentNode.right);
				remainingIndices.add(rightBA);
			}
		}

		//System.out.println("Max depth = " + maxDepth);
		return root;
	}
}
