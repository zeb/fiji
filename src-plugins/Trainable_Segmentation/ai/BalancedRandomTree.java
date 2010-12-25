package ai;

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
 * 			Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import ij.IJ;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import weka.core.Instance;
import weka.core.Instances;

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
	 * @param ins The instances to use.
	 * @param splitter split function generator
	 */
	public BalancedRandomTree(final Instance[] ins, final int numAttributes, final int numClasses, final int classIndex, final Splitter splitter)
	{
		this.rootNode = createNode( ins, numAttributes, numClasses, classIndex, splitter );
	}

	/**
	 * Build the random tree based on the data specified 
	 * in the constructor 
	 */
	private final BaseNode createNode(final Instance[] ins, final int numAttributes, final int numClasses, final int classIndex, final Splitter splitter)
	{
			
		final long start = System.currentTimeMillis();
		try {
			return createTree(ins, numAttributes, numClasses, classIndex, 0, splitter);
		} finally {
			final long end = System.currentTimeMillis();
			IJ.log("Creating tree took: " + (end-start) + "ms");
		}
	}

	/**
	 * Evaluate sample
	 * 
	 * @param instance sample to evaluate
	 * @return array of class probabilities
	 */
	public double[] evaluate(Instance instance)
	{
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
		 * @param instance input sample
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
		public double[] eval(Instance instance)   // TODO isn't this an error? instance is ignored
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
				final Instance[] ins,
				final int insSize,
				final int numClasses)
		{
			this.probability = new double[ numClasses ];
			for(int i=0; i<insSize; i++)
			{
				this.probability[ (int) ins[i].classValue() ] ++;
			}
			// Divide by the number of elements
			for(int i=0; i<probability.length; i++)
				this.probability[i] /= insSize;
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
		 * Construct interior node of the tree
		 * 
		 * @param data pointer to the original set of samples
		 * @param indices indices of the samples at this node
		 * @param depth current tree depth
		 */
		/*		public InteriorNode(
				final Instances data,
				final ArrayList<Integer> indices,
				final int depth,
				final Splitter splitFnProducer)
		{
			this.splitFn = splitFnProducer.getSplitFunction(data, indices);


			this.depth = depth;

			// left and right new arrays
			final ArrayList<Integer> leftArray = new ArrayList<Integer>();
			final ArrayList<Integer> rightArray = new ArrayList<Integer>();

			// split data
			int totalLeft = 0;
			int totalRight = 0;
			for(final Integer it : indices)
			{
				if( splitFn.evaluate( data.get(it.intValue()) ) )
				{
					leftArray.add(it);
					totalLeft ++;					
				}
				else
				{
					rightArray.add(it);
					totalRight ++;
				}
			}
			//System.out.println("total left = " + totalLeft + ", total rigth = " + totalRight + ", depth = " + depth);					
			//indices.clear();
			if( totalLeft == 0 )
			{
				left = new LeafNode(data, rightArray);
			}
			else if ( totalRight == 0 )
			{
				left = new LeafNode(data, leftArray);
			}
			else
			{
				left = new InteriorNode(data, leftArray, depth+1, splitFnProducer);
				right = new InteriorNode(data, rightArray, depth+1, splitFnProducer);
			}				
		}*/


		/**
		 * Evaluate sample at this node
		 */
		public double[] eval(Instance instance) 
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
			Instance[] ins,
			final int numAttributes,
			final int numClasses,
			final int classIndex,
			final int depth,
			final Splitter splitFnProducer)
	{
		int maxDepth = depth;
		// Create root node
		InteriorNode root = new InteriorNode(depth, splitFnProducer.getSplitFunction(ins, ins.length, numAttributes, numClasses, classIndex));
		
		// Create list of nodes to process and add the root to it
		final LinkedList<InteriorNode> remainingNodes = new LinkedList<InteriorNode>();
		remainingNodes.add(root);
		
		// Create list of indices to process (it must match all the time with the node list)
		final LinkedList<Instance[]> remainingInstances = new LinkedList<Instance[]>();
		remainingInstances.add(ins);
		final LinkedList<Integer> remainingSizes = new LinkedList<Integer>();
		remainingSizes.add(ins.length);
		// Forget the large array:
		ins = null;

		// While there still are nodes to process
		while (!remainingNodes.isEmpty())
		{
			final InteriorNode currentNode = remainingNodes.removeFirst(); // remove first, to forget the large arrays quickly
			final Instance[] currentInstances = remainingInstances.removeFirst();
			final int currentSize = remainingSizes.removeFirst();
			// new arrays for the left and right sons
			//final ArrayList<Instance> leftArray = new ArrayList<Instance>();
			//final ArrayList<Instance> rightArray = new ArrayList<Instance>();
			final Instance[] leftArray = new Instance[currentSize];
			final Instance[] rightArray = new Instance[currentSize];
			int nextLeft = 0,
			    nextRight = 0;

			// split data
			//for(int i=0; i < currentInstances.length; i++)
			for(int i=0; i < currentSize; i++)
			{
				if( currentNode.splitFn.evaluate( currentInstances[i] ) )
				{
					//leftArray.add(currentInstances[i]);
					leftArray[nextLeft++] = currentInstances[i];
				}
				else
				{
					//rightArray.add(currentInstances[i]);
					rightArray[nextRight++] = currentInstances[i];
				}
			}
			//System.out.println("total left = " + leftArray.size() + ", total right = " + rightArray.size() + ", depth = " + currentNode.depth);					
			//System.out.println("total left = " + nextLeft + ", total right = " + nextRight + ", depth = " + currentNode.depth);					

			// Update maximum depth (for the record)
			if(currentNode.depth > maxDepth)
				maxDepth = currentNode.depth;

			//if( leftArray.isEmpty() )
			if (0 == nextLeft)
			{
				//currentNode.left = new LeafNode(rightArray.toArray(new Instance[rightArray.size()]), numClasses);
				currentNode.left = new LeafNode(rightArray, nextRight, numClasses);
				//System.out.println("Created leaf with feature " + currentNode.splitFn.index);
			}
			//else if ( rightArray.isEmpty() )
			else if (0 == nextRight)
			{
				//currentNode.left = new LeafNode(leftArray.toArray(new Instance[leftArray.size()]), numClasses);
				currentNode.left = new LeafNode(leftArray, nextLeft, numClasses);
				//System.out.println("Created leaf with feature " + currentNode.splitFn.index);
			}
			else
			{
				//final Instance[] leftIns = leftArray.toArray(new Instance[leftArray.size()]);
				//currentNode.left = new InteriorNode(currentNode.depth+1, splitFnProducer.getSplitFunction(leftIns, numAttributes, numClasses, classIndex));
				currentNode.left = new InteriorNode(currentNode.depth+1, splitFnProducer.getSplitFunction(leftArray, nextLeft, numAttributes, numClasses, classIndex));
				remainingNodes.add((InteriorNode)currentNode.left);
				remainingInstances.add(leftArray);
				remainingSizes.add(nextLeft);

				//final Instance[] rightIns = rightArray.toArray(new Instance[rightArray.size()]);
				//currentNode.right = new InteriorNode(currentNode.depth+1, splitFnProducer.getSplitFunction(rightIns, numAttributes, numClasses, classIndex));
				currentNode.right = new InteriorNode(currentNode.depth+1, splitFnProducer.getSplitFunction(rightArray, nextRight, numAttributes, numClasses, classIndex));
				remainingNodes.add((InteriorNode)currentNode.right);
				remainingInstances.add(rightArray);
				remainingSizes.add(nextRight);
			}
		}

		//System.out.println("Max depth = " + maxDepth);
		return root;
	}

}
